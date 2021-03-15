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
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectionConfigData;
import org.idmunit.util.LdapConnectionHelper;

import java.util.*;

public class DirxmlLdapConnectorTests extends TestCase {

    private DirxmlLdapConnector conn;

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        return values;
    }

    protected void setUp() throws IdMUnitException {
        conn = new DirxmlLdapConnector();
        ConnectionConfigData ccd = new ConnectionConfigData("dxcmd", "org.idmunit.connector.DXCMD");
        ccd.setParam(BasicConnector.CONFIG_USER, "cn=admin,o=services");
        ccd.setParam(BasicConnector.CONFIG_PASSWORD, "trivir");
        ccd.setParam(BasicConnector.CONFIG_SERVER, "10.10.30.249");
        ccd.setParam(LdapConnectionHelper.CONFIG_TRUST_ALL_CERTS, "true");
        ccd.setParam(LdapConnectionHelper.CONFIG_USE_TLS, "true");
        conn.setup(ccd.getParams());
    }

    protected void tearDown() {
        conn.tearDown();
        conn = null;
    }

    public void testOpMigrateApp_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opMigrateApp(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver to start not specified.", e.getMessage());
        }
    }

    public void testOpMigrateApp_xmlFileOptionSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        data.put("dn", singleValue("cn=Meaningless DN"));
        data.put("xmlfile", singleValue("This is a meaningless value"));
        try {
            conn.opMigrateApp(data);
            fail("Should have thrown an exception");
        } catch (IdMUnitException e) {
            assertEquals("'xmlfile' option is no longer supported. use 'xmlfiledata'", e.getMessage());
        }
    }

    public void testOpMigrateApp_noXmlFileDataSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        data.put("dn", singleValue("cn=Meaningless DN"));
        try {
            conn.opMigrateApp(data);
            fail("Should have thrown an exception");
        } catch (IdMUnitException e) {
            assertEquals("'xmlfiledata' must be specified.", e.getMessage());
        }
    }

    // TODO: Tests for successful opMigrateApp call

    public void testOpStartJob_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opStartJob(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of job not specified.", e.getMessage());
        }
    }

    // TODO: Tests for successful opStartJob call

    public void testOpValidateDriverState_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opValidateDriverState(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver not specified.", e.getMessage());
        }
    }

    public void testOpValidateDriverState_noExpectedStatusSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        data.put("dn", singleValue("cn=Meaningless DN"));
        try {
            conn.opValidateDriverState(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("No expected status given", e.getMessage());
        }
    }

    // TODO: Tests for successful opValidateDriverState call

    public void testOpStartDriver_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opStartDriver(data);
            fail("Should have thrown an exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver to start not specified.", e.getMessage());
        }
    }

    // TODO: Tests for successful opStartDriver call

    public void testOpStopDriver_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opStopDriver(data);
            fail("Should have thrown an exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver to stop not specified.", e.getMessage());
        }
    }

    // TODO: Tests for successful opStopDriver call

    public void testOpSetDriverStatusManual_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opSetDriverStatusManual(data);
            fail("Should have thrown an exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver not specified.", e.getMessage());
        }
    }

    // TODO: Tests for successful opSetDriverStatusManual call

    public void testOpSetDriverStatusAuto_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opSetDriverStatusAuto(data);
            fail("Should have thrown an exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver not specified.", e.getMessage());
        }
    }

    // TODO: Tests for successful opSetDriverStatusAuto call

    public void testOpSetDriverStatusDisabled_noDnSpecified() {
        Map<String, Collection<String>> data = new HashMap<>();
        try {
            conn.opSetDriverStatusDisabled(data);
            fail("Should have thrown an exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver not specified.", e.getMessage());
        }
    }

    // TODO: Tests for successful opSetDriverStatusDisabled call

    /*
     * TODO: Implement the following tests or mark as private:
     *   - parseStatusInt
     *   - parseStatusString
     *   - writeXmlData
     *   - getDriverState
     *   - isDriverRunning
     *   - isDriverStopped
     *   - getDriverStartOption
     *   - getJobState
     */


    /*
     * ----------------------------------------------------------------------------------------------------------------
     *
     * These modifications were added in SVN revision 13126 by krawlings with the note:
     *   "Checking in dxcmd patch from Andrew and Carl with some cleanup."
     *
     * Three hours later in SVN revision 13127 krawlings noted:
     *   "- Reordered some statements in the connector.
     *    - Existing behavior remains unmodified, but new behavior can not be tested due to missing XML files."
     *


    private static final String CONNECTOR_DN = "Goldlnk Driver.Driver Set.servers.sa.system";
    private static final String CONNECTOR_DN2 = "Active Directory Driver.Driver Set.servers.sa.system";
    private static final String TEST_DN1 = "TestDN1";
    private static final String TEST_DN2 = "TestDN2";
    private static final String TEST_FILE_NO_CACHE = "NoCache";
    private static final String TEST_FILE_INITIAL_TIMESTAMP = "20002005";
    private static final String TEST_FILE_INITIAL_TIMESTAMP = ".//Documents/20002005";
    private static final String TEST_FILE_UNFINISHED_TIMESTAMP = "20032007";
    private static final String TEST_FILE_FINISHED_TIMESTAMP = "20062012";

    public static String addCopyToString(String string) {
        return string + "_copy";
    }

    public static String copyFile(String sourceFilePath) throws IdMUnitException, IOException {
        Document doc = DxcmdLdapConnector.loadXMLFromFS(DxcmdLdapConnector.getXmlFsName(sourceFilePath));
        BufferedWriter output = new BufferedWriter(new FileWriter(DxcmdLdapConnector.getXmlFsName(addCopyToString(sourceFilePath))));
        output.write(doc.toXML());
        output.close();
        return DxcmdLdapConnector.getXmlFsName(addCopyToString(sourceFilePath));
    }

    public void testCheckDriverProcessing() throws IdMUnitException, ParseException, NamingException, LDAPException {
        Map<String, Collection<String>> data = new HashMap<>();
        List<String> dns = new ArrayList<>();
//        dns.add(CONNECTOR_DN);
        dns.add(CONNECTOR_DN);

        data.put("dn", dns);

        try {
            conn.opCheckDriverProcessing(data);
        } catch (IdMUnitFailureException e) {
            fail("Unexpected failure: " + e.getMessage());
        }

        for (String currentDN : dns) {
            System.out.println("EPD: " + conn.eventProcessingDates.get(currentDN));
            assertNotNull(conn.eventProcessingDates.get(currentDN));
        }
    }

    public void testCheckDriverCache_EmptyEmpty() throws IdMUnitException, ParseException, IOException {
        //Expect success
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
    }

    public void testCheckDriverCache_EmptySomethingEmpty() throws IdMUnitException, ParseException, IOException {
        //Expect success
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
    }

    public void testCheckDriverCache_EmptySomethingDone() throws IdMUnitException, ParseException, IOException {
        //Expect success
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
            fail("Expected an exceptoin");
        } catch (IdMUnitFailureException e) {
            System.out.println(e.getMessage());
        }

        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
            fail("Expected an exceptoin");
        } catch (IdMUnitFailureException e) {
            System.out.println(e.getMessage());
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_FINISHED_TIMESTAMP));

    }

    public void testCheckDriverCache_EmptySomethingNotDone() throws IdMUnitException, ParseException, IOException {
        //Expect Failure
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_UNFINISHED_TIMESTAMP));
            fail("Should have thrown an exception");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }

    }

    public void testCheckDriverCache_SomethingEmpty() throws IdMUnitException, ParseException, IOException {
        //Expect success
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
    }

    public void testCheckDriverCache_SomethingSomethingDone() throws IdMUnitException, ParseException, IOException {
        //Expect success
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_FINISHED_TIMESTAMP));
    }

    public void testCheckDriverCache_SomethingSomethingNotDone() throws IdMUnitException, ParseException, IOException {
        //Expect Failure
        try {
            conn.validateCacheXml(CONNECTOR_DN, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        try {
            conn.validateCacheXml(CONNECTOR_DN, copyFile(TEST_FILE_UNFINISHED_TIMESTAMP));
            fail("Should have thrown an exception");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
    }

    public void testCheckDriverCache_MultipleDNsSuccessStaysTrue() throws IdMUnitException, ParseException, IOException {
        //Expect Success
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
            fail("Should have thrown an exception, cache empty on first pass");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        try {
            conn.validateCacheXml(TEST_DN2, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
            fail("Should have thrown an exception, first pass");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
        try {
            conn.validateCacheXml(TEST_DN2, copyFile(TEST_FILE_UNFINISHED_TIMESTAMP));
            fail("Should have thrown an exception, Event has not processed yet");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
        conn.validateCacheXml(TEST_DN2, copyFile(TEST_FILE_FINISHED_TIMESTAMP));
    }

    public void testCheckDriverCache_MultipleDNsOneFails() throws IdMUnitException, ParseException, IOException {
        //Expect Failure
        try {
            conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
            fail("Should have thrown an exception, cache empty on first pass");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        try {
            conn.validateCacheXml(TEST_DN2, copyFile(TEST_FILE_INITIAL_TIMESTAMP));
            fail("Should have thrown an exception, first pass");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
        try {
            conn.validateCacheXml(TEST_DN2, copyFile(TEST_FILE_UNFINISHED_TIMESTAMP));
            fail("Should have thrown an exception, Event has not processed yet");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
        conn.validateCacheXml(TEST_DN1, copyFile(TEST_FILE_NO_CACHE));
        try {
            conn.validateCacheXml(TEST_DN2, copyFile(TEST_FILE_UNFINISHED_TIMESTAMP));
            fail("Should have thrown an exception, Event has not processed yet");
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }
    }

     */
}
