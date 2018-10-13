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

import java.util.HashMap;
import java.util.Map;

public class ConfigurationTests extends TestCase {
    private static final String HOST = "10.10.30.249";
    private static final String USER = "trivir";
    private static final String PASSWORD = "Trivir#1";
    private static final String READ_PATH = "/tmp";
    private static final String WRITE_PATH = "/tmp";

    public void testDefaultPort() throws Exception {
        DTF2Connector dtfConn = new DTF2Connector();

        Map<String, String> configParams = new HashMap<String, String>();
        configParams.put(DTF2Connector.READ_PATH, READ_PATH);
        configParams.put(DTF2Connector.WRITE_PATH, WRITE_PATH);
        configParams.put(DTF2Connector.ROW_KEY, "UserId"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".csv");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, "UserId, Name, FirstName, LastName, Group, Role");
        configParams.put(DTF2Connector.SSH_HOST, HOST);
        configParams.put(DTF2Connector.SSH_USER, USER);
        configParams.put(DTF2Connector.SSH_PASSWORD, PASSWORD);

        dtfConn.setup(configParams);
    }
}
