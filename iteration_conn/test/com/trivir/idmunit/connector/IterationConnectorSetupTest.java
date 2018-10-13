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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kenneth Rawlings
 */
@SuppressWarnings("serial")
public class IterationConnectorSetupTest extends TestCase {
    private IterationConnector classUnderTest;

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(IterationConnectorSetupTest.class));
        return suite;
    }


    protected void setUp() throws Exception {
        classUnderTest = new IterationConnector();
    }

    public void tearDown() {
        classUnderTest = null;
    }

    public void testConnectorSetupWrappedConnector() throws IdMUnitException {
        final Map<String, String> config = new HashMap<String, String>() {{
                put(BasicConnector.CONFIG_SERVER, "SERVER");
                put(BasicConnector.CONFIG_USER, "USER");
                put(BasicConnector.CONFIG_PASSWORD, "PASSWORD");
                put(IterationConnector.CONFIG_WRAPPED_CONNECTOR, "com.trivir.idmunit.connector.IterationConnectorSetupTest$SetupTestConnector");
            }};


        classUnderTest.setup(config);

        // this is stripped before it is passed to the wrapped connector
        config.remove(IterationConnector.CONFIG_WRAPPED_CONNECTOR);

        assertEquals("Configuration in wrapped connector was not expected.", config, ((SetupTestConnector)classUnderTest.wrappedConnector).config);
    }

    public void testConnectorSetupInvalidWrappedConnector() throws IdMUnitException {
        final Map<String, String> config = new HashMap<String, String>() {{
                put(BasicConnector.CONFIG_SERVER, "SERVER");
                put(BasicConnector.CONFIG_USER, "USER");
                put(BasicConnector.CONFIG_PASSWORD, "PASSWORD");
                put(IterationConnector.CONFIG_WRAPPED_CONNECTOR, "BADCONNECTOR");
            }};


        try {
            classUnderTest.setup(config);
            fail("Did not receive expected exception");
        } catch (IdMUnitException e) {
            // expected exception
        }
    }

    public void testConnectorSetupMissingWrappedConnector() throws IdMUnitException {
        final Map<String, String> config = new HashMap<String, String>() {{
                put(BasicConnector.CONFIG_SERVER, "SERVER");
                put(BasicConnector.CONFIG_USER, "USER");
                put(BasicConnector.CONFIG_PASSWORD, "PASSWORD");
            }};


        try {
            classUnderTest.setup(config);
            fail("Did not receive expected exception");
        } catch (IdMUnitException e) {
            // expected exception
        }
    }

    public static class SetupTestConnector extends AbstractConnector {

        Map<String, String> config;

        public void setup(Map<String, String> configMap) throws IdMUnitException {
            this.config = configMap;
        }
    }
}
