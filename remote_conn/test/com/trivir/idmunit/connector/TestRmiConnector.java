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

import java.util.*;

public class TestRmiConnector extends TestCase {

    private static void addSingleValue(Map<String, Collection<String>> data, String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(name, values);
    }

    public void testSetup() throws IdMUnitException {
        RmiConnector conn = new RmiConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("rmi-server", "localhost");
        config.put("remote-type", "com.trivir.idmunit.connector.DummyConnector");
        config.put("server", "testserver");
        conn.setup(config);
        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "objectClass", "User");
        addSingleValue(data, "DefaultLogin", "tuserAdd");
        addSingleValue(data, "LastName", "lastname");
        addSingleValue(data, "FirstName", "firstname");
        addSingleValue(data, "DefaultShell", "noShell");

        conn.execute("validateObject", data);
        conn.tearDown();
    }
}
