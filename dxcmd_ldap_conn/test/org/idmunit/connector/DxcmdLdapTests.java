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
import org.idmunit.util.LdapConnectionHelper;

import java.util.*;

public class DxcmdLdapTests extends TestCase {
    private DxcmdLdap conn;

    @SuppressWarnings("SameParameterValue")
    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        return values;
    }

    protected void setUp() throws IdMUnitException {
        conn = new DxcmdLdap();
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

    public void testOpAddObject_noOptionSpecified() {
        try {
            Map<String, Collection<String>> data = new HashMap<>();
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("'option' not specified", e.getMessage());
        }
    }

    public void testOpAddObject_unsupportedOption() {
        final String TEST_UNSUPPORTED_OPTION_VALUE = "lorem ipsum";
        Map<String, Collection<String>> data = new HashMap<>();
        data.put("option", singleValue(TEST_UNSUPPORTED_OPTION_VALUE));
        try {
            conn.opAddObject(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals(String.format("Unsupported option '%s'", TEST_UNSUPPORTED_OPTION_VALUE), e.getMessage());
        }
    }

    // TODO: implement test for uppercase and lowercase options
    // TODO: implement successful tests
}
