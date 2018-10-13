/*
 * IdMUnit - Automated Testing Framework for Identity Management Solutions
 * Copyright (c) 2005-2018 TriVir, LLC
 *
 * This program is licensed under the terms of the GNU General Public License
 * Version 2 (the "License") as published by the Free Software Foundation, and
 * the TriVir Licensing Policies (the "License Policies").  A copy of the License
 * and the Policies were distributed with this program.
 *
 * The License is available at:
 * http://www.gnu.org/copyleft/gpl.html
 *
 * The Policies are available at:
 * http://www.idmunit.org/licensing/index.html
 *
 * Unless required by applicable law or agreed to in writing, this program is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied.  See the License and the Policies
 * for specific language governing the use of this program.
 *
 * www.TriVir.com
 * TriVir LLC
 * 13890 Braddock Road
 * Suite 310
 * Centreville, Virginia 20121
 *
 */

package com.trivir.idmunit.connector;

import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

public class MockSoapService extends AbstractConnector {
    private static Marker fatal = MarkerFactory.getMarker("FATAL");
    private static Logger log = LoggerFactory.getLogger(MockSoapService.class);
    private static Socket clientSocket = null;
    private static Thread acceptThread = null;
    private int serverPort = -1;

    public static String getSoapEnvelopeFromHTTPResponse(Socket inputClientSocket) throws IdMUnitException {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputClientSocket.getInputStream()));

            int soapEnvelopeLength = -1;
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    throw new IdMUnitException("Error reading request");
                }

                if (line.length() == 0) {
                    // End of HTTP headers
                    break;
                }

                if (line.startsWith("Content-Length")) {
                    soapEnvelopeLength = Integer.parseInt(line.substring(line.indexOf(":") + 2));
                }
            }

            if (soapEnvelopeLength == -1) {
                throw new IdMUnitException("No Content-Length header received.");
            }

            char[] soapEnvelope = new char[soapEnvelopeLength];
            in.read(soapEnvelope, 0, soapEnvelope.length);
            return new String(soapEnvelope);
        } catch (IOException e) {
            throw new IdMUnitException("failed while reading data from client: [" + e + "]");
        }
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        try {
            serverPort = Integer.parseInt(config.get(BasicConnector.CONFIG_SERVER));
        } catch (NumberFormatException e) {
            throw new IdMUnitException("Server URL provided must consist only of a port, but: [" + config.get(BasicConnector.CONFIG_SERVER) + "] was provided.");
        }
    }

    public void tearDown() throws IdMUnitException {
        // do nothing.
    }

    /**
     * Starts listener on configured port
     */
    public void opAddObject(Map<String, Collection<String>> data) throws IdMUnitException {
        log.info("Starting listener . .Server port:[" + serverPort + "]");
        acceptThread = new Thread(new SoapProcessor());
        acceptThread.start();
    }

    public void opValidateObject(Map<String, Collection<String>> data) throws IdMUnitException {

        // Confirm we've accepted the socket . .
        log.debug("Waiting on accept thread to complete . .");
        try {
            acceptThread.join(500);
        } catch (InterruptedException e) {
            // do nothing
        }

        if (acceptThread.isAlive()) {
            throw new IdMUnitFailureException("No message received");
        }

        if (clientSocket == null) {
            throw new IdMUnitException("clientSocket is not connected, please call addObject first!");
        }

        String request = null;
        String response = null;

        request = ConnectorUtil.getSingleValue(data, "request");
        response = ConnectorUtil.getSingleValue(data, "response");

        try {
            String requestStringFromIDM = getSoapEnvelopeFromHTTPResponse(clientSocket).replaceAll("[ \r\n\t]", "");
            request = request.replaceAll("[ \r\n\t]", "");

            String failMessage = null;
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            String responseCode = "200 OK";
            if (!requestStringFromIDM.matches(request)) {
                failMessage = "Received document from IDM did not match expected!\n" +
                        "Expected: [" + request + "]\n" +
                        "Received: [" + requestStringFromIDM + "]\n";

                responseCode = "400 Bad Request";
                response = "<status level=\"error\">Test failed.</status>";
            }
            String httpResponse =
                    "HTTP/1.1 " + responseCode + "\r\n" +
                            "Content-Type: application/soap+xml; charset=utf-8" +
                            "Content-Length: " + response.length() + "\r\n" +
                            "\r\n" +
                            response;

            out.println(httpResponse);
            out.close();

            if (failMessage != null) {
                throw new IdMUnitException(failMessage);
            }

        } catch (IOException e) {
            throw new IdMUnitException("exception while executing validating: [" + e + "]");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.info("Failed to close clientSocket connection", e);
            }
            clientSocket = null;
        }

    }

    class SoapProcessor implements Runnable {
        public void run() {

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(serverPort);
                log.debug("Created socket: [" + serverSocket + "], waiting for driver to connect . .");
                clientSocket = serverSocket.accept();
                log.debug("Accepted client socket . .");
                serverSocket.close();
            } catch (IOException e) {
                log.error(fatal, "Accept failed on port: [" + serverPort + "] :" + e);
                return;
            }
        }
    }
}
