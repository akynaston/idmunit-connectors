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

import com.google.gson.*;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OneIdentityConnector extends AbstractConnector {

    private static final String MODULE = "module";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SERVER = "server";
    private static final String JOBID = "jobID";
    private static final String TABLENAME = "tableName";
    private static final String EVENT = "event";
    private static final String NAMING = "naming";
    private static final String NAMEVALUE = "namevalue";
    private static final String UIDNAME = "uidName";
    private static final String UID = "UID";

    private static Logger log = LoggerFactory.getLogger(OneIdentityConnector.class);
    private RestClient rest;
    private Gson gson;
    private String sessionId;


    public void setup(Map<String, String> config) throws IdMUnitException {

        String module = config.get(MODULE);
        String user = config.get(USER);
        String password = config.get(PASSWORD);
        String server = config.get(SERVER);

        if (module == null) {
            throw new IdMUnitException("Missing configuration for Module");
        }
        if (user == null) {
            throw new IdMUnitException("Missing configuration for User");
        }
        if (password == null) {
            throw new IdMUnitException("Missing configuration for Password");
        }
        if (server == null) {
            throw new IdMUnitException("Missing configuration for Server");
        }
        auth(module, user, password, server);
    }

    @Override
    public void tearDown() throws IdMUnitException {

    }

    private void auth(String module, String user, String password, String server) throws IdMUnitException {

        gson = new GsonBuilder().create();
        rest = RestClient.init(server, user, password, false);

        String path = "/auth/apphost";
        String request = "{\"authString\":\"Module=" + module + ";User=" + user + ";Password=" + password + "\"}";
        JsonObject response = gson.fromJson(rest.executePost(path, request).messageBody, JsonObject.class);
        sessionId = response.get("sessionId").getAsString();
    }

    public void opStartJob(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String jobID = ConnectorUtil.getSingleValue(attrs, JOBID);
        String tableName = ConnectorUtil.getSingleValue(attrs, TABLENAME);

        if (jobID == null) {
            throw new IdMUnitException("No value specified for 'JobID'");
        }
        if (tableName == null) {
            throw new IdMUnitException("No value specified for 'TableName'");
        }

        String path = "/api/entity/" + tableName + "/" + jobID + "/event/run";
        String request = "{\n" +
                "  \"parameters\": {\n" +
                "    \"StringValue\": \"" + ConnectorUtil.getSingleValue(attrs, "StringValue") + "\",\n" +
                "    \"IntValue\": " + ConnectorUtil.getSingleValue(attrs, "IntValue") + ",\n" +
                "    \"DateValue\": \"" + ConnectorUtil.getSingleValue(attrs, "DateValue") + "\"\n" +
                "  }\n" +
                "}";
        rest.executePut(path, request, sessionId);
    }

    public void opStartEvent(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String Uid = ConnectorUtil.getSingleValue(attrs, UID);
        String tableName = ConnectorUtil.getSingleValue(attrs, TABLENAME);
        String event = ConnectorUtil.getSingleValue(attrs, EVENT);

        if (Uid == null) {
            throw new IdMUnitException("No value specified for 'UID'");
        }
        if (tableName == null) {
            throw new IdMUnitException("No value specified for 'TableName'");
        }
        if (event == null) {
            throw new IdMUnitException("No value specified for 'event'");
        }
        String path = "/api/entity/" + tableName + "/" + Uid + "/event/" + event;
        log.info("Starting event: " + path);
        String request = "{\n" +
                "  \"parameters\": {\n" +
                "    \"StringValue\": \"" + ConnectorUtil.getSingleValue(attrs, "StringValue") + "\",\n" +
                "    \"IntValue\": " + ConnectorUtil.getSingleValue(attrs, "IntValue") + ",\n" +
                "    \"DateValue\": \"" + ConnectorUtil.getSingleValue(attrs, "DateValue") + "\"\n" +
                "  }\n" +
                "}";
        rest.executePut(path, request, sessionId);
    }

    public void opValidateObject(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String cn = ConnectorUtil.getSingleValue(attrs, "cn");
        String tableName = ConnectorUtil.getSingleValue(attrs, TABLENAME);
        if (cn == null || cn.isEmpty()) {
            throw new IdMUnitException("No value for 'cn' provided");
        }
        if (tableName == null || tableName.isEmpty()) {
            throw new IdMUnitException("No value specified for 'TableName'");
        }
        String URLcn = cn.replace(" ", "+");
        // log.info("cn: " + cn + " and URL cn: " + URLcn);

        String path = "/api/entities/" + tableName + "?loadType=Default&format=json&cn=" + URLcn;
        // String testPath = "/api/entity/ADSAccount/fe4f62df-67fc-446f-ae36-4c89f99d4e3d";
        JsonArray obj = gson.fromJson(rest.executeGet(path, sessionId).messageBody, JsonArray.class);
        //log.info("obj: " + obj);
        //String uid = obj.getAsJsonObject("values").get("UID_" + tableName).getAsString();

        String uid = getUidByCn(obj, cn);
        if (uid == null || uid.isEmpty()) {
            throw new IdMUnitException("No UID was found in JSON response");
        }
        // log.info("uid of user: " + uid);
        path = "/api/entity/" + tableName + "/" + uid + "?format=json";
        JsonObject userObj = gson.fromJson(rest.executeGet(path, sessionId).messageBody, JsonObject.class);

        for (String key : attrs.keySet()) {

            if (TABLENAME.equals(key)) {
                continue;
            }
            // log.info("Validating attributes for: " + key);
            String expectedAttr = ConnectorUtil.getSingleValue(attrs, key);
            String actualAttr = userObj.getAsJsonObject("values").get(key).getAsString();
            // log.info("Attr retrieved: " + actualAttr);
            if (expectedAttr.equals(actualAttr)) {
                log.info(key + " expected value: " + expectedAttr + " matches actual value: " + actualAttr);
            } else {
                //throw exception if values do not match
                throw new IdMUnitException("Values for " + key + ": <Expected>: " + expectedAttr + " but was <Actual>: " + actualAttr);
            }
        }
    }

    public void opValidateAnyObject(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String naming = ConnectorUtil.getSingleValue(attrs, NAMING);
        String namevalue = ConnectorUtil.getSingleValue(attrs, NAMEVALUE);
        String tableName = ConnectorUtil.getSingleValue(attrs, TABLENAME);
        String uidName = ConnectorUtil.getSingleValue(attrs, UIDNAME);

        if (naming == null || naming.isEmpty()) {
            throw new IdMUnitException("No value for 'naming' provided");
        }
        if (tableName == null || tableName.isEmpty()) {
            throw new IdMUnitException("No value specified for 'TableName'");
        }
        String URLnaming = naming.replace(" ", "+");
        // log.info("naming: " + naming + " and URL namingcn: " + URLnaming);
        if (namevalue == null || namevalue.isEmpty()) {
            throw new IdMUnitException("No value specified for 'namevalue'");
        }
        String URLnamevalue = namevalue.replace(" ", "+");
        // log.info("namevalue: " + namevalue + " and URLnamevalue: " + URLnamevalue);
        if (uidName == null || uidName.isEmpty()) {
            throw new IdMUnitException("No value specified for 'uidName'");
        }

        String path = "/api/entities/" + tableName + "?loadType=Default&format=json&" + URLnaming + "=" + URLnamevalue;
        // log.info("path: " + path);
        // String testPath = "/api/entity/ADSAccount/fe4f62df-67fc-446f-ae36-4c89f99d4e3d";
        JsonArray obj = gson.fromJson(rest.executeGet(path, sessionId).messageBody, JsonArray.class);
        //log.info("obj: " + obj);
        //String uid = obj.getAsJsonObject("values").get("UID_" + tableName).getAsString();

        String uid = getUidByNaming(obj, namevalue, naming, uidName);
        if (uid == null || uid.isEmpty()) {
            throw new IdMUnitException("No UID was found in any JSON response");
        }
        // log.info("uid of user: " + uid);
        path = "/api/entity/" + tableName + "/" + uid + "?format=json";
        // log.info("path: " + path);
        JsonObject retobj = gson.fromJson(rest.executeGet(path, sessionId).messageBody, JsonObject.class);

        for (String key : attrs.keySet()) {

            if (TABLENAME.equals(key)) {
                continue;
            }
            log.info("Validating attributes for: " + key);
            String expectedAttr = ConnectorUtil.getSingleValue(attrs, key);
            String actualAttr = retobj.getAsJsonObject("values").get(key).getAsString();
            log.info("Attr retrieved: " + actualAttr);
            if (expectedAttr.equals(actualAttr)) {
                log.info(key + " expected value: " + expectedAttr + " matches actual value: " + actualAttr);
            } else {
                //throw exception if values do not match
                throw new IdMUnitException("Values for " + key + ": <Expected>: " + expectedAttr + " but was <Actual>: " + actualAttr);
            }
        }
    }

    private String getUidByCn(JsonArray obj, String objectCn) throws IdMUnitException {
        try {
            obj.size();
            for (int objNum = 0; objNum < obj.size(); objNum++) {
                JsonElement user = obj.get(objNum);
                JsonObject valobj = user.getAsJsonObject();
                JsonObject values = valobj.getAsJsonObject("values");
                String jsonCn = values.get("cn").getAsString();
                String jsonUid = values.get("UID_ADSAccount").getAsString();
                if (jsonCn.equalsIgnoreCase(objectCn)) {
                    return jsonUid;
                }
            }
        } catch (JsonParseException e) {
            throw new IdMUnitException("Error parsing user UID.", e);
        }
        return null;
    }

    private String getUidByNaming(JsonArray obj, String nameValue, String naming, String uidName) throws IdMUnitException {
        try {
            // log.info("nameValue: " + nameValue + " Naming: " + Naming + " uidName: " + uidName + " obj.size: " + obj.size());
            obj.size();
            for (int objNum = 0; objNum < obj.size(); objNum++) {
                JsonElement user = obj.get(objNum);
                JsonObject valobj = user.getAsJsonObject();
                JsonObject values = valobj.getAsJsonObject("values");
                String jsonCn = values.get(naming).getAsString();
                //   log.info("jsonCn: " + jsonCn);
                JsonElement ejsonUid = values.get(uidName);
                // log.info("got element: " + ejsonUid);
                String jsonUid = ejsonUid.getAsString();
                // log.info("after getstring");
                return jsonUid;
            }

        } catch (JsonParseException e) {
            throw new IdMUnitException("Error parsing user UID.", e);
        }
        //log.info("returning null uid");
        return null;
    }
}
