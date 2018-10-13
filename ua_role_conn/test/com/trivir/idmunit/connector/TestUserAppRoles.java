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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.BasicConnector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jeremiah Seaver
 */
public class TestUserAppRoles extends TestCase {
    private static final String PROVISIONING_URL = "http://172.17.2.70:8180/IDMProv";
    private static final String USER_APP_ADMIN = "cn=uaadmin,dc=arrisi,dc=com";
    private static final String USER_APP_ADMIN_PASSWORD = "trivir";
    private static final String DEFAULT_RECIPIENT_DN = "cn=role_conn_test,dc=arrisi,dc=com";
    private static final String DEFAULT_ROLE_DN = "cn=role_conn_role,cn=Level30,cn=RoleDefs,cn=RoleConfig,cn=AppConfig,cn=UserApplication,cn=DriverSet,o=idm";

    private UserAppRoles classUnderTest;
    private Map<String, String> config;

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(TestUserAppRoles.class));
        return suite;
    }


    protected void setUp() throws Exception {
        config = new HashMap<String, String>() {{
                put(BasicConnector.CONFIG_SERVER, PROVISIONING_URL);
                put(BasicConnector.CONFIG_USER, USER_APP_ADMIN);
                put(BasicConnector.CONFIG_PASSWORD, USER_APP_ADMIN_PASSWORD);
            }};

        classUnderTest = new UserAppRoles();
        classUnderTest.setup(config);
    }

    public void tearDown() {
    }

    public void testOpTestConnection() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();

        classUnderTest.opTestConnection(data);
    }

    public void testOpTestConnectionFailure() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();

        config.put(BasicConnector.CONFIG_SERVER, "http://127.0.0.1:8080/Something");
        classUnderTest.setup(config);

        try {
            classUnderTest.opTestConnection(data);
            fail("Expected exception not thrown.");
        } catch (IdMUnitException e) {
            // Expected exception
        }
    }

    public void testOpAssignRole() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                        add(DEFAULT_ROLE_DN);
                    }});
            }};

        classUnderTest.opAssignRole(data);
    }

    public void testOpAssignRoleFail() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                        add("Something");
                    }});
            }};

        try {
            classUnderTest.opAssignRole(data);
            fail("Expected exception not thrown.");
        } catch (IdMUnitException e) {
            // Expected Exception
        }
    }

    public void testOpRevokeRole() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                        add(DEFAULT_ROLE_DN);
                    }});
            }};

        classUnderTest.opRevokeRole(data);
    }

    public void testOpRevokeRoleFail() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                        add("Something");
                    }});
            }};

        try {
            classUnderTest.opRevokeRole(data);
            fail("Expected exception not thrown.");
        } catch (IdMUnitException e) {
            // Expected Exception
        }
    }

    public void testOpUserInRoles() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                        add(DEFAULT_ROLE_DN);
                    }});
            }};

        classUnderTest.opAssignRole(data);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            //ignore exception
        }
        classUnderTest.opUserInRoles(data);
    }

    public void testOpUserInRolesFail() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                        add(DEFAULT_ROLE_DN);
                    }});
            }};

        classUnderTest.opRevokeRole(data);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            //ignore exception
        }
        try {
            classUnderTest.opUserInRoles(data);
            fail("Expected exception not thrown.");
        } catch (IdMUnitException e) {
            // Expected Exception
        }
    }

    public void testOpUserNotInRoles() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                        add(DEFAULT_ROLE_DN);
                    }});
            }};

        classUnderTest.opRevokeRole(data);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            //ignore exception
        }
        classUnderTest.opUserNotInRoles(data);
    }

    public void testOpUserNotInRolesFail() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                    put(UserAppRoles.STR_DN, new ArrayList<String>() {{
                            add(DEFAULT_RECIPIENT_DN);
                        }});
                put(UserAppRoles.ROLE_DN, new ArrayList<String>() {{
                            add(DEFAULT_ROLE_DN);
                        }});
                }};

        classUnderTest.opAssignRole(data);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            //ignore exception
        }
        try {
            classUnderTest.opUserNotInRoles(data);
            fail("Expected exception not thrown.");
        } catch (IdMUnitException e) {
            // Expected Exception
        }
    }
}
