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

package com.trivir.idmunit.injector;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.idmunit.EncTool;
import org.idmunit.parser.ExcelParser;
import org.idmunit.IdMUnitException;
import org.idmunit.injector.Injection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class IdFromQueryFilterInjection implements Injection {
    private static final Type STRING_STRING_HASHMAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
    private static final Logger LOGGER = LoggerFactory.getLogger(IdFromQueryFilterInjection.class);
    // for password decryption
    private static final String ENCRYPTION_PREFIX = "ENC::";
    private static final String ENCRYPTION_KEY = "EncryptionKey";
    private static final String DECRYPT_PASSWORDS = "DecryptPasswords";
    private static final String DEFAULT_ENCRYPTION_KEY = "IDMUNIT1";

    private Map<String, String> injectorConfig = null;
    private String systemObject;

    @Override
    public void mutate(String mutation) throws IdMUnitException {
        LOGGER.debug("Loading config for IdFromQueryFilterInjection...");
        if (mutation == null || "".equals(mutation)) {
            throw new IdMUnitException("Missing configuration for IdFromQueryFilterInjection. Please provide configuration as a 'mutator' in the idmunit config file. The 'mutator' value must be formatted as a JSON object with the following required attributes: 'host', 'port', 'systemObject', 'queryFilter'. The following attributes are optional: 'ssl' [default: false], 'trustAllCerts' [default: false], 'username' [default: openidm-admin], 'password' [default: openidm-admin].");
        }
        List<String> missingProps = new ArrayList<String>();
        Gson gson = new Gson();
        injectorConfig = gson.fromJson(mutation, STRING_STRING_HASHMAP_TYPE);
        if (injectorConfig.get("host") == null || injectorConfig.get("host").equals("")) {
            missingProps.add("host");
        }

        if (injectorConfig.get("port") == null || injectorConfig.get("port").equals("")) {
            missingProps.add("port");
        }

        if (injectorConfig.get("systemObject") == null || injectorConfig.get("systemObject").equals("")) {
            missingProps.add("systemObject");
        }

        if (injectorConfig.get("queryFilter") == null || injectorConfig.get("queryFilter").equals("")) {
            missingProps.add("queryFilter");
        }

        if (!missingProps.isEmpty()) {
            throw new IdMUnitException(String.format("IdFromQueryFilterInjection configuration JSON object missing required value(s): %s", missingProps.toString()));
        }

        if (injectorConfig.get("useSSL") == null || injectorConfig.get("useSSL").equals("")) {
            LOGGER.info("IdFromQueryFilterInjection configuration JSON object missing 'useSSL' value. Using default value of 'false'.");
            injectorConfig.put("useSSL", "false");
        }

        if (injectorConfig.get("trustAllCerts") == null || injectorConfig.get("trustAllCerts").equals("")) {
            LOGGER.info("IdFromQueryFilterInjection configuration JSON object missing 'trustAllCerts' value. Using default value of 'false'.");
            injectorConfig.put("trustAllCerts", "false");
        }

        if (injectorConfig.get("username") == null || injectorConfig.get("username").equals("")) {
            LOGGER.info("IdFromQueryFilterInjection configuration JSON object missing 'username' value. Using default value of 'openidm-admin'.");
            injectorConfig.put("username", "openidm-admin");
        }

        if (injectorConfig.get("password") == null || injectorConfig.get("password").equals("")) {
            LOGGER.info("IdFromQueryFilterInjection configuration JSON object missing 'password' value. Using default value of 'openidm-admin'.");
            injectorConfig.put("password", "openidm-admin");
        }
    }

    @Override
    public String getDataInjection(String formatter) throws IdMUnitException {
        LOGGER.debug("Setting up IdFromQueryFilterInjection...");
        if (injectorConfig == null) {
            throw new IdMUnitException("IdFromQueryFilterInjection config JSON was not loaded. Please provide configuration as a 'mutator' in the idmunit config file. The 'mutator' value must be formatted as a JSON object with the following required attributes: 'host', 'port', 'systemObject', 'queryFilter'. The following attributes are optional: 'useSSL' [default: false], 'trustAllCerts' [default: false], 'username' [default: openidm-admin], 'password' [default: openidm-admin].");
        }

        String host = injectorConfig.get("host");
        String port = injectorConfig.get("port");
        systemObject = injectorConfig.get("systemObject");
        String queryFilter = injectorConfig.get("queryFilter");
        String ssl = injectorConfig.get("useSSL");
        String trustAllCerts = injectorConfig.get("trustAllCerts");
        String username = injectorConfig.get("username");
        String password = injectorConfig.get("password");

        if (password.startsWith(ENCRYPTION_PREFIX)) {
            LOGGER.debug("Decrypting password...");
            String encryptedPassword = password.substring(ENCRYPTION_PREFIX.length());
            Properties properties;
            try {
                properties = loadProperties();
            } catch (IOException e) {
                throw new IdMUnitException("Error loading idmunit-defaults.properties", e);
            }
            String encryptionKey = null;
            String decryptPasswords = properties.getProperty(DECRYPT_PASSWORDS);
            if (decryptPasswords == null || Boolean.parseBoolean(decryptPasswords)) {
                encryptionKey = properties.getProperty(ENCRYPTION_KEY);
                if (encryptionKey == null) {
                    encryptionKey = DEFAULT_ENCRYPTION_KEY;
                }
            }
            EncTool encTool = new EncTool(encryptionKey);
            password = encTool.decryptCredentials(encryptedPassword);
        }

        Response response = executeQuery(host, port, queryFilter, ssl, trustAllCerts, username, password);
        LOGGER.debug("Query complete!");

        JsonParser jsonParser = new JsonParser();
        JsonObject resultsJsonObject = jsonParser.parse(response.messageBody).getAsJsonObject();
        int resultsSize = resultsJsonObject.get("resultCount").getAsInt();
        if (resultsSize < 1) {
            throw new IdMUnitException("Found no results from the provided query '" + queryFilter + "'.");
        }

        if (resultsSize > 1) {
            throw new IdMUnitException("Found more than one result from the provided query '" + queryFilter + "'.");
        }

        return resultsJsonObject.get("result").getAsJsonArray().get(0).getAsJsonObject().get("_id").getAsString();
    }

    String getSystemObject() {
        return systemObject;
    }

    private Response executeQuery(String host, String port, String queryFilter, String ssl, String trustAllCerts, String username, String password) throws IdMUnitException {
        LOGGER.debug("Executing query...");
        try {
            if (Boolean.parseBoolean(trustAllCerts) && Boolean.parseBoolean(ssl)) {
                TrustManager[] trustManager = new TrustManager[] {new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {

                    }
                }, };

                SSLContext sc;
                try {
                    sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustManager, new java.security.SecureRandom());
                } catch (NoSuchAlgorithmException e) {
                    throw new IdMUnitException("Fatal error in configuration of SSL TrustManager.");
                } catch (KeyManagementException e) {
                    throw new IdMUnitException("Fatal error in configuration of SSL TrustManager.");
                }

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                });
            }

            URL url;
            if (Boolean.parseBoolean(ssl)) {
                url = new URL("https://" + host + ":" + port);
            } else {
                url = new URL("http://" + host + ":" + port);
            }

            URI uri = new URI(url.getProtocol(), null, url.getHost(), url.getPort(), "/openidm/" + systemObject, "_queryFilter=" + queryFilter + "&_fields=_id", null);
            url = uri.toURL();

            HttpURLConnection conn;

            if (Boolean.parseBoolean(ssl)) {
                conn = (HttpsURLConnection) url.openConnection();
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-OpenIDM-Username", username);
            conn.setRequestProperty("X-OpenIDM-Password", password);

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
        } catch (IOException e) {
            throw new IdMUnitException("Error submitting query", e);
        } catch (URISyntaxException e) {
            throw new IdMUnitException("URL is malformed.", e);
        }
    }

    // TODO: This is borrowed code from the ExcelParser in order to get encryption key
    private static Properties loadProperties() throws IOException {
        Properties properties = System.getProperties();
        InputStream propertiesFile = ExcelParser.class.getClassLoader().getResourceAsStream("idmunit-defaults.properties");
        if (propertiesFile == null) {
            LOGGER.warn("Unable to find idmunit-defaults.properties");
        } else {
            Properties defaultProperties = new Properties();
            defaultProperties.load(propertiesFile);
            for (Enumeration<?> en = defaultProperties.propertyNames(); en.hasMoreElements(); ) {
                String key = (String)en.nextElement();
                if (!properties.containsKey(key)) {
                    properties.put(key, defaultProperties.getProperty(key));
                }
            }
        }
        return properties;
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
