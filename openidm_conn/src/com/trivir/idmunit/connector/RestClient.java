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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.idmunit.IdMUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

final class RestClient {
    private static Logger log = LoggerFactory.getLogger(RestClient.class);

    private final String server;
    private final String username;
    private final String password;
    private boolean sslConnect = false;

    private RestClient(String server, String port, String username, String password, boolean sslConnect) {
        this.server = server + ":" + port;
        this.username = username;
        this.password = password;
        this.sslConnect = sslConnect;
    }

    static RestClient init(String server, String port, String username, String password, boolean sslConnect) {
        return new RestClient(server, port, username, password, sslConnect);
    }

    Response executeDelete(String path) throws IdMUnitException {
        return executeDelete(path, null);
    }

    Response executeDelete(String path, String expectedRev) throws IdMUnitException {
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        if (expectedRev != null) {
            additionalHeaders.put("If-Match", expectedRev);
        }
        return executeRequest("DELETE", path, null, additionalHeaders);
    }

    Response executeGet(String path) throws IdMUnitException {
        return executeRequest("GET", path, null);
    }

    Response executePost(String path) throws IdMUnitException {
        return executePost(path, null);
    }

    Response executePost(String path, String request) throws IdMUnitException {
        return executeRequest("POST", path, request);
    }

    private Response executeRequest(String method, String path, String request) throws IdMUnitException {
        return executeRequest(method, path, request, null);
    }

    private Response executeRequest(String method, String path, String request, Map<String, String> additionalHeaders) throws IdMUnitException {
        Response r = executeRequestReturnRawResponse(method, path, request, additionalHeaders);
        if (r.statusCode < 300) {
            return r;
        }

        log.debug("An error occurred sending request: method={}, path={}, request={}", method, path, request);

        // {"code":403,"reason":"Forbidden","message":"Policy validation failed","detail":{"result":false,"failedPolicyRequirements":[{"policyRequirements":[{"policyRequirement":"CANNOT_CONTAIN_OTHERS","params":{"disallowedFields":"givenName"}}],"property":"password"}]}}

        JsonObject response;
        try {
//            response = new Gson().fromJson(r.messageBody, STRING_STRING_HASHMAP_TYPE);
            response = new JsonParser().parse(r.messageBody).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new IdMUnitException("Unexpected message body in response: " + r.messageBody);
        }

        JsonElement code = response.get("code");
        JsonElement reason = response.get("reason");
        JsonElement message = response.get("message");
        JsonElement detail = response.get("detail");

        if (code == null || reason == null || message == null) {
            return r;
        }

        if (detail == null) {
            throw new RestError(code.getAsString(), reason.getAsString(), message.getAsString());
        } else {
            throw new RestError(code.getAsString(), reason.getAsString(), message.getAsString(), detail.toString());
        }
    }

    private Response executeRequestReturnRawResponse(String method, String path, String request) throws IdMUnitException {
        return executeRequestReturnRawResponse(method, path, request, null);
    }

    private Response executeRequestReturnRawResponse(String method, String path, String request, Map<String, String> additionalHeaders) throws IdMUnitException {
        URL url;
        try {
            if (sslConnect) {
                url = new URL("https://" + server + "/openidm" + path);
            } else {
                url = new URL("http://" + server + "/openidm" + path);
            }
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-OpenIDM-Username", username);
            conn.setRequestProperty("X-OpenIDM-Password", password);

            if (additionalHeaders != null && !additionalHeaders.isEmpty()) {
                for (Map.Entry<String, String> headerItem : additionalHeaders.entrySet()) {
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
                messageBody.append((char)c);
            }

            conn.disconnect();

            return new Response(conn.getResponseCode(), conn.getResponseMessage(), messageBody.toString());
        } catch (MalformedURLException e) {
            throw new IdMUnitException("Bad host or path specified.", e);
        } catch (IOException e) {
            throw new IdMUnitException("Error sending or receiving request.", e);
        }
    }

//    private static final Type STRING_OBJECT_HASHMAP_TYPE = new TypeToken<HashMap<String,Object>>(){}.getType();

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
