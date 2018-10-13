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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;


public final class CurlClient {
    private final String url;
    private boolean sslConnect = false;

    private CurlClient(String url) {
        this.url = url;
    }

    static CurlClient init(String url) {
        return new CurlClient(url);
    }

    Response executeDelete(String path, Map<String, String> headers) throws IdMUnitException {
        return executeRequest("DELETE", path, null, headers);
    }

    Response executeGet(String path) throws IdMUnitException {
        return executeRequest("GET", path, null);
    }

    Response executeGet(String path, Map<String, String> headers) throws IdMUnitException {
        return executeRequest("GET", path, null, headers);
    }

    Response executePut(String path) throws IdMUnitException {
        return executePut(path, null);
    }

    Response executePut(String path, String request) throws IdMUnitException {
        return executeRequest("PUT", path, request);
    }

    Response executePut(String path, String request, Map<String, String> headers) throws IdMUnitException {
        return executeRequest("PUT", path, request, headers);
    }

    Response executePost(String path) throws IdMUnitException {
        return executePost(path, null);
    }

    Response executePost(String path, String request) throws IdMUnitException {
        return executeRequest("POST", path, request);
    }

    Response executePost(String path, String request, Map<String, String> headers) throws IdMUnitException {
        return executeRequest("POST", path, request, headers);
    }

    private Response executeRequest(String method, String path, String request) throws IdMUnitException {
        return executeRequest(method, path, request, null);
    }

    private Response executeRequest(String method, String path, String request, Map<String, String> additionalHeaders) throws IdMUnitException {
        return executeRequestReturnRawResponse(method, path, request, additionalHeaders);
    }

    private Response executeRequestReturnRawResponse(String method, String path, String request) throws IdMUnitException {
        return executeRequestReturnRawResponse(method, path, request, null);
    }

    private Response executeRequestReturnRawResponse(String method, String urlString, String request, Map<String, String> headers) throws IdMUnitException {
        try {
            URL url1  = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(method);

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> headerItem : headers.entrySet()) {
                    conn.setRequestProperty(headerItem.getKey(), headerItem.getValue());
                }
            }

            if (request != null) {
                OutputStream os = conn.getOutputStream();
                os.write(request.getBytes("UTF8"));
                os.flush();
            }

            InputStream is;
            if (conn.getResponseCode() < 400) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            StringBuilder messageBody = new StringBuilder();
            int c;
            while ((c = br.read()) != -1) {
                messageBody.append((char) c);
            }

            conn.disconnect();

            return new Response(conn.getResponseCode(), conn.getResponseMessage(), messageBody.toString());
        } catch (MalformedURLException e) {
            throw new IdMUnitException("Bad host or path specified.", e);
        } catch (IOException e) {
            throw new IdMUnitException("Error sending or receiving request.", e);
        }
    }

    static class Response {
        int statusCode;
        String reasonPhrase;
        String messageBody;

        Response(int statusCode, String reasonPhrase, String messageBody) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.messageBody = messageBody;
        }
    }
}
