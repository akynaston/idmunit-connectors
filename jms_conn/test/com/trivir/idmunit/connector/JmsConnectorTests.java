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
import org.apache.activemq.ActiveMQConnection;
import org.apache.log4j.BasicConfigurator;
import org.idmunit.IdMUnitException;

import java.util.*;

public class JmsConnectorTests extends TestCase {
    private JmsConnector conn = new JmsConnector();

    private static void addSingleValue(Map<String, Collection<String>> data, String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(name, values);
    }

    @Override
    protected void setUp() throws Exception {
        BasicConfigurator.configure();
        Map<String, String> config = new HashMap<String, String>();
        config.put("url", "10.10.30.128:8080");
        config.put("user", ActiveMQConnection.DEFAULT_USER);
        config.put("password", ActiveMQConnection.DEFAULT_PASSWORD);
        config.put("subject", "TEST.FOO");
        config.put("topic", "false");
        config.put("connection-factory", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        config.put("connection-factory-jndi-name", "ConnectionFactory");
        conn.setup(config);
    }

    @Override
    protected void tearDown() throws Exception {
        conn.tearDown();
    }

    public void testPublish() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "message", "This is a test message.");

        conn.execute("publish", data);
    }

    public void testValidate() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        addSingleValue(data, "message", "This is a test message.");

        conn.execute("validate", data);
    }
}
