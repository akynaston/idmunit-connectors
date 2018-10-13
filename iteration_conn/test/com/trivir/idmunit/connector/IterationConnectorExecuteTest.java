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

import java.util.*;

/**
 * @author Kenneth Rawlings
 */
@SuppressWarnings("serial")
public class IterationConnectorExecuteTest extends TestCase {
    public static final String OPERATION = "operation";

    private IterationConnector classUnderTest;

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(IterationConnectorExecuteTest.class));
        return suite;
    }


    protected void setUp() throws Exception {
        final Map<String, String> config = new HashMap<String, String>() {{
                put(IterationConnector.CONFIG_WRAPPED_CONNECTOR, "com.trivir.idmunit.connector.IterationConnectorExecuteTest$ExecuteTestConnector");
            }};

        classUnderTest = new IterationConnector();

        classUnderTest.setup(config);
    }

    public void tearDown() {
        classUnderTest = null;
    }

    public void testConnectorExecuteIterateRows() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(IterationConnector.ROW_ITERATION_START, Arrays.asList("1"));
                put(IterationConnector.ROW_ITERATION_END, Arrays.asList("3"));
                put("testKey", Arrays.asList("testValue$rowIteration$"));
            }};

        classUnderTest.execute(OPERATION, data);

        List<Map<String, Collection<String>>> expectedDataList = new ArrayList<Map<String, Collection<String>>>() {{
                add(new HashMap<String, Collection<String>>() {{
                        put("testKey", Arrays.asList("testValue1"));
                    }});
                add(new HashMap<String, Collection<String>>() {{
                        put("testKey", Arrays.asList("testValue2"));
                    }});
                add(new HashMap<String, Collection<String>>() {{
                        put("testKey", Arrays.asList("testValue3"));
                    }});
            }};

        assertEquals("Row iteration results do not match.", expectedDataList, ((ExecuteTestConnector)classUnderTest.wrappedConnector).dataList);
        assertEquals("Unexpected operation value.", OPERATION, ((ExecuteTestConnector)classUnderTest.wrappedConnector).op);
    }

    public void testConnectorExecuteIterateRowsNoStartOrEnd() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put("testKey", Arrays.asList("testValue$rowIteration$"));
            }};

        classUnderTest.execute(OPERATION, data);

        List<Map<String, Collection<String>>> expectedDataList = new ArrayList<Map<String, Collection<String>>>() {{
                add(new HashMap<String, Collection<String>>() {{
                        put("testKey", Arrays.asList("testValue1"));
                    }});
            }};

        assertEquals("Row iteration results do not match.", expectedDataList, ((ExecuteTestConnector)classUnderTest.wrappedConnector).dataList);
        assertEquals("Unexpected operation value.", OPERATION, ((ExecuteTestConnector)classUnderTest.wrappedConnector).op);
    }

    public void testConnectorExecuteIterateRowsReversedStartAndEnd() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(IterationConnector.ROW_ITERATION_START, Arrays.asList("3"));
                put(IterationConnector.ROW_ITERATION_END, Arrays.asList("1"));
                put("testKey", Arrays.asList("testValue$rowIteration$"));
            }};

        try {
            classUnderTest.execute(OPERATION, data);
            fail();
        } catch (IdMUnitException e) {
            // expected
        }
    }

    public void testConnectorExecuteIterateAttributes() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(IterationConnector.ATTRIBUTE_ITERATION_START, Arrays.asList("1"));
                put(IterationConnector.ATTRIBUTE_ITERATION_END, Arrays.asList("3"));
                put("testKey", Arrays.asList("testValue$attributeIteration$"));
            }};

        classUnderTest.execute(OPERATION, data);

        List<Map<String, Collection<String>>> expectedDataList = new ArrayList<Map<String, Collection<String>>>() {{
                add(new HashMap<String, Collection<String>>() {{
                        put("testKey", Arrays.asList("testValue1", "testValue2", "testValue3"));
                    }});
                }};

        assertEquals("Attribute iteration results do not match.", expectedDataList, ((ExecuteTestConnector)classUnderTest.wrappedConnector).dataList);
        assertEquals("Unexpected operation value.", OPERATION, ((ExecuteTestConnector)classUnderTest.wrappedConnector).op);
    }

    public void testConnectorExecuteIterateAttributesNoStartOrEnd() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put("testKey", Arrays.asList("testValue$attributeIteration$"));
            }};

        classUnderTest.execute(OPERATION, data);

        List<Map<String, Collection<String>>> expectedDataList = new ArrayList<Map<String, Collection<String>>>() {{
                add(new HashMap<String, Collection<String>>() {{
                        put("testKey", Arrays.asList("testValue1"));
                    }});
                }};

        assertEquals("Row iteration results do not match.", expectedDataList, ((ExecuteTestConnector)classUnderTest.wrappedConnector).dataList);
        assertEquals("Unexpected operation value.", OPERATION, ((ExecuteTestConnector)classUnderTest.wrappedConnector).op);
    }

    public void testConnectorExecuteIterateAttributesReversedStartAndEnd() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(IterationConnector.ATTRIBUTE_ITERATION_START, Arrays.asList("3"));
                put(IterationConnector.ATTRIBUTE_ITERATION_END, Arrays.asList("1"));
                put("testKey", Arrays.asList("testValue$attributeIteration$"));
            }};

        try {
            classUnderTest.execute(OPERATION, data);
            fail();
        } catch (IdMUnitException e) {
            // expected
        }
    }

    public static class ExecuteTestConnector extends AbstractConnector {

        String op;
        Map<String, String> config;
        List<Map<String, Collection<String>>> dataList = new ArrayList<Map<String, Collection<String>>>();

        public void setup(Map<String, String> configMap) throws IdMUnitException {
            this.config = configMap;
        }

        @Override
        public void execute(String operation, Map<String, Collection<String>> data) throws IdMUnitException {
            if (this.op == null) {
                this.op = operation;
            }

            if (operation.equals(this.op) == false) {
                throw new IdMUnitException("Operation must be the same for duration of test."); //This is a test constraint only
            }
            dataList.add(data);
        }
    }
}
