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

import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.ConnectionConfigData;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Tests not working yet . .
 *
 * @author TriVir
 */
public class MockSoapServiceTests extends TestCase {
    MockSoapService mss = new MockSoapService();

    String addInteldocsXML =
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<SOAP-ENV:Body>" +
                    "<addInteldocs>" +
                    "<teamid>teama</teamid>" +
                    "<fullname>Team A</fullname>" +
                    "</addInteldocs>" +
                    "</SOAP-ENV:Body>" +
                    "</SOAP-ENV:Envelope>";

    String addInteldocsXMLResponse =
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<SOAP-ENV:Body>" +
                    "<SOAP-ENV:addInteldocsResponse>" +
                    "<success>true</success>" +
                    "<message>Team folder created: jinteldocs</message>" +
                    "<inteldocsfolderid>123</inteldocsfolderid>" +
                    "<inteldocsurl>http://lindev3o.lab.ismc.intelink.gov/inteldocs/browse.php?fFolderId=123</inteldocsurl>" +
                    "</SOAP-ENV:addInteldocsResponse>" +
                    "</SOAP-ENV:Body>" +
                    "<operation-data>" +
                    "<return-to-me association=\"jinteldocs\" class-name=\"Group\" event-id=\"IMOFSDTEST3O-NDS#20080124193308#1#1\" qualified-src-dn=\"C=US\\O=U.S. Government\\OU=Passport\\OU=Groups\\CN=jinteldocs\" src-dn=\"\\PASSPORT_DEV\\US\\U.S. Government\\Passport\\Groups\\jinteldocs\" src-entry-id=\"34493\" xds-event=\"add\"/>" +
                    "</operation-data>" +
                    "</SOAP-ENV:Envelope>";

    private static void addSingleValue(Map<String, Collection<String>> data, String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(name, values);
    }

    public void testConnect() throws IdMUnitException, IOException {
        ConnectionConfigData ccd = new ConnectionConfigData("", "");
        ccd.setParam("server", "4444");
        mss.setup(ccd.getParams());
        mss.opAddObject(null);

        // Connect to our MockConnection:
        // wait just a bit to ensure we've hit accept on the lister socket.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //ignore exception
        }

        Socket mockDriverDataSocket = new Socket("localhost", 4444);

        //System.out.println("Client port: started, client socket contains: [" + mockDriverDataSocket + "]");

        PrintWriter out = new PrintWriter(new OutputStreamWriter(mockDriverDataSocket.getOutputStream()), true);
        //BufferedReader in = new BufferedReader(new InputStreamReader(mockDriverDataSocket.getInputStream()));

        Map<String, Collection<String>> assertedAttrs = new HashMap<String, Collection<String>>();
        addSingleValue(assertedAttrs, "request", addInteldocsXML);
        addSingleValue(assertedAttrs, "response", addInteldocsXMLResponse);

        out.println(addInteldocsXML);


        mss.opValidateObject(assertedAttrs);

        String finalDoc = MockSoapService.getSoapEnvelopeFromHTTPResponse(mockDriverDataSocket);
        /*
        StringBuffer finalDoc = new StringBuffer("");
        int oneChar;

        while ((oneChar = in.read()) != -1) {
            finalDoc.append((char)oneChar);
            System.out.print((char)oneChar);
            if (finalDoc.toString().toLowerCase().endsWith("</soap-env:envelope>")) {
                break;
            }
        }
        */
        System.out.println();
        System.out.println("Response: [" + finalDoc + "]");

        //finalDoc = finalDoc.replaceAll("[ \r\n\t]", "");


    }
}
