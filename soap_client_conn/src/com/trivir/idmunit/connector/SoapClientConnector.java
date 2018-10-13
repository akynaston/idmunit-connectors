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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoapClientConnector extends AbstractConnector {
    static final String CONFIG_HTTP_REQUEST_HEADERS = "httpRequestHeaders";

    static final String ATTR_URL = "url";
    static final String ATTR_REQUEST = "request";
    static final String ATTR_RESPONSE = "response";
    private static Logger log = LoggerFactory.getLogger(SoapClientConnector.class);
    protected String username;
    protected String password;
    // TODO: support the validation of httpResponseHeaders .. hasn't been needed so it hasn't been implemented.
    protected Set<String> httpRequestHeaders;

    public void tearDown() {
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        username = config.get(BasicConnector.CONFIG_USER);
        password = config.get(BasicConnector.CONFIG_PASSWORD);
        String headers = config.get(SoapClientConnector.CONFIG_HTTP_REQUEST_HEADERS);
        httpRequestHeaders = new HashSet<String>();
        if (headers != null) {
            for (String header : headers.split(" *, *")) {
                httpRequestHeaders.add(header);
            }
        }
    }

    public void opValidateXpath(Map<String, Collection<String>> data) throws IdMUnitException {
        final String url = ConnectorUtil.getSingleValue(data, ATTR_URL);
        final String request = ConnectorUtil.getSingleValue(data, ATTR_REQUEST);
        final String expectedResponse = ConnectorUtil.getSingleValue(data, ATTR_RESPONSE);

        Map<String, Collection<String>> headerValues = new HashMap<String, Collection<String>>(data);
        headerValues.keySet().retainAll(httpRequestHeaders);

        String response = getResponse(url, request, headerValues);

        try {
            log.debug("Expected: " + expectedResponse);
            log.debug("Actual  : " + response);
            XMLAssert.assertXpathExists(expectedResponse, response);
            log.info("Response: " + response);
        } catch (IOException e) {
            throw new IdMUnitFailureException("The xpath expression didn't match. [" + expectedResponse + "] did not match:\n\t" + response, e);
        } catch (SAXException e) {
            throw new IdMUnitFailureException("The xpath expression didn't match. [" + expectedResponse + "] did not match:\n\t" + response, e);
        } catch (XpathException e) {
            throw new IdMUnitFailureException("The xpath expression didn't match. [" + expectedResponse + "] did not match:\n\t" + response, e);
        }
    }

    public void opValidateRegex(Map<String, Collection<String>> data) throws IdMUnitException {
        final String url = ConnectorUtil.getSingleValue(data, ATTR_URL);
        final String request = ConnectorUtil.getSingleValue(data, ATTR_REQUEST);
        final String expectedResponse = ConnectorUtil.getSingleValue(data, ATTR_RESPONSE);

        Map<String, Collection<String>> headerValues = new HashMap<String, Collection<String>>(data);
        headerValues.keySet().retainAll(httpRequestHeaders);

        String response = getResponse(url, request, headerValues);

        log.debug("ExpectedResponse: " + expectedResponse);
        log.debug("Response(Actual): " + response);
        log.debug("Did they match?: " + response.matches(expectedResponse));

        Pattern pattern = Pattern.compile(expectedResponse, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (!matcher.matches()) {
            throw new IdMUnitFailureException("The regex expression didn't match the response. Regex: [" + expectedResponse + "] did not match:\n\t" + response);
        }
    }

    public String getResponse(String url, String request, Map<String, Collection<String>> httpHeaders) throws IdMUnitException {
        StringEntity requestEntity;
        try {
            requestEntity = new StringEntity(request, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IdMUnitException("Error building request entity.", e);
        }

        requestEntity.setContentType("text/xml");

        HttpClient client = buildHttpClient(url, username, password);
        HttpPost post = new HttpPost(url);
        post.setEntity(requestEntity);

        // Add configured HTTP headers:
        for (String header : httpHeaders.keySet()) {
            post.addHeader(header, ConnectorUtil.getFirstValue(httpHeaders, header));
        }

        ResponseHandler<String> handler = new ResponseHandler<String>() {
            public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                if (httpResponse.getStatusLine().getStatusCode() == 401) {
                    throw new IOException("Authentication failed. Please check your authentication credentials.");
                }

                HttpEntity responseEntity = httpResponse.getEntity();
                if (responseEntity != null) {
                    return EntityUtils.toString(responseEntity);
                } else {
                    return null;
                }
            }
        };

        String response;
        try {
            response = client.execute(post, handler);
        } catch (IOException e) {
            throw new IdMUnitException("Error sending request.", e);
        }
        return response;
    }

    public HttpClient buildHttpClient(String urlString, String clientUsername, String clientPassword) throws IdMUnitException {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        if (clientUsername == null && clientPassword != null) {
            throw new IdMUnitException("SoapClientConnector username isn't setup correctly in the idmunit-config file.");
        }

        URI uri;
        int port = -1;
        boolean isHttps = false;
        String scheme;
        try {
            uri = new URI(urlString);
            scheme = uri.getScheme().toLowerCase();

            if (!("https".equals(scheme) || "http".equals(scheme))) {
                throw new IdMUnitException(String.format("SOAP URL contains unknown scheme '%s'.", uri.getScheme()));
            }

            if ("http".equals(scheme)) {
                if (uri.getPort() == -1) {
                    port = 80;
                } else {
                    port = uri.getPort();
                }
            } else if ("https".equals(scheme)) {
                if (uri.getPort() == -1) {
                    port = 443;
                } else {
                    port = uri.getPort();
                }
                isHttps = true;
            }

        } catch (URISyntaxException e) {
            throw new IdMUnitException("Soap URL has bad syntax.", e);
        }

        if (clientUsername != null) {
            this.username = clientUsername.trim();
            this.password = clientPassword == null ? "" : clientPassword.trim();
            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_SCHEME),
                    new UsernamePasswordCredentials(clientUsername, clientPassword));
        }

        if (!isHttps) {
            return httpClient; //If it isn't https, then we are done here.
        }

        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new IdMUnitException("SSLContext Instance doesn't exist", e);
        }

        X509TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        try {
            ctx.init(null, new TrustManager[]{tm}, null);
        } catch (KeyManagementException e) {
            throw new IdMUnitException("SSLContext failed to initialize.", e);
        }
        SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        ClientConnectionManager ccm = httpClient.getConnectionManager();
        SchemeRegistry sr = ccm.getSchemeRegistry();
        sr.register(new Scheme(scheme, port, ssf));

        return httpClient;
    }
}
