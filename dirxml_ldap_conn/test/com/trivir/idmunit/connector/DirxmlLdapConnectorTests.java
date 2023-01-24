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
}
