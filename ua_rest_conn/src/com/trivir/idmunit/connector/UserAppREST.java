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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.idmunit.Failures;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kenneth Rawlings
 */
public class UserAppREST extends AbstractConnector {
    static final String WORKFLOW_IDENTIFIER = "workflowIdentifier";
    static final String WORKFLOW_DN = "workflowDn";
    static final String WORKFLOW_FILTER = "workflowFilter";
    static final String WORKFLOW_RECIPIENT = "workflowRecipient";
    static final String WORKFLOW_APPROVAL_STATUS = "workflowApprovalStatus";
    static final String WORKFLOW_PROCESS_STATUS = "workflowProcessStatus";
    private static final String REST_AUTH = "RESTAuthorization";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String PATH_DEFINITIONS = "v1/wf/definitions";
    private static final String PATH_WORKITEMS = "v1/wf/workitems";
    private static final String PATH_PROCESSES = "v1/wf/processes";
    private static final String JSON_FIELD_DN = "DN";
    private static final String JSON_FIELD_GUID = "GUID";
    private static final String JSON_FIELD_DATAITEMS = "DataItems";
    private static final String JSON_FIELD_VALUES = "Values";
    private static final String JSON_FIELD_VALUE = "Value";
    private static final String JSON_FIELD_NAME = "Name";
    private static final String JSON_FIELD_MULTIVALUED = "MultiValued";
    private static final String JSON_FIELD_RECIPIENT = "Recipient";
    private static final String JSON_FIELD_CODE = "Code";
    private static final String JSON_FIELD_AVAILABLE_ACTIONS = "AvailableActions";
    private static final String JSON_FIELD_APPROVAL_STATUS = "ApprovalStatus";
    private static final String JSON_FIELD_PROCESS_STATUS = "ProcessStatus";
    private static final Map<String, String> EMPTY_PARAMS = Collections.unmodifiableMap(new HashMap<String, String>());
    private static final String PARAM_FILTER = "filter";
    private static final String FILTER_DEFINITION = "Definition";
    private static final String FILTER_PROCESSID = "ProcessId";
    // NOTE: Even though we are using ConcurrentHashMap, there may be some "check then act" problems that would need to be resolved by an explicit lock.
    private static final Map<String, String> WORKFLOW_CACHE = new ConcurrentHashMap<String, String>();
    private static final Map<String, PreCapture> CAPTURE_CACHE = new ConcurrentHashMap<String, PreCapture>();
    private static Logger log = LoggerFactory.getLogger(UserAppREST.class);
    private String serverUrl;
    private String credentials;
    private WebResource rootResource;
    private Map<String, String> defaultHeaders;

    private static void fillDataItems(JSONArray dataItems, Map<String, Collection<String>> data, boolean strict) throws IdMUnitException {
        try {
            Set<String> workflowDataNames = new HashSet<String>();
            for (int itemNum = 0; itemNum < dataItems.length(); itemNum++) {
                JSONObject item = dataItems.getJSONObject(itemNum);
                String itemName = item.getString(JSON_FIELD_NAME);
                Collection<String> values = data.get(itemName);

                if (values == null) {
                    /*To allow the ability to leave some values NULL once they get in the workflow,
                    The line below is commented out ad the two lines after were added.*/
                    //throw new IdMUnitException(String.format("Values were not supplied for workflow data item '%s'.",item.getString(JSON_FIELD_NAME)));
                    log.debug(String.format("Values were not supplied for workflow data item '%s'.", item.getString(JSON_FIELD_NAME)));
                    continue;
                }

                if (!item.getBoolean(JSON_FIELD_MULTIVALUED) && values.size() > 1) {
                    throw new IdMUnitException(String.format("Multiple values were supplied for single valued workflow item '%s'", itemName));
                }

                JSONArray jsonValues = new JSONArray();
                item.put(JSON_FIELD_VALUES, jsonValues);

                for (final String value : values) {
                    jsonValues.put(new JSONObject() {{
                            put(JSON_FIELD_VALUE, value);
                        }});
                }

                workflowDataNames.add(itemName);
            }

            if (strict && !workflowDataNames.containsAll(data.keySet())) {
                throw new IdMUnitException("Values were supplied that are not present in the worflow data items");
            }
        } catch (JSONException e) {
            throw new IdMUnitException("An error occured while filling out the workflow data items", e);
        }
    }

    @SuppressWarnings("serial")
    public void setup(Map<String, String> config) throws IdMUnitException {
        serverUrl = config.get(BasicConnector.CONFIG_SERVER);
        final String user = config.get(BasicConnector.CONFIG_USER);
        final String password = config.get(BasicConnector.CONFIG_PASSWORD);


        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                },
        };

        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }

        };

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            throw new IdMUnitException("Error setting up ssl context", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IdMUnitException("Error setting up ssl context", e);
        }

        final ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
        clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, sslContext));
        final Client client = Client.create(clientConfig);

        rootResource = client.resource(serverUrl);

        credentials = new String(Base64.encodeBase64(String.format("%s:%s", user, password).getBytes()));

        defaultHeaders = Collections.unmodifiableMap(new HashMap<String, String>() {{
                put(REST_AUTH, credentials);
                put(ACCEPT, APPLICATION_JSON);
            }});
    }

    public void opStartWorkflow(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        final String workflowIdentifier = getAndRemoveConfigValue(data, WORKFLOW_IDENTIFIER); // This is used to track individual workflow requests in a single test
        final String workflowDn = getAndRemoveConfigValue(data, WORKFLOW_DN);
        final String workflowRecipient = getAndRemoveConfigValue(data, WORKFLOW_RECIPIENT);

        if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_IDENTIFIER));
        }

        if (workflowDn == null || workflowDn.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_DN));
        }

        if (workflowRecipient == null || workflowRecipient.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_RECIPIENT));
        }

        if (WORKFLOW_CACHE.containsKey(workflowIdentifier) || CAPTURE_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException(String.format("%s '%s' already in use", WORKFLOW_IDENTIFIER, workflowIdentifier));
        }

        JSONArray definitions = getDefinitions();
        String definitionGuid = getGuidByDn(definitions, workflowDn);
        if (definitionGuid == null) {
            throw new IdMUnitException(String.format("Specified workflow DN '%s' was not found", workflowDn));
        }

        JSONObject definition = getDefinition(definitionGuid);

        try {
            JSONArray dataItems = definition.getJSONArray(JSON_FIELD_DATAITEMS);
            fillDataItems(dataItems, data, true);
        } catch (JSONException e) {
            throw new IdMUnitException("Error retrieving data items", e);
        }

        String processGuid = sendWorkflow(definition, workflowRecipient);
        log.debug("GUID :" + processGuid);
        WORKFLOW_CACHE.put(workflowIdentifier, processGuid);
    }

    public void opApproveWorkflow(Map<String, Collection<String>> data) throws IdMUnitException {
        performWorkflowApproval(data, true);
    }

    public void opDenyWorkflow(Map<String, Collection<String>> data) throws IdMUnitException {
        performWorkflowApproval(data, false);
    }

    public void opCheckWorkflowStatus(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        final String workflowIdentifier = getAndRemoveConfigValue(data, WORKFLOW_IDENTIFIER); // This is used to track individual workflow requests in a single test
        final String workflowApprovalStatus = getAndRemoveConfigValue(data, WORKFLOW_APPROVAL_STATUS);
        final String workflowProcessStatus = getAndRemoveConfigValue(data, WORKFLOW_PROCESS_STATUS);

        if (!WORKFLOW_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException("Unknown workflow identifier");
        }

        if (CAPTURE_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException("PostCaptureWorkflow must be called before CheckWorkflowStatus");
        }

        if (workflowApprovalStatus == null || workflowApprovalStatus.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_APPROVAL_STATUS));
        }

        if (workflowProcessStatus == null || workflowProcessStatus.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_PROCESS_STATUS));
        }

        JSONObject workflow = getProcessItem(WORKFLOW_CACHE.get(workflowIdentifier));

        try {
            Failures failures = new Failures();
            String actualApprovalStatus = workflow.getString(JSON_FIELD_APPROVAL_STATUS);
            String actualProcessStatus = workflow.getString(JSON_FIELD_PROCESS_STATUS);
            if (!actualApprovalStatus.equals(workflowApprovalStatus)) {
                failures.add("Unexpected approval status value.", Arrays.asList(new String[]{workflowApprovalStatus}), Arrays.asList(new String[]{actualApprovalStatus}));
            }

            if (!actualProcessStatus.equals(workflowProcessStatus)) {
                failures.add("Unexpected process status value.", Arrays.asList(new String[]{workflowProcessStatus}), Arrays.asList(new String[]{actualProcessStatus}));
            }

            if (failures.hasFailures()) {
                throw new IdMUnitException(failures.toString());
            }
        } catch (JSONException e) {
            throw new IdMUnitException("A error occured while retrieving statuses.", e);
        }
    }

    public void opPreCaptureWorkflow(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        final String workflowIdentifier = getAndRemoveConfigValue(data, WORKFLOW_IDENTIFIER); // This is used to track individual workflow requests in a single test
        final String workflowDn = getAndRemoveConfigValue(data, WORKFLOW_DN);

        if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_IDENTIFIER));
        }

        if (workflowDn == null || workflowDn.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_DN));
        }

        if (WORKFLOW_CACHE.containsKey(workflowIdentifier) || CAPTURE_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException(String.format("%s '%s' already in use", WORKFLOW_IDENTIFIER, workflowIdentifier));
        }
        JSONArray processesBefore = getProcessItems(workflowDn);

        CAPTURE_CACHE.put(workflowIdentifier, new PreCapture(workflowDn, processesBefore));
    }

    public void opCaptureWorkflow(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        final String workflowIdentifier = getAndRemoveConfigValue(data, WORKFLOW_IDENTIFIER); // This is used to track individual workflow requests in a single test

        if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_IDENTIFIER));
        }

        if (WORKFLOW_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException(String.format("%s '%s' already in use by StartWorkflow", WORKFLOW_IDENTIFIER, workflowIdentifier));
        }

        if (!CAPTURE_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException(String.format("%s '%s' not found", WORKFLOW_IDENTIFIER, workflowIdentifier));
        }

        PreCapture preCapture = CAPTURE_CACHE.get(workflowIdentifier);
        JSONArray processesAfter = getProcessItems(preCapture.workflowDn);

        String processGuid = getProcessGuid(preCapture.processes, processesAfter);

        CAPTURE_CACHE.remove(workflowIdentifier);
        WORKFLOW_CACHE.put(workflowIdentifier, processGuid);
    }

    public void opCaptureWorkflowFilteredWorkItem(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        final String workflowIdentifier = getAndRemoveConfigValue(data, WORKFLOW_IDENTIFIER); // This is used to track individual workflow requests in a single test
        Collection<String> workflowFilters = data.get(WORKFLOW_FILTER);

        if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_IDENTIFIER));
        }

        if (workflowFilters == null) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_FILTER));
        }

        Set<String> results = new HashSet<String>();
        int count = 0;
        for (String filter : workflowFilters) {
            log.debug("DELE: filter: " + filter);
            log.debug("DELE: cout: " + count);

            if (count == 0) {
                JSONArray workItemsArray = getWorkItemsFromFilter(filter);
                log.debug("DELE: Workitems length:" + workItemsArray.length());
                log.debug("\n");
                for (int i = 0; i < workItemsArray.length(); i++) {
                    log.debug("DELE: Inside for");

                    JSONObject jsonObject;
                    try {
                        log.debug("DELE: Inside try");
                        jsonObject = workItemsArray.getJSONObject(i);
                        String guid = jsonObject.getJSONObject("Process").getString(JSON_FIELD_GUID);
                        log.debug(" GUID: " + guid);
                        results.add(guid);
                        log.debug("DELE: finish try");

                    } catch (JSONException e) {
                        throw new IdMUnitException("Error retrieving process GUID.", e);
                    }
                } //end for
                log.debug(" GUID Count: " + results.size());
                //end if count==0
            } else {
                log.debug("DELE: Inside else statement - ");
                JSONArray workItemsArray = getWorkItemsFromFilter(filter);
                Set<String> currentResluts = new HashSet<String>();

                for (int i = 0; i < workItemsArray.length(); i++) {
                    JSONObject jsonObject;
                    try {
                        log.debug("DELE: Inside else statement - Try");
                        jsonObject = workItemsArray.getJSONObject(i);
                        String guid = jsonObject.getJSONObject("Process").getString(JSON_FIELD_GUID);
                        log.debug("GUID: " + guid);
                        currentResluts.add(guid);
                        log.debug("DELE: End else statement - Try");
                    } catch (JSONException e) {
                        throw new IdMUnitException("Error retrieving process GUID.", e);
                    }
                } //end for

                log.debug("GUID Count: " + currentResluts.size());
                results.retainAll(currentResluts);
            } //end else
            count++;
            if (results.size() == 1) {
                break;
            } //else move on to the next filter and keep cutting down the result set
        } //end for workflowFilters

        int resultCount = results.size();
        if (resultCount != 1) {
            String errorInfo;
            if (resultCount > 1) {
                errorInfo = "Check that the filter gets 1 result. Clean up any left over workflows in UserApp";
            } else { //<1
                errorInfo = "Check that the filter gets 1 result. Check to see if the workflow started up.";
            }
            throw new IdMUnitException("Error. Expected: [1] but Actual: [" + resultCount + "]. " + errorInfo);
        }

        for (String guid : results) {
            //returns the first
            WORKFLOW_CACHE.put(workflowIdentifier, guid);
            return;
        }
    }

    //This shouldn't be used unless you can guarantee your filter will only get the results you want to delete
    public void opCaptureAndDenyAllWorkflowsFromFilteredWorkItem(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        Collection<String> workflowFilters = data.get(WORKFLOW_FILTER);

        if (workflowFilters == null) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_FILTER));
        }

        Set<String> results = new HashSet<String>();
        int count = 0;
        for (String filter : workflowFilters) {
            if (count == 0) {
                JSONArray workItemsArray = getWorkItemsFromFilter(filter);
                for (int i = 0; i < workItemsArray.length(); i++) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = workItemsArray.getJSONObject(i);
                        String guid = jsonObject.getJSONObject("Process").getString(JSON_FIELD_GUID);
                        log.debug(" GUID: " + guid);
                        results.add(guid);

                    } catch (JSONException e) {
                        throw new IdMUnitException("Error retrieving process GUID.", e);
                    }
                } //end for
                log.debug(" GUID Count: " + results.size());
                //end if count==0
            } else {
                JSONArray workItemsArray = getWorkItemsFromFilter(filter);
                Set<String> currentResluts = new HashSet<String>();

                for (int i = 0; i < workItemsArray.length(); i++) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = workItemsArray.getJSONObject(i);
                        String guid = jsonObject.getJSONObject("Process").getString(JSON_FIELD_GUID);
                        log.debug("GUID: " + guid);
                        currentResluts.add(guid);
                    } catch (JSONException e) {
                        throw new IdMUnitException("Error retrieving process GUID.", e);
                    }
                } //end for

                log.debug("GUID Count: " + currentResluts.size());
                results.retainAll(currentResluts);
            } //end else
            count++;
            if (results.size() == 1) {
                break;
            } //else move on to the next filter and keep cutting down the result set
        } //end for workflowFilters

        log.debug("There are [" + results.size() + "] workflow processes that will be denied for this filter.");
        for (String guid : results) {
            JSONArray workItems = getWorkItems(guid);
            try {
                for (int itemNum = 0; itemNum < workItems.length(); itemNum++) {
                    JSONObject item = workItems.getJSONObject(itemNum);
                    JSONObject approvalItem = getWorkItem(getGuid(item));
                    approveWorkItem(approvalItem, false);
                    putJsonObject(String.format("%s/%s", PATH_WORKITEMS, getGuid(approvalItem)), approvalItem, EMPTY_PARAMS);
                }
            } catch (JSONException e) {
                throw new IdMUnitException("Failed work item activity.", e);
            }
        } //end for each GUID
    }

    public void opCaptureWorkflowFilteredProcesses(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        final String workflowIdentifier = getAndRemoveConfigValue(data, WORKFLOW_IDENTIFIER); // This is used to track individual workflow requests in a single test
        Collection<String> workflowFilters = data.get(WORKFLOW_FILTER);

        if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_IDENTIFIER));
        }

        if (workflowFilters == null) {
            throw new IdMUnitException(String.format("The field '%s' is required", WORKFLOW_FILTER));
        }

        Set<String> results = new HashSet<String>();
        int count = 0;
        for (String filter : workflowFilters) {
            if (count == 0) {
                JSONArray processItemsArray = getProcessItemsFromFilter(filter);
                for (int i = 0; i < processItemsArray.length() - 1; i++) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = processItemsArray.getJSONObject(i);
                        String guid = jsonObject.getString(JSON_FIELD_GUID);
                        results.add(guid);

                    } catch (JSONException e) {
                        throw new IdMUnitException("Error retrieving process GUID.", e);
                    }
                } //end for
                log.debug("GUID Count: " + results.size());
                //end if count==0
            } else {
                JSONArray processItemsArray = getProcessItemsFromFilter(filter);
                Set<String> currentResluts = new HashSet<String>();

                for (int i = 0; i < processItemsArray.length() - 1; i++) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = processItemsArray.getJSONObject(i);
                        String guid = jsonObject.getString(JSON_FIELD_GUID);
                        log.debug("GUID: " + guid);
                        currentResluts.add(guid);
                    } catch (JSONException e) {
                        throw new IdMUnitException("Error retrieving process GUID.", e);
                    }
                } //end for
                log.debug("GUID Count: " + currentResluts.size());
                results.retainAll(currentResluts);
            } //end else
            count++;
            if (results.size() == 1) {
                break;
            } //else move on to the next filter and keep cutting down the result set
        } //end for workflowFilters

        int resultCount = results.size();
        if (resultCount != 1) {
            String errorInfo;
            if (resultCount > 1) {
                errorInfo = "Check that the filter gets 1 result. Clean up any left over workflows in UserApp";
            } else { //<1
                errorInfo = "Check that the filter gets 1 result. Check to see if the workflow started up.";
            }
            throw new IdMUnitException("Error. Expected: [1] but Actual: [" + resultCount + "]. " + errorInfo);
        }

        for (String guid : results) {
            //returns the first
            WORKFLOW_CACHE.put(workflowIdentifier, guid);
            return;
        }
    }

    public void opTestConnection(Map<String, Collection<String>> data) throws IdMUnitException {
        getDefinitions();
    }

    private void performWorkflowApproval(Map<String, Collection<String>> data, boolean approve) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        final String workflowIdentifier = getAndRemoveConfigValue(data, WORKFLOW_IDENTIFIER); // This is used to track individual workflow requests in a single test

        if (CAPTURE_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException("PostCaptureWorkflow must be called before performing workflow approval/reject.");
        }

        if (!WORKFLOW_CACHE.containsKey(workflowIdentifier)) {
            throw new IdMUnitException("Unknown workflow identifier");
        }

        JSONArray workItems = getWorkItems(WORKFLOW_CACHE.get(workflowIdentifier));

        try {
            for (int itemNum = 0; itemNum < workItems.length(); itemNum++) {
                JSONObject item = workItems.getJSONObject(itemNum);
                JSONObject approvalItem = getWorkItem(getGuid(item));
                approveWorkItem(approvalItem, approve);
                fillDataItems(approvalItem.getJSONArray(JSON_FIELD_DATAITEMS), data, true);
                putJsonObject(String.format("%s/%s", PATH_WORKITEMS, getGuid(approvalItem)), approvalItem, EMPTY_PARAMS);
            }
        } catch (JSONException e) {
            throw new IdMUnitException("Failed work item activity.", e);
        }
    }

    private void approveWorkItem(JSONObject item, boolean approve) throws IdMUnitException {
        try {
            JSONObject action = new JSONObject();
            action.put(JSON_FIELD_CODE, approve ? "0" : "1");
            action.put(JSON_FIELD_VALUE, approve ? "Approve" : "Deny");
            JSONArray actions = new JSONArray();
            actions.put(action);
            item.put(JSON_FIELD_AVAILABLE_ACTIONS, actions);
        } catch (JSONException e) {
            throw new IdMUnitException("Failure creating approved work item.", e);
        }
    }

    private String getProcessGuid(JSONArray beforeItems, JSONArray afterItems) throws IdMUnitException {
        int diffSize = afterItems.length() - beforeItems.length();

        if (diffSize <= 0) {
            throw new IdMUnitException("Unable to determine process GUID. After submission process count is less than or equal to before submission.");
        }

        if (diffSize > 1) {
            throw new IdMUnitException("Unable to determine process GUID. Number of processes after submission is >1 larger than before submission.");
        }

        try {
            Set<String> beforeGuids = new HashSet<String>();
            for (int beforeNum = 0; beforeNum < beforeItems.length(); beforeNum++) {
                JSONObject beforeItem = beforeItems.getJSONObject(beforeNum);
                beforeGuids.add(beforeItem.getString(JSON_FIELD_GUID));
            }

            for (int afterNum = 0; afterNum < afterItems.length(); afterNum++) {
                JSONObject afterItem = afterItems.getJSONObject(afterNum);
                String guid = afterItem.getString(JSON_FIELD_GUID);
                if (!beforeGuids.contains(guid)) {
                    return guid;
                }
            }
        } catch (JSONException e) {
            throw new IdMUnitException("Error retrieving process GUID.", e);
        }

        throw new IdMUnitException("Error retrieving process GUID.");
    }

    @SuppressWarnings("serial")
    private JSONArray getProcessItems(final String workflowDn) {
        Map<String, String> params = new HashMap<String, String>() {{
                put(PARAM_FILTER, String.format("%s=%s", FILTER_DEFINITION, workflowDn));
            }};

        return getJsonArray(PATH_PROCESSES, params);
    }

    @SuppressWarnings("serial")
    private JSONArray getWorkItemsFromFilter(final String filter) {
        Map<String, String> params = new HashMap<String, String>() {{
                put(PARAM_FILTER, filter);
            }};

        return getJsonArray(PATH_WORKITEMS, params);
    }

    @SuppressWarnings("serial")
    private JSONArray getProcessItemsFromFilter(final String filter) {
        Map<String, String> params = new HashMap<String, String>() {{
                put(PARAM_FILTER, filter);
            }};

        return getJsonArray(PATH_PROCESSES, params);
    }

    private JSONObject getProcessItem(final String guid) {
        return getJsonObject(String.format("%s/%s", PATH_PROCESSES, guid), EMPTY_PARAMS);
    }

    private JSONObject getWorkItem(String guid) {
        return getJsonObject(String.format("%s/%s", PATH_WORKITEMS, guid), EMPTY_PARAMS);
    }

    @SuppressWarnings("serial")
    private JSONArray getWorkItems(final String processGuid) {
        Map<String, String> params = new HashMap<String, String>() {{
                put(PARAM_FILTER, String.format("%s=%s", FILTER_PROCESSID, processGuid));
            }};

        return getJsonArray(PATH_WORKITEMS, params);
    }

    private String getAndRemoveConfigValue(Map<String, Collection<String>> data, String name) throws IdMUnitException {
        String value = ConnectorUtil.getSingleValue(data, name);
        data.remove(name);
        return value;
    }

    private String sendWorkflow(JSONObject definition, String recipientDn) throws IdMUnitException {
        String path = String.format("%s/%s", PATH_DEFINITIONS, getGuid(definition));
        try {
            definition.put(JSON_FIELD_RECIPIENT, recipientDn);
        } catch (JSONException e) {
            throw new IdMUnitException("Failure setting recipient while starting workflow");
        }
        return postJsonObject(path, definition, EMPTY_PARAMS);
    }

    private JSONArray getDefinitions() {
        return getJsonArray(PATH_DEFINITIONS, EMPTY_PARAMS);
    }

    private JSONObject getDefinition(String guid) {
        return getJsonObject(String.format("%s/%s", PATH_DEFINITIONS, guid), EMPTY_PARAMS);
    }

    private String getGuidByDn(JSONArray items, String workflowDn) throws IdMUnitException {
        try {
            for (int defNum = 0; defNum < items.length(); defNum++) {
                JSONObject definition = items.getJSONObject(defNum);
                if (definition != null) {
                    if (definition.getString(JSON_FIELD_DN).equalsIgnoreCase(workflowDn)) {
                        return getGuid(definition);
                    }
                }
            }
        } catch (JSONException e) {
            throw new IdMUnitException("Error retrieving workflow guid.", e);
        }

        throw new IdMUnitException("Error retrieving workflow guid");
    }

    private String getGuid(JSONObject item) throws IdMUnitException {
        try {
            return item.getString(JSON_FIELD_GUID);
        } catch (JSONException e) {
            throw new IdMUnitException("Error retrieving workflow guid.", e);
        }
    }

    private JSONObject getJsonObject(String path, Map<String, String> params) {
        WebResource resource = rootResource.path(path);

        for (Map.Entry<String, String> param : params.entrySet()) {
            resource = resource.queryParam(param.getKey(), param.getValue());
        }

        return addDefaultRequestHeaders(resource).get(JSONObject.class);
    }

    private JSONArray getJsonArray(String path, Map<String, String> params) {
        WebResource resource = rootResource.path(path);
        for (Map.Entry<String, String> param : params.entrySet()) {
            resource = resource.queryParam(param.getKey(), param.getValue());
        }

        return addDefaultRequestHeaders(resource).get(JSONArray.class);
    }

    private String postJsonObject(String path, JSONObject object, Map<String, String> params) throws IdMUnitException {
        WebResource resource = rootResource.path(path);

        for (Map.Entry<String, String> param : params.entrySet()) {
            resource = resource.queryParam(param.getKey(), param.getValue());
        }

        ClientResponse response = addDefaultRequestHeaders(resource).post(ClientResponse.class, object);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new IdMUnitException(String.format("Status code '%s' not expected while posting to '%s'", response.getStatus(), path));
        }
        try {
            JSONObject o = new JSONObject(IOUtils.toString(response.getEntityInputStream()));
            return o.getString("requestID");
        } catch (IOException e) {
            throw new IdMUnitException("Unable to parse REST response", e);
        } catch (JSONException e) {
            throw new IdMUnitException("Unable to parse REST response", e);
        }
    }

    private void putJsonObject(String path, JSONObject object, Map<String, String> params) throws IdMUnitException {
        WebResource resource = rootResource.path(path);

        for (Map.Entry<String, String> param : params.entrySet()) {
            resource = resource.queryParam(param.getKey(), param.getValue());
        }

        ClientResponse response = addDefaultRequestHeaders(resource).put(ClientResponse.class, object);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new IdMUnitException(String.format("Status code '%s' not expected while posting to '%s'", response.getStatus(), path));
        }
    }

    private Builder addRequestHeaders(WebResource resource, Map<String, String> headers) {
        Builder builder = resource.getRequestBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder = builder.header(entry.getKey(), entry.getValue());
        }

        return builder;
    }

    private Builder addDefaultRequestHeaders(WebResource resource) {
        return addRequestHeaders(resource, defaultHeaders);
    }

    private final class PreCapture {
        private final String workflowDn;
        private final JSONArray processes;

        private PreCapture(String workflowDn, JSONArray processes) {
            this.workflowDn = workflowDn;
            this.processes = processes;
        }
    }
}
