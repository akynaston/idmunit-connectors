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

import junit.framework.TestCase;
import org.idmunit.injector.Injection;
import org.idmunit.IdMUnitException;

public class IdFromQueryFilterInjectionTest extends TestCase {
    private static final String TEST_SERVER = "10.10.30.249";
    private static final String TEST_SERVER_PORT = "8080";
    private static final String TEST_SERVER_PORT_SSL = "8443";

    private Injection injector;


    @Override
    protected void setUp() throws Exception {
        injector = new IdFromQueryFilterInjection();
    }

    public void testJsonConfig() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'," +
                " 'useSSL' : false," +
                " 'username' : 'openidm-admin'," +
                " 'password' : 'openidm-admin'" +
                " }";

        injector.mutate(jsonConfig);
    }

    public void testJsonConfigMissingValues() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                // Missing Port
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'," +
                " 'useSSL' : false," +
                " 'username' : 'openidm-admin'," +
                " 'password' : 'openidm-admin'" +
                " }";

        try {
            injector.mutate(jsonConfig);
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().equals("IdFromQueryFilterInjection configuration JSON object missing required value(s): [port]"));
        }
    }

    public void testJsonConfigUseDefaultValues() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'" +
                // USESSL DEFAULTS TO FALSE
                // USERNAME DEFAULTS TO 'openidm-admin'
                // PASSWORD DEFAULTS TO 'openidm-admin'
                " }";

        injector.mutate(jsonConfig);
    }

    public void testNoUserResult() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"fakeuser\"'" +
                " }";
        injector.mutate(jsonConfig);

        try {
            injector.getDataInjection(null);
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().startsWith("Found no results from the provided query"));
        }
    }

    public void testSingleUserResult() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'," +
                " 'useSSL' : false," +
                " 'username' : 'openidm-admin'," +
                " 'password' : 'openidm-admin'" +
                " }";
        injector.mutate(jsonConfig);

        injector.getDataInjection(null);
    }

    public void testSingleUserResultUsingDefaults() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'" +
                " }";
        injector.mutate(jsonConfig);

        injector.getDataInjection(null);
    }

    public void testSingleUserResultUsingSSLTrustAllCerts() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT_SSL + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'," +
                " 'useSSL' : true," +
                " 'trustAllCerts' : true," +
                " 'username' : 'openidm-admin'," +
                " 'password' : 'openidm-admin'" +
                " }";
        injector.mutate(jsonConfig);

        injector.getDataInjection(null);
    }

    public void testSingleUserResultUsingEncryptedPassword() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT_SSL + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'," +
                " 'useSSL' : true," +
                " 'trustAllCerts' : true," +
                " 'username' : 'openidm-admin'," +
                " 'password' : 'ENC::xtXVCEVo8ewcwC8ykv3ACw=='" +
                " }";
        injector.mutate(jsonConfig);

        injector.getDataInjection(null);
    }

    public void testMultipleUserResult() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'givenName+eq+\"test\"'" +
                " }";
        injector.mutate(jsonConfig);

        try {
            injector.getDataInjection(null);
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().startsWith("Found more than one result from the provided query"));
        }
    }

}
