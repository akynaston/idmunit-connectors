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
import org.idmunit.connector.BasicConnector;

import java.util.*;

/**
 * @author Kenneth Rawlings
 */
@SuppressWarnings("serial")
public class UserAppRESTTest extends TestCase {
    private static final String RIS_URL = "https://172.17.2.42/RIS";
    private static final String USER_APP_ADMIN = "cn=uaadmin,ou=sa,o=data";
    private static final String USER_APP_ADMIN_PASSWORD = "Password1";
    private static final String DEFAULT_RECIPIENT_DN = "cn=uaadmin,ou=sa,o=data";
    private static final String DEFAULT_WORKFLOW_DN = "cn=RestConnTest,cn=RequestDefs,cn=AppConfig,cn=UserApplication,cn=driverset1,o=system";

    private UserAppREST classUnderTest;
    private Map<String, String> config;

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(UserAppRESTTest.class));
        return suite;
    }


    protected void setUp() throws Exception {
        config = new HashMap<String, String>() {{
                put(BasicConnector.CONFIG_SERVER, RIS_URL);
                put(BasicConnector.CONFIG_USER, USER_APP_ADMIN);
                put(BasicConnector.CONFIG_PASSWORD, USER_APP_ADMIN_PASSWORD);
            }};

        classUnderTest = new UserAppREST();
        classUnderTest.setup(config);
    }

    public void tearDown() {
    }

    public void testOpTestConnection() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();

        classUnderTest.opTestConnection(data);
    }

    public void testOpTestConnectionFailure() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();

        config.put(BasicConnector.CONFIG_SERVER, "http://127.0.0.1:8080/RISBOGUS");
        classUnderTest.setup(config);

        try {
            classUnderTest.opTestConnection(data);
            fail("Expected exception not thrown.");
        } catch (IdMUnitException e) {
            // Expected exception
        }
    }

    public void testOpStartWorkflowWithoutRequiredArgs() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();

        try {
            classUnderTest.opStartWorkflow(data);
            fail("Expected exception not thrown");
        } catch (IdMUnitException e) {
            // expected exception
        }
    }

    public void testOpStartWorkflowDnNotFound() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(UUID.randomUUID().toString());
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add("cn=bogusdn,o=boguso");
                    }});
                put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
            }};

        try {
            classUnderTest.opStartWorkflow(data);
            fail("Expected exception not thrown");
        } catch (IdMUnitException e) {
            // expected exception
        }
    }

    /* The current connector has this turned off.
    public void testOpStartWorkflowMissingValues() {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>(){{
            put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>(){{add(UUID.randomUUID().toString());}});
            put(UserAppREST.WORKFLOW_DN, new ArrayList<String>(){{add(DEFAULT_WORKFLOW_DN);}});
            put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>(){{add(DEFAULT_RECIPIENT_DN);}});
            put("recipient", new ArrayList<String>(){{add(DEFAULT_RECIPIENT_DN);}});
        }};

        try {
            classUnderTest.opStartWorkflow(data);
            fail("Expected exception not thrown");
        } catch (IdMUnitException e) {
            // expected exception
        }
    }*/

    public void testOpStartWorkflowTooManyValues() {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(UUID.randomUUID().toString());
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add(DEFAULT_WORKFLOW_DN);
                    }});
                put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put("reason", new ArrayList<String>() {{
                        add("Because it's fun");
                    }});
                put("recipient", new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put("bogusValue", new ArrayList<String>() {{
                        add("bogiosity");
                    }});
            }};

        try {
            classUnderTest.opStartWorkflow(data);
            fail("Expected exception not thrown");
        } catch (IdMUnitException e) {
            // expected exception
        }
    }

    public void testOpStartWorkflow() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(UUID.randomUUID().toString());
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add(DEFAULT_WORKFLOW_DN);
                    }});
                put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put("reason", new ArrayList<String>() {{
                        add("Because it's fun");
                    }});
                put("recipient", new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
            }};

        classUnderTest.opStartWorkflow(data);
    }

    public void testApproveWorkflowUnknownWorkflowId() throws IdMUnitException {
        Map<String, Collection<String>> workflowData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(UUID.randomUUID().toString());
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add(DEFAULT_WORKFLOW_DN);
                    }});
                put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put("reason", new ArrayList<String>() {{
                        add("Because it's fun");
                    }});
                put("recipient", new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
            }};
        Map<String, Collection<String>> approvalData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(UUID.randomUUID().toString());
                    }});
            }};

        classUnderTest.opStartWorkflow(workflowData);
        try {
            classUnderTest.opApproveWorkflow(approvalData);
            fail("Expected exception not thrown");
        } catch (IdMUnitException e) {
            // expected exception
        }
    }

    public void testApproveWorkflow() throws IdMUnitException, InterruptedException {
        final String workflowId = UUID.randomUUID().toString();
        Map<String, Collection<String>> workflowData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(workflowId);
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add(DEFAULT_WORKFLOW_DN);
                    }});
                put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put("reason", new ArrayList<String>() {{
                        add("Because it's fun");
                    }});
                put("recipient", new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
            }};
        Map<String, Collection<String>> approvalData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(workflowId);
                    }});
                put("approvalDataTest", new ArrayList<String>() {{
                        add("Approval Data Test");
                    }});
            }};
        Map<String, Collection<String>> statusData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(workflowId);
                    }});
                put(UserAppREST.WORKFLOW_APPROVAL_STATUS, new ArrayList<String>() {{
                        add("Approved");
                    }});
                put(UserAppREST.WORKFLOW_PROCESS_STATUS, new ArrayList<String>() {{
                        add("Completed");
                    }});
            }};

        classUnderTest.opStartWorkflow(workflowData);
        Thread.sleep(10000);
        classUnderTest.opApproveWorkflow(approvalData);
        Thread.sleep(10000);
        classUnderTest.opCheckWorkflowStatus(statusData);
    }

    public void testRejectWorkflow() throws IdMUnitException, InterruptedException {
        final String workflowId = UUID.randomUUID().toString();
        Map<String, Collection<String>> workflowData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(workflowId);
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add(DEFAULT_WORKFLOW_DN);
                    }});
                put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put("reason", new ArrayList<String>() {{
                        add("Because it's fun");
                    }});
                put("recipient", new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
            }};
        Map<String, Collection<String>> approvalData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(workflowId);
                    }});
            }};
        Map<String, Collection<String>> statusData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(workflowId);
                    }});
                put(UserAppREST.WORKFLOW_APPROVAL_STATUS, new ArrayList<String>() {{
                        add("Denied");
                    }});
                put(UserAppREST.WORKFLOW_PROCESS_STATUS, new ArrayList<String>() {{
                        add("Completed");
                    }});
            }};

        classUnderTest.opStartWorkflow(workflowData);
        Thread.sleep(10000);
        classUnderTest.opDenyWorkflow(approvalData);
        Thread.sleep(10000);
        classUnderTest.opCheckWorkflowStatus(statusData);
    }

    public void testCaptureWorkflow() throws IdMUnitException, InterruptedException {
        final String workflowId = UUID.randomUUID().toString();
        final String captureWorkflowId = UUID.randomUUID().toString();
        Map<String, Collection<String>> workflowData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(workflowId);
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add(DEFAULT_WORKFLOW_DN);
                    }});
                put(UserAppREST.WORKFLOW_RECIPIENT, new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
                put("reason", new ArrayList<String>() {{
                        add("Because it's fun");
                    }});
                put("recipient", new ArrayList<String>() {{
                        add(DEFAULT_RECIPIENT_DN);
                    }});
            }};
        Map<String, Collection<String>> approvalData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(captureWorkflowId);
                    }});
            }};
        Map<String, Collection<String>> statusData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(captureWorkflowId);
                    }});
                put(UserAppREST.WORKFLOW_APPROVAL_STATUS, new ArrayList<String>() {{
                        add("Approved");
                    }});
                put(UserAppREST.WORKFLOW_PROCESS_STATUS, new ArrayList<String>() {{
                        add("Completed");
                    }});
            }};
        Map<String, Collection<String>> preCaptureData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(captureWorkflowId);
                    }});
                put(UserAppREST.WORKFLOW_DN, new ArrayList<String>() {{
                        add(DEFAULT_WORKFLOW_DN);
                    }});
            }};
        Map<String, Collection<String>> captureData = new HashMap<String, Collection<String>>() {{
                put(UserAppREST.WORKFLOW_IDENTIFIER, new ArrayList<String>() {{
                        add(captureWorkflowId);
                    }});
            }};

        classUnderTest.opPreCaptureWorkflow(preCaptureData);
        classUnderTest.opStartWorkflow(workflowData);
        Thread.sleep(10000);
        classUnderTest.opCaptureWorkflow(captureData);
        classUnderTest.opApproveWorkflow(approvalData);
        Thread.sleep(10000);
        classUnderTest.opCheckWorkflowStatus(statusData);
    }
}
