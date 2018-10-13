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
import nu.xom.Document;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectionConfigData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class DxcmdConnectorTests extends TestCase {
    private static final String CONNECTOR_DN = "Goldlnk Driver.Driver Set.servers.sa.system";
    private static final String CONNECTOR_DN2 = "Active Directory Driver.Driver Set.servers.sa.system";
    private static final String TEST_DN1 = "TestDN1";
    private static final String TEST_DN2 = "TestDN2";
    private static final String TEST_FILE_NO_CACHE = "NoCache";
    private static final String TEST_FILE_INITIAL_TIMESTAMP = "20002005";
    private static final String TEST_FILE_UNFINISHED_TIMESTAMP = "20032007";
    private static final String TEST_FILE_FINISHED_TIMESTAMP = "20062012";
    private DxcmdConnector conn;

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    public static String addCopyToString(String string) {
        return string + "_copy";
    }

    public static String copyFile(String sourceFilePath) throws IdMUnitException, IOException {
        Document doc = DxcmdConnector.loadXMLFromFS(DxcmdConnector.getXmlFsName(sourceFilePath));
        BufferedWriter output = new BufferedWriter(new FileWriter(DxcmdConnector.getXmlFsName(addCopyToString(sourceFilePath))));
        output.write(doc.toXML());
        output.close();
        return DxcmdConnector.getXmlFsName(addCopyToString(sourceFilePath));
    }

    protected void setUp() throws IdMUnitException {
        conn = new DxcmdConnector();
        ConnectionConfigData ccd = new ConnectionConfigData("dxcmd", "org.idmunit.connector.DXCMD");
        ccd.setParam(BasicConnector.CONFIG_USER, "admin.sa.system");
        ccd.setParam(BasicConnector.CONFIG_PASSWORD, "trivir"); // trivir
        ccd.setParam(BasicConnector.CONFIG_SERVER, "172.17.2.105");
        conn.setup(ccd.getParams());
    }

    protected void tearDown() throws IdMUnitException {
        conn.tearDown();
        conn = null;
    }

    public void testMigrateUserMigrateappNoDriverDN() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        try {
            conn.opMigrateApp(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("The driver dot format of the driver to process the command must be specified 'driverdn' or 'dn' must be specified", e.getMessage());
        }
    }

    public void testMigrateUserMigrateappNoXmlFile() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("driverdn", singleValue("RSADriver.EAPDrivers.resources"));
        try {
            conn.opMigrateApp(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("Either 'xmlFile' or 'xmlFileData' must be specified.", e.getMessage());
        }
    }

    public void testMigrateUserMigrateappXmlFileDoesNotExist() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("driverdn", singleValue("RSADriver.EAPDrivers.resources"));
        data.put("xmlFile", singleValue("C:/doesnotexist.gone"));

        try {
            conn.opMigrateApp(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'xmlFile' specifed: 'C:/doesnotexist.gone' does not exist!", e.getMessage());
        }
    }

    public void testMigrateUserMigrateappDataAndFileSupplied() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("driverdn", singleValue("RSADriver.EAPDrivers.resources"));
        data.put("xmlFile", singleValue("C:/doesnotexist.gone"));
        data.put("xmlFileData", singleValue("and somedata . ."));

        try {
            conn.opMigrateApp(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("Specify either 'xmlFile' or 'xmlFileData', do not use both.", e.getMessage());
        }
    }

    public void testStartJobNoDN() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        try {
            conn.opStartJob(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of job not specified.", e.getMessage());
        }
    }

    public void testCheckDriverProcessing() throws IdMUnitException, ParseException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        List<String> dns = new ArrayList<String>();
        dns.add(CONNECTOR_DN);
        dns.add(CONNECTOR_DN2);

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
}
