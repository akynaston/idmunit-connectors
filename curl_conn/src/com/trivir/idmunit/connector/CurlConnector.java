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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

public class CurlConnector extends AbstractConnector {
    private static final String CONFIG_TRUST_ALL_CERTS = "trust-all-certs";

    private static Logger log = LoggerFactory.getLogger(CurlConnector.class);

    private CurlClient curlClient;

    @Override
    public void setup(Map<String, String> config) throws IdMUnitException {
        if (config.get(CONFIG_TRUST_ALL_CERTS) != null && Boolean.valueOf(config.get(CONFIG_TRUST_ALL_CERTS))) {
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, new TrustManager[] {new TrustAllX509TrustManager()}, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String string, SSLSession ssls) {
                        return true;
                    }
                });
            } catch (NoSuchAlgorithmException e) {
                //handle exception
            } catch (KeyManagementException e) {
                //handle exception
            }

        }
    }

    public void opAction(Map<String, Collection<String>> attrs) throws IdMUnitException {
        validateAttrs(attrs, "ACTION");
        String url = ConnectorUtil.getSingleValue(attrs, "url");
        String method = ConnectorUtil.getSingleValue(attrs, "method").toLowerCase();
        String body = ConnectorUtil.getSingleValue(attrs, "body");

        Map<String, String> headers = new HashMap<String, String>();
        if (attrs.get("headers") != null) {
            for (String header : attrs.get("headers")) {
                String[] headerSplit = header.split("=", 2);
                headers.put(headerSplit[0], headerSplit[1]);
            }
        }

        curlClient = CurlClient.init(url);
        CurlClient.Response response;

        if ("post".equals(method)) {
            response = curlClient.executePost(url, body, headers);

        } else if ("put".equals(method)) {
            response = curlClient.executePut(url, body, headers);

        } else if ("delete".equals(method)) {
            response = curlClient.executeDelete(url, headers);

        } else if ("get".equals(method) || "patch".equals(method)) {
            throw new IdMUnitException(String.format("method [%s] is an unsupported HTTP method for 'opAction'", method));
        } else {
            throw new IdMUnitException(String.format("method [%s] is an unknown HTTP method", method));
        }

        if (response.statusCode >= 400) {
            if (response.statusCode == 404 && "delete".equals(method)) {
                log.info("Ignoring 404 error because the operation was delete");
            } else {
                throw new IdMUnitException(String.format("opAction was unsuccessful. Response [status: '%s' reason: '%s' body: '%s']", response.statusCode, response.reasonPhrase, response.messageBody));
            }
        }
    }

    public void opValidate(Map<String, Collection<String>> attrs) throws IdMUnitException {
        validateAttrs(attrs, "VALIDATE");
        String url = ConnectorUtil.getSingleValue(attrs, "url");
        String method = ConnectorUtil.getSingleValue(attrs, "method").toLowerCase();
        String body = ConnectorUtil.getSingleValue(attrs, "body");
        String responseBody = ConnectorUtil.getSingleValue(attrs, "responseBody");
        String statusCodeString = ConnectorUtil.getSingleValue(attrs, "statusCode");
        Integer statusCode = null;

        if (statusCodeString != null) {
            statusCode = Integer.valueOf(statusCodeString);
        }

        Map<String, String> headers = new HashMap<String, String>();
        for (String header : attrs.get("headers")) {
            String[] headerSplit = header.split("=", 2);
            headers.put(headerSplit[0], headerSplit[1]);
        }

        CurlClient.Response response;

        curlClient = CurlClient.init(url);

        if ("get".equals(method)) {
            response = curlClient.executeGet(url, headers);

        } else if ("post".equals(method)) {
            response = curlClient.executePost(url, body, headers);

        } else if ("put".equals(method)) {
            response = curlClient.executePut(url, body, headers);

        } else if ("delete".equals(method)) {
            response = curlClient.executeDelete(url, headers);
        } else if ("patch".equals(method)) {
            headers.put("X-HTTP-Method-Override", "PATCH");
            log.info("true PATCH method not supported. Trying POST with 'X-HTTP-Method-Override=PATCH' header...");
            response = curlClient.executePost(url, body, headers);
        } else {
            throw new IdMUnitException(String.format("Method [%s] is an unknown HTTP method.", method));
        }

        if (responseBody != null) {
            JsonObject expectedOutput = new JsonParser().parse(responseBody).getAsJsonObject();
            JsonObject actualOutput = new JsonParser().parse(response.messageBody).getAsJsonObject();
            List<String> differences = jsonMatches(expectedOutput, actualOutput);
            if (differences.size() > 0) {
                throw new IdMUnitException("Validation failed: " + differences.toString());
            }
        }

        if (statusCode != null) {
            if (statusCode != response.statusCode) {
                throw new IdMUnitException(String.format("Validation failed: expected statusCode of [%s] but was [%s].", statusCode, response.statusCode));
            }
        }
    }

    private void validateAttrs(Map<String, Collection<String>> attrs, String opType) throws IdMUnitException {
        String url = ConnectorUtil.getSingleValue(attrs, "url");
        String method = ConnectorUtil.getSingleValue(attrs, "method");
        String responseBody = ConnectorUtil.getSingleValue(attrs, "responseBody");
        String statusCode = ConnectorUtil.getSingleValue(attrs, "statusCode");
        if ("ACTION".equals(opType)) {
            if (url == null || method == null) {
                StringBuilder sb = new StringBuilder();
                if (url == null) {
                    sb.append(" url");
                }
                if (method == null) {
                    sb.append(" method");
                }

                throw new IdMUnitException(String.format("The following required attributes were not found:%s", sb.toString()));
            }
        } else if ("VALIDATE".equals(opType)) {
            if (url == null || method == null || (responseBody == null && statusCode == null)) {
                StringBuilder sb = new StringBuilder();
                if (url == null) {
                    sb.append(" url");
                }
                if (responseBody == null && statusCode == null) {
                    sb.append(" one of either responseBody or statusCode");
                }

                throw new IdMUnitException(String.format("The following required attributes were not found:%s", sb.toString()));
            }
        }
    }

    static List<String> jsonMatches(JsonElement expected, JsonElement actual) {
        List<String> differences = new ArrayList<String>();
        jsonMatches(differences, "", expected, actual);
        return differences;
    }

    static void jsonMatches(List<String> differences, String baseName, JsonElement expected, JsonElement actual) {
        Gson gson = new Gson();

        if (expected.isJsonPrimitive()) {
            // Java null means the attribute is missing
            if (actual == null || actual.isJsonNull()) {
                if (!expected.getAsString().isEmpty()) {
                    differences.add(String.format("'%s' attribute mismatch: expected %s but was null", baseName, gson.toJson(expected)));
                }
                return;
            }

            if (!actual.isJsonPrimitive() || !actual.getAsString().matches(expected.getAsString())) {
                if (actual.getAsString().equals(expected.getAsString())) {
                    log.info(String.format("'%s' attribute failed regex check but passed equals check", baseName));
                } else {
                    differences.add(String.format("'%s' attribute mismatch: expected %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
                }
            }
            return;
        }

        if (expected.isJsonArray()) {
            if (actual == null || actual.isJsonNull()) {
                if (expected.getAsJsonArray().size() != 0) {
                    differences.add(String.format("'%s' attribute mismatch: expected %s but was null", baseName, gson.toJson(expected)));
                }
                return;
            }

            if (!actual.isJsonArray()) {
                differences.add(String.format("'%s' attribute mismatch: expected an array %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
                return;
            }

            // Validating an array is a "contains" operation, it ignores ordering of the items in the array
            Set<JsonElement> actualItems = new LinkedHashSet<JsonElement>();
            for (JsonElement item : actual.getAsJsonArray()) {
                actualItems.add(item);
            }
            for (JsonElement expectedItem : expected.getAsJsonArray()) {
                int actualItemSize = actualItems.size();
                for (Iterator<JsonElement> i = actualItems.iterator(); i.hasNext(); ) {
                    JsonElement actualItem = i.next();
                    List<String> d = new ArrayList<String>();
                    jsonMatches(d, baseName + "[]", expectedItem, actualItem);
                    if (d.size() == 0) {
                        i.remove();
                        break;
                    }
                }
                if (actualItems.size() == actualItemSize) {
                    differences.add(String.format("'%s' attribute mismatch: expected item %s was not found in %s", baseName, gson.toJson(expectedItem), gson.toJson(actual)));
                }
            }
            return;
        }

        if (expected.isJsonObject()) {
            if (actual == null || actual.isJsonNull()) {
                differences.add(String.format("'%s' attribute mismatch: expected an object %s but was null", baseName, gson.toJson(expected)));
                return;
            }
            if (!actual.isJsonObject()) {
                differences.add(String.format("'%s' attribute mismatch: expected an object %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
                return;
            }
            JsonObject actualObj = actual.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : expected.getAsJsonObject().entrySet()) {
                String name  = entry.getKey();
                JsonElement expectedValue = entry.getValue();
                JsonElement actualValue = actualObj.get(name);
                jsonMatches(differences, baseName + "." + name, expectedValue, actualValue);
            }
        }
    }

    private static class TrustAllX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
