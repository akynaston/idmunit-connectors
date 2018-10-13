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
import org.idmunit.IdMUnitFailureException;

import java.util.*;

public class CmdLineConnectorTests extends TestCase {
    private CmdLineConnector conn = null;

    private static void addSingleValue(Map<String, Collection<String>> data, String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(name, values);
    }

    protected void setUp() throws Exception {
        conn = new CmdLineConnector();
        Map<String, String> config = new TreeMap<String, String>();
        config.put("user", "testUserName");
        config.put("password", "testPassword");
        conn.setup(config);
    }

    protected void tearDown() throws Exception {
        conn = null;
    }

    public void testRequiredColumnsCheck() throws IdMUnitException {
        CmdLineConnector connTemp = new CmdLineConnector();
        Map<String, String> config = new TreeMap<String, String>();
        connTemp.setup(config);
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "command", "dir");
        addSingleValue(data, "response", ".*Volume Serial Number.*");
        conn.opRunCmd(data);
    }

    public void testOpValidateObjectSuccess() throws IdMUnitException, IdMUnitFailureException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "command", "dir");
        addSingleValue(data, "response", ".*Volume Serial Number.*");
        conn.opRunCmd(data);
    }

    public void testOpValidateObjectFailure() throws IdMUnitException, IdMUnitFailureException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "command", "dir");
        addSingleValue(data, "response", "InvalidResponse!");
        try {
            conn.opRunCmd(data);
            fail("An IdMUnitFailureException should have been thrown due to a failed test.");
        } catch (IdMUnitFailureException e) {
            //Success
        }
    }

    public void testOpValidateObjectInValidCommand() throws IdMUnitException, IdMUnitFailureException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "command", "InvalidCommand!");
        addSingleValue(data, "response", ".*Volume Serial Number.*");

        try {
            conn.opRunCmd(data);
            fail("An exception should have been thrown for the bad command entered.");
        } catch (IdMUnitException e) {
            //The connector threw the correct exception
        }
    }

    public void testOpValidateObjectInvalidRegex() throws IdMUnitException, IdMUnitFailureException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "command", "dir");
        addSingleValue(data, "response", ")");

        try {
            conn.opRunCmd(data);
            fail("An exception should have been thrown for the bad regex entered.");
        } catch (IdMUnitException e) {
            assertTrue(e.getCause().getClass().toString().equalsIgnoreCase("class java.util.regex.PatternSyntaxException"));

            //assertTrue((e.getCause().getClass().toString().equalsIgnoreCase("class java.util.regex.PatternSyntaxException")));
            //Success
        }
    }

    public void testOpValidateObjectBlankCommand() throws IdMUnitException, IdMUnitFailureException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "command", "");
        addSingleValue(data, "response", ".*Volume Serial Number.*");

        try {
            conn.opRunCmd(data);
            fail("An exception should have been thrown for a blank command.");
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("No value was entered for the \"" + conn.COMMAND + "\" column."));
            //Success
        }
    }

    public void testOpValidateObjectBlankRegex() throws IdMUnitException, IdMUnitFailureException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "command", "dir");
        addSingleValue(data, "response", "");

        try {
            conn.opRunCmd(data);
            fail("An exception should have been thrown for a blank response (regex).");
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("No value was entered for the \"" + conn.RESPONSE + "\" column."));
            //Success
        }
    }

}
