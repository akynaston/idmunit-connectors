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
import org.idmunit.IdMUnitFailureException;
import org.idmunit.util.LdapConnectionHelper;


import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;

public class EdirLdapConnectorTest extends TestCase {
    DirContext ctx = null;

    String server = "10.10.30.249:636";
    String admin = "cn=admin,o=services";
    String adminPassword = "trivir";

    String usableTestContainer = "o=Users";

    // specify a useable test password that complies with userableTestContainer's password policy (if any exist)
    String passwordThatCompliesWithPolicy = "Mypassword#1234";

    EdirLdapConnector ldapConnectorTest = null;

    ArrayList<String> userCleanupList = new ArrayList<String>();

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(EdirLdapConnectorTest.class));
        return suite;
    }


    protected void setUp() throws Exception {
        Map<String, String> config = new HashMap<String, String>();
        config.put("server", server);
        config.put("user", admin);
        config.put("password", adminPassword);
        config.put(LdapConnectionHelper.CONFIG_TRUST_ALL_CERTS, "true");
        config.put(LdapConnectionHelper.CONFIG_USE_TLS, "true");
//        config.put("keystore-path", "")
        ldapConnectorTest = new EdirLdapConnector();
        ldapConnectorTest.setup(config);

        // create our manual ldap connection test object:
        /*Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.PROVIDER_URL, "ldaps://" + server);
        env.put(Context.SECURITY_PRINCIPAL, admin);
        env.put(Context.SECURITY_CREDENTIALS, adminPassword);
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");*/

        ctx = LdapConnectionHelper.createLdapConnection(config);

    }

    public void tearDown() throws Exception {

        for (String dn : userCleanupList) {
            try {
                ctx.destroySubcontext(dn);
            } catch (NamingException e) {
                // send a message that we couldn't delete user, but don't stop deleting.
                System.out.println("NOTE: failed to delete: [" + dn + "], exception: [" + e + "]");
            }
        }

        if (ldapConnectorTest != null) {
            ldapConnectorTest.tearDown();
            ldapConnectorTest = null;
        }

        if (ctx != null) {
            ctx.close();
            ctx = null;
        }
    }

    private String createSimpleUserWithPassword() throws EdirLdapConnectorTestException {
        return doCreateUser("userWithPass", true);
    }

    private String createSimpleUserNoPassword() throws EdirLdapConnectorTestException {
        return doCreateUser("userNoPass", false);
    }


    private String doCreateUser(String cn, boolean createWithPassword) throws EdirLdapConnectorTestException {
        String dn = "cn=" + cn + "," + usableTestContainer;

        try {
            ctx.lookup(dn);
            System.out.println("DN: [" + dn + "] already existed, deleting before re-creating . .");
            ctx.unbind(dn);
        } catch (NamingException e) {
            String errorMessage = e.getMessage().toUpperCase();
            if (!(errorMessage.indexOf("NO SUCH ENTRY") != -1 || errorMessage.indexOf("NO_OBJECT") != -1 || errorMessage.indexOf("OBJECT") != -1)) {
                throw new EdirLdapConnectorTestException("Deletion failure: Invalid DN: " + e.getMessage());
            }
        }


        Attribute attrOC = new BasicAttribute("objectClass", "User");
        Attribute attrSN = new BasicAttribute("sn", "lastname");
        Attribute attrPass = new BasicAttribute("userPassword", passwordThatCompliesWithPolicy);

        Attributes createAttrs = new BasicAttributes();
        createAttrs.put(attrOC);
        createAttrs.put(attrSN);
        if (createWithPassword) {
            createAttrs.put(attrPass);
        }

        try {
            ctx.createSubcontext(dn, createAttrs);
            userCleanupList.add(dn);
        } catch (NamingException e) {
            throw new EdirLdapConnectorTestException("Could not create user: [" + dn + "]", e);
        }

        return dn;

    }

    public void testPasswordExists() throws EdirLdapConnectorTestException, IdMUnitException {
        HashMap<String, Collection<String>> dataUserNoPassword = new HashMap<String, Collection<String>>();
        ArrayList<String> valuesnopass = new ArrayList<String>();

        HashMap<String, Collection<String>> dataUserWithPassword = new HashMap<String, Collection<String>>();
        ArrayList<String> valueswithpass = new ArrayList<String>();

        HashMap<String, Collection<String>> dataNoUser = new HashMap<String, Collection<String>>();
        ArrayList<String> valuesnoUser = new ArrayList<String>();

        valueswithpass.add(createSimpleUserWithPassword());
        dataUserWithPassword.put("dn", valueswithpass);

        valuesnopass.add(createSimpleUserNoPassword());
        dataUserNoPassword.put("dn", valuesnopass);

        valuesnoUser.add("cn=doesntexis13451345," + usableTestContainer);
        dataNoUser.put("dn", valuesnoUser);

        ldapConnectorTest.opPasswordExists(dataUserWithPassword);
        try {
            ldapConnectorTest.opPasswordExists(dataUserNoPassword);
            fail("Test should have failed, user: [" + dataUserNoPassword + "] has no password.");
        } catch (IdMUnitFailureException e) {
            assertEquals(e.getMessage(), "Password did not exist for user: [cn=userNoPass," + usableTestContainer + "]");
        }
        try {
            ldapConnectorTest.opPasswordExists(dataNoUser);
        } catch (IdMUnitException e) {
            assertEquals("Could not execute password test", e.getMessage());
            String cause = "javax.naming.NameNotFoundException: [LDAP: error code 32 - NDS error: no such entry (-601)]; remaining name 'cn=doesntexis13451345," + usableTestContainer + "'";
            assertEquals(cause, e.getCause().toString());
        }

    }

    public void testPasswordDoesNotExist() throws EdirLdapConnectorTestException, IdMUnitException {
        HashMap<String, Collection<String>> dataUserNoPassword = new HashMap<String, Collection<String>>();
        ArrayList<String> valuesnopass = new ArrayList<String>();

        HashMap<String, Collection<String>> dataUserWithPassword = new HashMap<String, Collection<String>>();
        ArrayList<String> valueswithpass = new ArrayList<String>();

        HashMap<String, Collection<String>> dataNoUser = new HashMap<String, Collection<String>>();
        ArrayList<String> valuesnoUser = new ArrayList<String>();

        valueswithpass.add(createSimpleUserWithPassword());
        dataUserWithPassword.put("dn", valueswithpass);

        valuesnopass.add(createSimpleUserNoPassword());
        dataUserNoPassword.put("dn", valuesnopass);

        valuesnoUser.add("cn=doesntexis13451345," + usableTestContainer);
        dataNoUser.put("dn", valuesnoUser);


        ldapConnectorTest.opPasswordDoesNotExist(dataUserNoPassword);
        try {
            ldapConnectorTest.opPasswordDoesNotExist(dataUserWithPassword);
            fail("Test should have failed, user: [" + dataUserWithPassword + "] has no password.");
        } catch (IdMUnitFailureException e) {
            assertEquals(e.getMessage(), "Password exists for user: [cn=userWithPass," + usableTestContainer + "]");
        }
        try {
            ldapConnectorTest.opPasswordDoesNotExist(dataNoUser);
        } catch (IdMUnitException e) {
            assertEquals("Could not execute password test", e.getMessage());
            String cause = "javax.naming.NameNotFoundException: [LDAP: error code 32 - NDS error: no such entry (-601)]; remaining name 'cn=doesntexis13451345," + usableTestContainer + "'";
            assertEquals(cause, e.getCause().toString());
        }

    }


}

