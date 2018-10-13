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

package com.trivir.idmunit.connector.rest;

import com.google.gson.*;
import com.trivir.idmunit.connector.util.JWTUtil;
import lombok.Getter;
import lombok.Setter;
import org.idmunit.IdMUnitException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.net.HttpURLConnection.*;

public final class RestClient {
    private static final String ACCESS_TOKEN_AUD = "https://www.googleapis.com/oauth2/v3/token";

    private final String restServiceUrl;
    private final String authToken;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    private RestClient(String restServiceUrl, String authToken) {
        this.restServiceUrl = restServiceUrl;
        this.authToken = authToken;
    }

    public static RestClient init(String restServiceUrl, String serviceAccountEMail, PrivateKey key, String authUser, String[] scopes) throws IdMUnitException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String authToken = requestAuthToken(serviceAccountEMail, key, scopes, authUser);
        return new RestClient(restServiceUrl, authToken);
    }

    static String requestAuthToken(String serviceAccountEMail, PrivateKey key, String[] scopes, String superUser) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IdMUnitException {
        String jwt = JWTUtil.generateJWT(serviceAccountEMail, key, scopes, ACCESS_TOKEN_AUD, superUser);
        try {
            return sendAuthRequest(jwt);
        } catch (RestError e) {
            // First test that the service account email and key work for authentication.
            String[] minScopes = {"profile"}; // It seems like either 'profile', 'email', or 'openid' would be a good option here.
            try {
                sendAuthRequest(JWTUtil.generateJWT(serviceAccountEMail, key, minScopes, ACCESS_TOKEN_AUD, null));
            } catch (RestError ex) {
                throw new IdMUnitException("Unable to authenticate with '" + serviceAccountEMail + "' and the private key supplied.", ex);
            }

            if (e.httpStatusCode == HTTP_BAD_REQUEST) {
                if (e.errorDescription.equals("Not a valid email.")) {
                    throw new IdMUnitException("Unable to authenticate, possible problem with the subject '" + superUser + "'", e);
                }
            }

            if (e.httpStatusCode == HTTP_UNAUTHORIZED && "Unauthorized client or scope in request.".equals(e.errorDescription)) {
                throw new IdMUnitException("Unable to authenticate because Domain Wide Delegation is not enabled or the service account client ID is not authorized for any scopes", e);
            }

            if (e.httpStatusCode == HTTP_FORBIDDEN) { // Requested client not authorized.
                List<String> missingScopes = new ArrayList<String>();
                for (String scope : scopes) {
                    try {
                        sendAuthRequest(JWTUtil.generateJWT(serviceAccountEMail, key, new String[]{scope}, ACCESS_TOKEN_AUD, superUser));
                    } catch (RestError rer) {
                        missingScopes.add(scope);
                    }
                }
                if (missingScopes.size() > 0) {
                    throw new IdMUnitException("Unable to authenticate, the following scopes are not authorized for the service account '" +
                            JWTUtil.join(missingScopes.toArray(new String[missingScopes.size()]), ", ") + "'");
                }
            }

            throw new IdMUnitException("Unable to authenticate for an unknown reason.", e);
        }
    }

    static String sendAuthRequest(String jwt) throws IdMUnitException {
        String postData = encodeParameter("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer") + '&' + encodeParameter("assertion", jwt);
        Response response = internalRequest("POST", ACCESS_TOKEN_AUD, "application/x-www-form-urlencoded", null, postData);
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            String errorDescription = parseErrorDescription(response.messageBody);
            throw new RestError(response.statusCode, response.reasonPhrase, response.messageBody, errorDescription);
        }

        if (response.messageBody == null || response.messageBody.length() == 0) {
            throw new IdMUnitException("Server returned an empty response to authentication request");
        }

        Gson gson = new Gson();
        JsonElement element = gson.fromJson(response.messageBody, JsonElement.class);
        JsonObject jsonObj = element.getAsJsonObject();

        return jsonObj.get("access_token").toString().replaceAll("\"", "");
    }

    private static Response internalRequest(String method, String urlString, String contentType, String authToken, String request) throws IdMUnitException {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new IdMUnitException("Bad host or path specified.", e);
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            if ("PATCH".equals(method)) {
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                method = "POST";
            }
            conn.setRequestMethod(method);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", contentType);
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            conn.setUseCaches(false);

            if (request != null) {
                byte[] requestBytes = request.getBytes("UTF-8");
                conn.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
                OutputStream os = conn.getOutputStream();
                os.write(requestBytes);
                os.flush();
            }

            InputStream is;
            if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) { // 400
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            StringBuilder messageBody = new StringBuilder();

            // The input stream will be null if the http response body is empty.
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                int c;
                while ((c = br.read()) != -1) {
                    messageBody.append((char) c);
                }
            }

            conn.disconnect();

            return new Response(conn.getResponseCode(), conn.getResponseMessage(), messageBody.toString());
        } catch (IOException e) {
            throw new IdMUnitException("Error sending or receiving request.", e);
        }
    }

    private static String encodeParameters(Map<String, String> params) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> param : params.entrySet()) {
            if (result.length() != 0) {
                result.append('&');
            }

            result.append(encodeParameter(param.getKey(), param.getValue()));
        }

        return result.toString();
    }

    private static String encodeParameter(String name, String value) {
        return urlEncode(name) + '=' + urlEncode(value);
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error URL encoding '" + s + "'", e);
        }
    }

    /*
     * An error response will have one of two formats. Here are examples of both:
     *
     * {
     *  "error": "invalid_grant",
     *  "error_description": "Not a valid email."
     * }
     *
     * All errors in this format have "domain", "reason", and "message" properties
     * in each member of the errors array.
     *
     * {
     *  "error": {
     *   "errors": [
     *    {
     *     "domain": "global",
     *     "reason": "required",
     *     "message": "Login Required",
     *     "locationType": "header",
     *     "location": "Authorization"
     *    }
     *   ],
     *   "code": 401,
     *   "message": "Login Required"
     *  }
     * }
     *
     * This method returns "error_description" in the first case and "message" in the second.
     */
    public static String parseErrorDescription(String errorJson) {
        JsonElement jsonElement = new JsonParser().parse(errorJson);
        JsonObject parentObject = jsonElement.getAsJsonObject();

        JsonElement errorElement = parentObject.get("error");
        if (errorElement.isJsonPrimitive()) {
            return parentObject.get("error_description").getAsString();
        } else {
            JsonObject errorObject = errorElement.getAsJsonObject();
            JsonElement errorMessage = errorObject.get("message");
            if (errorMessage != null && errorMessage.isJsonPrimitive()) {
                return errorMessage.getAsString();
            } else {
                return "";
            }
        }
    }

    public Response executeDelete(String path) throws IdMUnitException {
        return executeRequest("DELETE", path, null);
    }

    public Response executeGet(String path) throws IdMUnitException {
        return executeGet(path, new HashMap<String, String>());
    }

    public Response executeGet(String path, Map<String, String> queryParams) throws IdMUnitException {
        if (queryParams.size() != 0) {
            path = String.format("%s?%s", path, encodeParameters(queryParams));
        }
        return executeRequest("GET", path, null);
    }

    @SuppressWarnings("unused")
    public Response executePost(String path) throws IdMUnitException {
        return executePost(path, new JsonObject());
    }

    public Response executePost(String path, JsonElement requestBody) throws IdMUnitException {
        return executeRequest("POST", path, gson.toJson(requestBody));
    }

    public Response executePost(String path, String body) throws IdMUnitException {
        return executeRequest("POST", path, body);
    }

    public Response executePut(String path, JsonElement requestBody) throws IdMUnitException {
        return executeRequest("PUT", path, gson.toJson(requestBody));
    }

    public Response executePut(String path, String body) throws IdMUnitException {
        return executeRequest("PUT", path, body);
    }

    public Response executePatch(String path, JsonElement requestBody) throws IdMUnitException {
        return executeRequest("PATCH", path, gson.toJson(requestBody));
    }

    public Response executePatch(String path, String body) throws IdMUnitException {
        return executeRequest("PATCH", path, body);
    }

    //TODO: Fix exception handling - Right now it will simply return the Response, the caller will be responsible
    // to check the status code and messages.
    private Response executeRequest(String method, String path, String request) throws IdMUnitException {
        String url = String.format("%s%s", restServiceUrl, path);
        return internalRequest(method, url, "application/json", authToken, request);
    }

    @Getter
    @Setter
    public static final class Response {
        int statusCode;
        String reasonPhrase;
        String messageBody;

        Response(int statusCode, String reasonPhrase, String messageBody) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.messageBody = messageBody;
        }

        @Override
        public String toString() {
            return Integer.toString(statusCode) + " " + reasonPhrase + "\n" + messageBody;
        }
    }
}
