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

import javax.net.ssl.SSLHandshakeException;
import java.security.cert.CertificateException;
import java.util.*;


public class TestOpenIdmNoTrustCert extends TestCase {
    private static final String SERVER = "server";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SSL_CONNECTION = "sslConnection";
    private static final String PORT = "port";
    private static final String CONFIG_TRUST_ALL_CERTS = "trust-all-certs";

    private static final Map<String, String> CONFIG;

    static {
        CONFIG = new HashMap<String, String>();
        CONFIG.put(SERVER, "172.17.2.70");
        CONFIG.put(PORT, "8443");
        CONFIG.put(USER, "openidm-admin");
        CONFIG.put(PASSWORD, "openidm-admin");
        CONFIG.put(SSL_CONNECTION, "true");
        CONFIG.put(CONFIG_TRUST_ALL_CERTS, "false");

    }

    private OpenIdmConnector connector;

    public void setUp() throws Exception {
        connector = new OpenIdmConnector();
        connector.setup(CONFIG);
    }

    public void tearDown() throws Exception {
        RestClient rest = RestClient.init("172.17.2.70", "8080", "openidm-admin", "openidm-admin", false);
        try {
            rest.executeDelete("/managed/user/tuser2_id");
        } catch (IdMUnitException ignore) {
            // ignore any error cleaning up
        }

        connector.tearDown();
    }

    public void testUserCRUDSslConnection() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("userName", singleValue("tuser2"));
        attrs.put("givenName", singleValue("Test"));
        attrs.put("sn", singleValue("User"));
        attrs.put("mail", singleValue("tuser@example.com"));
        attrs.put("telephoneNumber", singleValue("555-555-1212"));
        attrs.put("password", singleValue("T3stPassw0rd"));
        attrs.put("_id", singleValue("tuser2_id"));
        attrs.put("objectType", singleValue("user"));

        try {
            connector.execute("AddObject", Collections.unmodifiableMap(attrs));

            fail();
        } catch (IdMUnitException e) {
            if (!(e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertificateException)) {
                throw e;
            }
            //We Know that the default cert we are sending has no subject and thus throws an error, we catch that error and allow our test to pass.
        }
    }

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }
}
