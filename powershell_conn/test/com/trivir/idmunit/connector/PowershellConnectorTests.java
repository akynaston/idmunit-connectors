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
import org.apache.log4j.BasicConfigurator;
import org.idmunit.IdMUnitException;

import java.util.*;

public class PowershellConnectorTests extends TestCase {
    private PowershellConnector conn = new PowershellConnector();

    private static void addSingleValue(Map<String, Collection<String>> data, String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(name, values);
    }

    @Override
    protected void setUp() throws Exception {
        BasicConfigurator.configure();
        Map<String, String> config = new HashMap<String, String>();
        config.put("path", "C:\\WINDOWS\\system32\\windowspowershell\\v1.0");
        config.put("ps-console-file", "psconsolefile.psc1");
        conn.setup(config);
    }

    @Override
    protected void tearDown() throws Exception {
        conn.tearDown();
    }

    public void testExec() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "exec", "echo \"test\"");

        conn.execute("exec", data);
    }

    public void testExecSyntaxError() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "exec", "asdff -");

        try {
            conn.execute("exec", data);
            fail("Expected an IdMUnitException to be thrown");
        } catch (IdMUnitException e) {
            // success
        }
    }

    public void testValidateString() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "exec", "echo \"test\"");
        addSingleValue(data, "output", "tes.?");

        conn.execute("validate", data);
    }

    public void testValidateObject() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "exec", "Get-Host");
        addSingleValue(data, "Name", "ConsoleHost");

        conn.execute("validate", data);
    }
}
