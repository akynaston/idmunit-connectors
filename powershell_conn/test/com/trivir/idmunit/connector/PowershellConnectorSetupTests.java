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

public class PowershellConnectorSetupTests extends TestCase {
    private static final String POWERSHELL_PATH = "C:\\WINDOWS\\system32\\windowspowershell\\v1.0";

    private static void runCommand(PowershellConnector conn) throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "exec", "echo \"test\"");

        conn.execute("exec", data);
    }

    private static void addSingleValue(Map<String, Collection<String>> data, String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(name, values);
    }

    protected void setUp() throws Exception {
        BasicConfigurator.configure();
    }

    public void testSetup() throws Exception {
        PowershellConnector conn = new PowershellConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("path", POWERSHELL_PATH);
        conn.setup(config);

        runCommand(conn);
    }

    public void testPSConsoleFile() throws Exception {
        PowershellConnector conn = new PowershellConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("path", POWERSHELL_PATH);
        config.put("ps-console-file", "psconsolefile.psc1");
        conn.setup(config);

        runCommand(conn);
    }

    public void testPSConsoleFileDoesNotExist() throws Exception {
        PowershellConnector conn = new PowershellConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("path", POWERSHELL_PATH);
        config.put("ps-console-file", "foo.psc1");
        conn.setup(config);

        runCommand(conn);
    }
}
