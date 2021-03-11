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

package org.idmunit.connector;

import junit.framework.TestCase;
import org.idmunit.IdMUnitException;

import java.util.*;

public class DxcmdLdapTests extends TestCase {
    private DxcmdLdap conn;

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    protected void setUp() throws IdMUnitException {
        conn = new DxcmdLdap();
        ConnectionConfigData ccd = new ConnectionConfigData("dxcmd", "org.idmunit.connector.DXCMD");
        ccd.setParam(BasicConnector.CONFIG_USER, "cn=admin,o=services");
        ccd.setParam(BasicConnector.CONFIG_PASSWORD, "trivir");
        ccd.setParam(BasicConnector.CONFIG_SERVER, "10.10.30.249");
        ccd.setParam("trusted-cert-file", "10.10.30.249.cer");
        conn.setup(ccd.getParams());
    }

    protected void tearDown() throws IdMUnitException {
        conn.tearDown();
        conn = null;
    }

    public void testMigrateUserNoOptionSpecified() throws IdMUnitException {
        try {
            Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'option' not specified", e.getMessage());
        }
    }

    public void testMigrateUserMigrateappNoDriverDN() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("option", singleValue("migrateapp"));
        try {
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of driver to start not specified.", e.getMessage());
        }
    }

    public void testMigrateUserMigrateappNoXmlFile() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("option", singleValue("migrateapp"));
        data.put("dn", singleValue("RSADriver.EAPDrivers.resources"));
        try {
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'xmlFileData' must be specified.", e.getMessage());
        }
    }

    @Deprecated
    public void testMigrateUserMigrateappXmlFileDoesNotExist() throws IdMUnitException { //TODO: Delete this test. This test is no longer needed because of xmlFile no longer being supported.
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("option", singleValue("migrateapp"));
        data.put("dn", singleValue("RSADriver.EAPDrivers.resources"));
        data.put("xmlFile", singleValue("C:/doesnotexist.gone"));

        try {
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'xmlFile' specifed: 'C:/doesnotexist.gone' does not exist!", e.getMessage());
        }
    }

    public void testMigrateUserMigrateappDataAndFileSupplied() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("option", singleValue("migrateapp"));
        data.put("dn", singleValue("RSADriver.EAPDrivers.resources"));
        data.put("xmlFile", singleValue("C:/doesnotexist.gone"));
        data.put("xmlFileData", singleValue("and somedata . ."));

        try {
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'xmlFile' is no longer supported. Use 'xmlFileData'", e.getMessage());
        }
    }

    public void testStartJobNoDN() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("option", singleValue("startjob"));
        try {
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'dn' of job not specified.", e.getMessage());
        }
    }
}
