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
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

public class OpenIdmConnector extends AbstractConnector {
    private static final String SERVER = "server";
    private static final String PORT = "port";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SSL_CONNECTION = "sslConnection";
    private static final String CONFIG_TRUST_ALL_CERTS = "trust-all-certs";
    private static Logger log = LoggerFactory.getLogger(OpenIdmConnector.class);

    private static final String DEFAULT_PORT = "8080";

    private RestClient rest;
    private Gson gson = new Gson();

    public void setup(Map<String, String> config) throws IdMUnitException {
        String server = config.get(SERVER);
        if (server == null) {
            throw new IdMUnitException("Missing configuration for '" + SERVER + "'");
        }

        String port = config.get(PORT);
        if (port == null) {
            port = DEFAULT_PORT;
        }

        String username = config.get(USER);
        if (username == null) {
            throw new IdMUnitException("Missing configuration for '" + USER + "'");
        }

        String password = config.get(PASSWORD);
        if (password == null) {
            throw new IdMUnitException("Missing configuration for '" + PASSWORD + "'");
        }

        if (config.get(CONFIG_TRUST_ALL_CERTS) != null && Boolean.valueOf(config.get(CONFIG_TRUST_ALL_CERTS))) {
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String string, SSLSession ssls) {
                        return true;
                    }
                });
            } catch (NoSuchAlgorithmException e) {
                //handle exception
            } catch (KeyManagementException e) {
                //handle exception
            }

        }

        boolean sslConnect = config.get(SSL_CONNECTION) != null && Boolean.valueOf(config.get(SSL_CONNECTION));

        rest = RestClient.init(server, port, username, password, sslConnect);
    }

    @SuppressWarnings("unused")
    public void opAddObject(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String objectType = ConnectorUtil.getSingleValue(attrs, "objectType");

        log.info("...performing user creation");

        if (objectType == null) {
            throw new IdMUnitException("No object type provided");
        }
        attrs.remove("objectType");
        JsonParser jsonParser = new JsonParser();
        JsonObject request = new JsonObject();
        for (String attrName : attrs.keySet()) {
            if (attrName.endsWith("[]")) {
                JsonArray vals = new JsonArray();
                for (String valItem : attrs.get(attrName)) {
                    vals.add(new JsonPrimitive(valItem));
                }
                request.add(attrName.substring(0, attrName.length() - "[]".length()), vals);
            } else if (attrName.contains("::")) {
                String[] attrNameSplit = attrName.split("::");
                if (attrNameSplit[1].equals("boolean")) {
                    if (ConnectorUtil.getSingleValue(attrs, attrName).equalsIgnoreCase("true")) {
                        request.addProperty(attrNameSplit[0], true);
                    }
                    if (ConnectorUtil.getSingleValue(attrs, attrName).equalsIgnoreCase("false")) {
                        request.addProperty(attrNameSplit[0], false);
                    }
                } else if (attrNameSplit[1].equals("string")) {
                    request.addProperty(attrNameSplit[0], ConnectorUtil.getSingleValue(attrs, attrName));
                } else {
                    throw new IdMUnitException(String.format("Unknown type following '::' in column header. Expected 'boolean', got '%s'.", attrNameSplit[1]));
                }
            } else {
                String value = ConnectorUtil.getSingleValue(attrs, attrName);
                try {
                    request.add(attrName, jsonParser.parse(value));
                } catch (JsonSyntaxException e) {
                    log.debug("Error parsing {} ({}), adding it as a string value.", attrName, value);
                    request.addProperty(attrName, value);
                }
            }
        }
        rest.executePost("/managed/" + objectType + "?_action=create", gson.toJson(request));
    }

    @SuppressWarnings("unused")
    public void opDeleteObjectLeaveLinks(Map<String, Collection<String>> attrs) throws IdMUnitException {
        deleteObject(attrs, false);
    }

    @SuppressWarnings("unused")
    public void opDeleteObject(Map<String, Collection<String>> attrs) throws IdMUnitException {
        deleteObject(attrs, true);
    }

    private void deleteObject(Map<String, Collection<String>> attrs, boolean deleteLinks) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String objectType = ConnectorUtil.getSingleValue(attrs, "objectType");
        if (objectType == null) {
            throw new IdMUnitException("No object type provided");
        }
        attrs.remove("objectType");
        String id = ConnectorUtil.getSingleValue(attrs, "_id");
        if (id == null) {
            String userName = ConnectorUtil.getSingleValue(attrs, "userName");
            if (userName == null) {
                throw new IdMUnitException("No '_id' or 'userName' specified for the user");
            }
            try {
                id = getIdFromUserName(userName);
            } catch (IdMUnitException e) {
                if (e.getMessage().contains("\"resultCount\":0")) {
                    // if no user is returned in the query by userName, then do not fail the delete operation
                    return;
                } else {
                    throw e;
                }
            }
        }

        log.info("...performing delete for 'id' of [" + id + "]");

        if (deleteLinks) {
            String request = String.format("/repo/link/?_queryFilter=firstId eq \"%1$s\" or secondId eq \"%1$s\"", id);
            request = request.replaceAll(" ", "%20");
            String links = rest.executeGet(request).messageBody;
            JsonParser parser = new JsonParser();
            JsonObject linkJson = (JsonObject)parser.parse(links);

            for (JsonElement e : linkJson.getAsJsonArray("result")) {
                rest.executeDelete("/repo/link/" + e.getAsJsonObject().get("_id").getAsString(), e.getAsJsonObject().get("_rev").getAsString());
            }
        }

        try {
            rest.executeDelete("/managed/" + objectType + "/" + id);
        } catch (RestError e) {
            if (!e.getErrorCode().equals("404")) {
                throw new IdMUnitException("Deletion failure: " + e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings("unused")
    public void opReconcile(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String id = ConnectorUtil.getSingleValue(attrs, "_id");
        String mapping = ConnectorUtil.getSingleValue(attrs, "mapping");

        if (mapping == null) {
            throw new IdMUnitException("No 'mapping' specified for the reconcile action");
        }
        if (attrs.size() == 1) {
            log.info("...performing reconciliation for [" + mapping + "]");
            RestClient.Response response = rest.executePost("/recon?_action=recon&mapping=" + mapping + "&waitForCompletion=true");
            log.info(response.messageBody);
            verifyReconResponse(response.messageBody);
        } else if (attrs.size() == 2) {
            if (id != null) {
                log.info(String.format("...performing single user reconciliation with '_id' of [%s] and 'mapping' of [%s]", id, mapping));
                RestClient.Response response = rest.executePost("/recon?_action=reconById&mapping=" + mapping + "&ids=" + id + "&waitForCompletion=true");
                log.info(response.messageBody);
            } else {
                attrs.remove("mapping");
                String sourceAttrName = attrs.keySet().toArray()[0].toString();
                String sourceAttrValue = ConnectorUtil.getSingleValue(attrs, sourceAttrName);
                JsonObject syncConfig = new JsonParser().parse(rest.executeGet("/config/sync").messageBody).getAsJsonObject();
                String sourceString = null;
                for (JsonElement mappingItem : syncConfig.get("mappings").getAsJsonArray()) {
                    if (mapping.equals(mappingItem.getAsJsonObject().get("name").getAsString())) {
                        sourceString = mappingItem.getAsJsonObject().get("source").getAsString();
                        break;
                    }
                }
                if (sourceString == null) {
                    throw new IdMUnitException(String.format("No 'mapping' of %s found", mapping));
                }
                String queryString = String.format("/%s?_queryFilter=%s eq \"%s\"&_fields=_id,%2$s", sourceString, sourceAttrName, sourceAttrValue);
                JsonObject userFromMappedSystem = new JsonParser().parse(rest.executeGet(queryString.replaceAll(" ", "%20")).messageBody).getAsJsonObject();
                if (userFromMappedSystem.get("result").getAsJsonArray().size() == 0) {
                    throw new IdMUnitException(String.format("User %s was not found in mapped system %s.", sourceAttrValue, mapping));
                }
                id = userFromMappedSystem.get("result").getAsJsonArray().get(0).getAsJsonObject().get("_id").getAsString();
                log.info(String.format("...user was found with id of [%s] using search attribute name of [%s] with value of [%s]", id, sourceAttrName, sourceAttrValue));
                log.info(String.format("...performing single user reconciliation with an id of [%s] and 'mapping' of [%s]", id, mapping));
                RestClient.Response response = rest.executePost("/recon?_action=reconById&mapping=" + mapping + "&ids=" + id + "&waitForCompletion=true");
                log.info("...reconciliation successful :" + response.messageBody);

                verifyReconResponse(response.messageBody);
            }
        } else {
            attrs.remove("mapping");
            throw new IdMUnitException(String.format("Ambiguous search attribute. Expected (1) search attribute, got (%d): %s.", attrs.size(), attrs.keySet().toString()));
        }
    }

    private void verifyReconResponse(String responseBody) throws IdMUnitException {
        JsonParser jsonParser = new JsonParser();
        String reconId = jsonParser.parse(responseBody).getAsJsonObject().get("_id").getAsString();
        String status = jsonParser.parse(responseBody).getAsJsonObject().get("state").getAsString();
        if ("SUCCESS".equalsIgnoreCase(status)) {
            RestClient.Response response = rest.executeGet("/recon/" + reconId);
            int numOfFailures = jsonParser.parse(response.messageBody).getAsJsonObject().get("statusSummary").getAsJsonObject().get("FAILURE").getAsInt();
            if (numOfFailures > 0) {
                throw new IdMUnitException(String.format("Reconciliation failed with (%d) failures. Your OpenIDM logs may have more information.", numOfFailures));
            }
        } else {
            throw new IdMUnitException(String.format("Reconciliation completed with unexpected state of %s", status));
        }
    }

    @SuppressWarnings("unused")
    public void opLiveSync(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String sourceSystem = ConnectorUtil.getSingleValue(attrs, "sourceSystem");
        if (sourceSystem == null) {
            throw new IdMUnitException("No source system provided");
        }

        log.info("...performing LiveSync on 'sourceSystem' of [" + sourceSystem + "]");

        RestClient.Response response = rest.executePost(String.format("/%s?_action=liveSync&detailedFailure=true", sourceSystem));
        if (response.statusCode != 200) {
            throw new IdMUnitException(String.format("LiveSync failed on 'sourceSystem' of [%s] with the following response: %s", sourceSystem, response.messageBody));
        }
    }

    private void opPatchObject(String operation, Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String objectType = ConnectorUtil.getSingleValue(attrs, "objectType");
        if (objectType == null) {
            throw new IdMUnitException("No object type provided");
        }
        attrs.remove("objectType");
        String id = ConnectorUtil.getSingleValue(attrs, "_id");
        if (id == null) {
            String userName = ConnectorUtil.getSingleValue(attrs, "userName");
            if (userName == null) {
                throw new IdMUnitException("No '_id' or 'userName' specified for the user");
            }
            id = getIdFromUserName(userName);
        }

        log.info("...performing attribute modification for user with id of [" + id + "]");

        attrs.remove("_id");
        attrs.remove("userName");
        JsonArray request = new JsonArray();
        for (String attrName : attrs.keySet()) {
            /*nested for loop. for each key we create a new object in our request. each object will have
            operation: add/remove/replace, whatever we passed in
            field: ""this is where we put the keyset name. with / in front of it
            value: "'this is where we put the value that we get from the map we passed in.
              */
            JsonObject op = new JsonObject();
            op.addProperty("operation", operation);
            if (attrName.endsWith("[]")) {
                op.addProperty("field", "/" + attrName.substring(0, attrName.length() - "[]".length()));
                JsonArray values = new JsonArray();
                for (String value : attrs.get(attrName)) {
                    values.add(new JsonPrimitive(value));
                }
                op.add("value", values);
            } else if (attrName.contains("::")) {
                String[] attrNameSplit = attrName.split("::");
                if (attrNameSplit[1].equals("boolean")) {
                    if (ConnectorUtil.getSingleValue(attrs, attrName).equals("true")) {
                        op.addProperty("field", "/" + attrNameSplit[0]);
                        op.addProperty("value", true);
                    }
                    if (ConnectorUtil.getSingleValue(attrs, attrName).equals("false")) {
                        op.addProperty("field", "/" + attrNameSplit[0]);
                        op.addProperty("value", false);
                    }
                } else if (attrNameSplit[1].equals("string")) {
                    op.addProperty("field", "/" + attrNameSplit[0]);
                    op.addProperty("value", ConnectorUtil.getSingleValue(attrs, attrName));
                } else {
                    throw new IdMUnitException(String.format("Unknown type following '::' in column header. Expected 'boolean', got '%s'.", attrNameSplit[1]));
                }
            } else {
                op.addProperty("field", "/" + attrName);
                op.addProperty("value", ConnectorUtil.getSingleValue(attrs, attrName));
            }

            request.add(op);
        }

        rest.executePost("/managed/" + objectType + "/" + id + "?_action=patch", gson.toJson(request));
    }

    public void opRemoveAttribute(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String objectType = ConnectorUtil.getSingleValue(attrs, "objectType");
        if (objectType == null) {
            throw new IdMUnitException("No object type provided");
        }
        attrs.remove("objectType");
        String id = ConnectorUtil.getSingleValue(attrs, "_id");
        if (id == null) {
            String userName = ConnectorUtil.getSingleValue(attrs, "userName");
            if (userName == null) {
                throw new IdMUnitException("No '_id' or 'userName' specified for the selected user");
            }
            id = getIdFromUserName(userName);
        }

        log.info("...performing remove attribute operation for user with id of [" + id + "]");

        attrs.remove("_id");
        attrs.remove("userName");
        JsonArray request = new JsonArray();
        for (String attrName : attrs.keySet()) {
            JsonObject op = new JsonObject();
            op.addProperty("operation", "remove");
            if (attrName.endsWith("[]")) {
                op.addProperty("field", "/" + attrName.substring(0, attrName.length() - "[]".length()));
            } else if (attrName.contains("::")) {
                String[] attrNameSplit = attrName.split("::");
                op.addProperty("field", "/" + attrNameSplit[0]);
            } else {
                op.addProperty("field", "/" + attrName);
            }

            request.add(op);
        }

        rest.executePost("/managed/" + objectType + "/" + id + "?_action=patch", gson.toJson(request));
    }

    public void opAddAttribute(Map<String, Collection<String>> attrs) throws IdMUnitException {
        opPatchObject("add", attrs);
    }

    public void opReplaceAttribute(Map<String, Collection<String>> attrs) throws IdMUnitException {
        opPatchObject("replace", attrs);
    }

    public void opValidateLink(Map<String, Collection<String>> attrs) throws IdMUnitException {
        String linkType = ConnectorUtil.getSingleValue(attrs, "linkType");
        if (linkType == null) {
            throw new IdMUnitException("No linkType specified");
        }

        String objecType = ConnectorUtil.getSingleValue(attrs, "objectType");
        if (objecType != null) {
            String userName = ConnectorUtil.getSingleValue(attrs, "userName");
            String id = getIdFromUserName(userName);
            if (id == null) {
                log.error("Unable to get id for user '" + userName + "'");
                throw new IdMUnitException("Unable to get id for user '" + userName + "'");
            }

            String queryFilter;
            try {
                queryFilter = URLEncoder.encode("firstId eq \"" + id + "\" and linkType eq \"" + linkType + "\"", "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            String url = "/repo/link/?_queryFilter=" + queryFilter;

            RestClient.Response response = rest.executeGet(url);
            JsonObject searchResults = new JsonParser().parse(response.messageBody).getAsJsonObject();
            searchResults.getAsJsonPrimitive("resultCount");
            if (searchResults.get("resultCount") == null) {
                log.error("An error occurred searching for links, statusCode '" + response.statusCode + "' message body '" + response.messageBody + "'");
                throw new IdMUnitException("Error searching for links to validate");
            }

            if (searchResults.get("resultCount").getAsInt() == 0) {
                log.info("No links returned from search");
                throw new IdMUnitException("No links returned from search");
            }

            if (searchResults.get("resultCount").getAsInt() != 1) {
                log.info("More than one link was returned from search");
                throw new IdMUnitException("More than one link was returned from search");
            }
        }
    }

    @SuppressWarnings("unused")
    public void opValidateObject(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new LinkedHashMap<String, Collection<String>>(attrs);

        String resourceType;

        String objectType = removeAttr(attrs, "objectType");
        if (objectType != null) {
            resourceType = "managed/" + objectType;
        } else {
            resourceType = removeAttr(attrs, "resourceType");
        }

        if (resourceType == null) {
            throw new IdMUnitException("No object or resource type provided");
        }

        List<String> failures = new ArrayList<String>();

        String fields = "_fields=*,*_ref";
        String url = "/" + resourceType;

        JsonObject actualOutput;

        String id = ConnectorUtil.getSingleValue(attrs, "_id");
        if (id != null) {
            url += "/" + id +  "?" + fields;
            log.info("...performing validation for " + objectType + " with id of [" + id + "]");
            RestClient.Response response = rest.executeGet(url + "/" + id +  "?" + fields);
            actualOutput = new JsonParser().parse(response.messageBody).getAsJsonObject();
        } else {
            String userName = removeAttr(attrs, "userName");
            if (userName != null) {
                url += "?_queryId=for-userName&uid=" + userName + "&" + fields;
                log.info("...performing validation for " + objectType + " with userName of [" + userName + "]");
            } else {
                String queryId = removeAttr(attrs, "queryId");
                if (queryId != null) {
                    String queryIdArgs = removeAttr(attrs, "queryIdArgs");
                    if (queryIdArgs == null) {
                        throw new IdMUnitException("No 'queryIdArgs' specified for queryId '" + queryId + "'");
                    }

                    url += "?_queryId=" + queryId + "&" + queryIdArgs + "&" + fields;
                    log.info("...performing validation for " + objectType + " with queryId of [" + queryId + ", args=" + queryIdArgs + "]");
                } else {
                    String queryFilter = removeAttr(attrs, "queryFilter");
                    if (queryFilter == null) {
                        throw new IdMUnitException("No '_id' or 'userName' specified for the user");
                    }
                    url += "?_queryFilter=" + queryFilter + "&" + fields;
                    log.info("...performing validation for " + objectType + " with queryFilter of [" + queryFilter + "]");
                }
            }
            RestClient.Response response = rest.executeGet(url);
            JsonObject searchResults = new JsonParser().parse(response.messageBody).getAsJsonObject();
            searchResults.getAsJsonPrimitive("resultCount");
            if (searchResults.get("resultCount") == null) {
                log.error("An error occurred searching for object to validate. '" + url + "' response statusCode '" + response.statusCode + "' message body '" + response.messageBody + "'");
                throw new IdMUnitException("Error searching for object to validate");
            }

            if (searchResults.get("resultCount").getAsInt() == 0) {
                log.info("No objects returned from search to validate '" + url + "'");
                throw new IdMUnitException("No objects returned from search to validate");
            }

            if (searchResults.get("resultCount").getAsInt() != 1) {
                log.info("More than one object was returned from search to validate '" + url + "'");
                throw new IdMUnitException("More than one object was returned from search to validate");
            }

            actualOutput = searchResults.getAsJsonArray("result").get(0).getAsJsonObject();
        }


        /*
        {"_id":"joe","_rev":"1","mail":"joe@example.com","sn":"smith","passwordAttempts":"0","lastPasswordAttempt":"Thu Oct 09 2014 15:22:57 GMT-0600 (MDT)","address2":"","givenName":"joe","effectiveRoles":["openidm-authorized"],"country":"","city":"","lastPasswordSet":"","postalCode":"","description":"My first user","accountStatus":"active","telephoneNumber":"555-123-1234","roles":["openidm-authorized"],"effectiveAssignments":{},"postalAddress":"","userName":"joe","stateProvince":""}
         */

        doAttrValidation(actualOutput, attrs);
    }

    static JsonObject mapToJsonObject(Map<String, Collection<String>> attrs) throws IdMUnitException {
        Map<String, List<String>> newAttrs = new LinkedHashMap<String, List<String>>();
        for (String name : attrs.keySet()) {
            newAttrs.put(name, new ArrayList<String>(attrs.get(name)));
        }
        return mapToJsonObject("", newAttrs);
    }

    private static JsonObject mapToJsonObject(String baseName, Map<String, List<String>> attrs) throws IdMUnitException {
        JsonObject result = new JsonObject();
        HashMap<String, Map<String, List<String>>> subAttrs = new LinkedHashMap<String, Map<String, List<String>>>();
        for (Iterator<String> i = attrs.keySet().iterator(); i.hasNext(); ) {
            String expectedAttrName = i.next();
            if (expectedAttrName.contains(".")) {
                String[] nameParts = expectedAttrName.split("\\.", 2);
                Map<String, List<String>> subValues = subAttrs.get(nameParts[0]);
                if (subValues == null) {
                    subValues = new LinkedHashMap<String, List<String>>();
                    subAttrs.put(nameParts[0], subValues);
                }
                subValues.put(nameParts[1], attrs.get(expectedAttrName));
                i.remove();
            } else {
                if (expectedAttrName.contains("::")) {
                    String[] attr = expectedAttrName.split("::");
                    List<String> values = attrs.get(expectedAttrName);
                    if (values == null) {
                        result.addProperty(attr[0], "");
                    } else if (values.size() == 0) {
                        i.remove();
                        result.addProperty(attr[0], "");
                    } else if (values.size() == 1) {
                        String value = values.remove(0);
                        if ("[EMPTY]".equals(value)) {
                            result.addProperty(attr[0], "");
                        } else if ("true".equals(value)) {
                            result.addProperty(attr[0], true);
                        } else if ("false".equals(value)) {
                            result.addProperty(attr[0], false);
                        } else {
                            result.addProperty(attr[0], value);
                        }
                    }
                } else if (expectedAttrName.endsWith("[]")) {
                    Collection<String> values = attrs.get(expectedAttrName);
                    i.remove();
                    JsonArray v = new JsonArray();
                    if (values.size() == 1) {
                        String value = values.iterator().next();
                        if (!"[EMPTY]".equals(value)) {
                            v.add(new JsonPrimitive(value));
                        }
                    } else {
                        for (String value : values) {
                            v.add(new JsonPrimitive(value));
                        }
                    }
                    result.add(expectedAttrName.substring(0, expectedAttrName.length() - "[]".length()), v);
                } else {
                    List<String> values = attrs.get(expectedAttrName);
                    if (values == null) {
                        result.addProperty(expectedAttrName, "");
                    } else if (values.size() == 0) {
                        i.remove();
                        result.addProperty(expectedAttrName, "");
                    } else if (values.size() == 1) {
                        String value = values.remove(0);
                        if ("[EMPTY]".equals(value)) {
                            result.addProperty(expectedAttrName, "");
                        } else {
                            result.addProperty(expectedAttrName, value);
                        }
                    } else {
                        throw new IdMUnitException(String.format("'%s' attribute has multiple values specified but it is not an array. Either add the array specifier '[]' to the attribute name or remove the extra values", expectedAttrName));
                    }
                }
            }
        }

        for (String attrName : subAttrs.keySet()) {
            Map<String, List<String>> values = subAttrs.get(attrName);
            if (attrName.endsWith("[]")) {
                result.add(attrName.substring(0, attrName.length() - "[]".length()), mapToJsonArray(baseName + "." + attrName, values));
            } else {
                result.add(attrName, mapToJsonObject(baseName + "." + attrName, values));
            }
        }
        return result;
    }

    private static JsonArray mapToJsonArray(String attrName, Map<String, List<String>> subAttrs) throws IdMUnitException {
        JsonArray result = new JsonArray();

        while (subAttrs.size() > 0) {
            JsonObject newObj = new JsonObject();
            for (Iterator<String> i = subAttrs.keySet().iterator(); i.hasNext(); ) {
                String name = i.next();
                List<String> values = subAttrs.get(name);
                String[] nameParts = name.split("\\.");
                JsonObject currentObj = newObj;
                for (int j = 0; j < nameParts.length - 1; ++j) {
                    JsonElement o = currentObj.get(nameParts[j]);
                    if (o == null) {
                        o = new JsonObject();
                        currentObj.add(nameParts[j], o);
                    } else if (!o.isJsonObject()) {
                        throw new IdMUnitException("Attribute " + attrName + "." + nameParts[j] + " cannot be used as a primative and object");
                    }
                    currentObj = o.getAsJsonObject();
                }
                currentObj.addProperty(nameParts[nameParts.length - 1], values.remove(0));
                if (values.size() == 0) {
                    i.remove();
                }
            }
            result.add(newObj);
        }

        return result;
    }

    static List<String> jsonMatches(JsonElement expected, JsonElement actual) {
        List<String> differences = new ArrayList<String>();
        jsonMatches(differences, "", expected, actual);
        return differences;
    }

    static List<String> jsonExactMatches(JsonElement expected, JsonElement actual) {
        List<String> differences = new ArrayList<String>();
        jsonExactMatches(differences, "", expected, actual);
        return differences;
    }

    static void jsonMatches(List<String> differences, String baseName, JsonElement expected, JsonElement actual) {
        Gson gson = new Gson();

        if (expected.isJsonPrimitive()) {
            // Java null means the attribute is missing
            if (actual == null || actual.isJsonNull()) {
                if (!expected.getAsString().isEmpty()) {
                    differences.add(String.format("'%s' attribute mismatch: expected %s but was null", baseName, gson.toJson(expected)));
                }
                return;
            }

            if (!actual.isJsonPrimitive() || !actual.getAsString().matches(expected.getAsString())) {
                differences.add(String.format("'%s' attribute mismatch: expected %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
            }
            return;
        }

        if (expected.isJsonArray()) {
            if (actual == null || actual.isJsonNull()) {
                if (expected.getAsJsonArray().size() != 0) {
                    differences.add(String.format("'%s' attribute mismatch: expected %s but was null", baseName, gson.toJson(expected)));
                }
                return;
            }

            if (!actual.isJsonArray()) {
                differences.add(String.format("'%s' attribute mismatch: expected an array %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
                return;
            }

            // Validating an array is a "contains" operation, it ignores ordering of the items in the array
            Set<JsonElement> actualItems = new LinkedHashSet<JsonElement>();
            for (JsonElement item : actual.getAsJsonArray()) {
                actualItems.add(item);
            }
            for (JsonElement expectedItem : expected.getAsJsonArray()) {
                int actualItemSize = actualItems.size();
                for (Iterator<JsonElement> i = actualItems.iterator(); i.hasNext(); ) {
                    JsonElement actualItem = i.next();
                    List<String> d = new ArrayList<String>();
                    jsonMatches(d, baseName + "[]", expectedItem, actualItem);
                    if (d.size() == 0) {
                        i.remove();
                        break;
                    }
                }
                if (actualItems.size() == actualItemSize) {
                    differences.add(String.format("'%s' attribute mismatch: expected item %s was not found in %s", baseName, gson.toJson(expectedItem), gson.toJson(actual)));
                }
            }
            return;
        }

        if (expected.isJsonObject()) {
            if (!actual.isJsonObject()) {
                differences.add(String.format("'%s' attribute mismatch: expected an object %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
                return;
            }
            JsonObject actualObj = actual.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : expected.getAsJsonObject().entrySet()) {
                String name = entry.getKey();
                JsonElement expectedValue = entry.getValue();
                JsonElement actualValue = actualObj.get(name);
                jsonMatches(differences, baseName + "." + name, expectedValue, actualValue);
            }
        }
    }

    static void jsonExactMatches(List<String> differences, String baseName, JsonElement expected, JsonElement actual) {
        Gson gson = new Gson();

        if (expected.isJsonPrimitive()) {
            // Java null means the attribute is missing
            if (actual == null || actual.isJsonNull()) {
                if (!expected.getAsString().isEmpty()) {
                    differences.add(String.format("'%s' attribute mismatch: expected %s but was null", baseName, gson.toJson(expected)));
                }
                return;
            }

            if (!actual.isJsonPrimitive() || !actual.getAsString().matches(expected.getAsString())) {
                differences.add(String.format("'%s' attribute mismatch: expected %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
            }
            return;
        }

        if (expected.isJsonArray()) {
            if (actual == null || actual.isJsonNull()) {
                if (expected.getAsJsonArray().size() != 0) {
                    differences.add(String.format("'%s' attribute mismatch: expected %s but was null", baseName, gson.toJson(expected)));
                }
                return;
            }

            if (!actual.isJsonArray()) {
                differences.add(String.format("'%s' attribute mismatch: expected an array %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
                return;
            }

            if (actual.getAsJsonArray().size() != expected.getAsJsonArray().size()) {
                differences.add(String.format("'%s' attribute mismatch: actual item contains %s values when our expected item contains %s values. \nExpected values: %s \nActual values: %s ", baseName, actual.getAsJsonArray().size(), expected.getAsJsonArray().size(), gson.toJson(expected), gson.toJson(actual)));
            }

            // Validating an array is a "contains" operation, it ignores ordering of the items in the array
            Set<JsonElement> actualItems = new LinkedHashSet<JsonElement>();
            for (JsonElement item : actual.getAsJsonArray()) {
                actualItems.add(item);
            }
            for (JsonElement expectedItem : expected.getAsJsonArray()) {
                int actualItemSize = actualItems.size();
                for (Iterator<JsonElement> i = actualItems.iterator(); i.hasNext(); ) {
                    JsonElement actualItem = i.next();
                    List<String> d = new ArrayList<String>();
                    jsonExactMatches(d, baseName + "[]", expectedItem, actualItem);
                    if (d.size() == 0) {
                        i.remove();
                        break;
                    }
                }
                if (actualItems.size() == actualItemSize) {
                    differences.add(String.format("'%s' attribute mismatch: expected item %s was not found in %s", baseName, gson.toJson(expectedItem), gson.toJson(actual)));
                }
            }
            return;
        }

        if (expected.isJsonObject()) {
            if (!actual.isJsonObject()) {
                differences.add(String.format("'%s' attribute mismatch: expected an object %s but was %s", baseName, gson.toJson(expected), gson.toJson(actual)));
                return;
            }
            JsonObject actualObj = actual.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : expected.getAsJsonObject().entrySet()) {
                String name = entry.getKey();
                JsonElement expectedValue = entry.getValue();
                JsonElement actualValue = actualObj.get(name);
                jsonExactMatches(differences, baseName + "." + name, expectedValue, actualValue);
            }
        }
    }

    // Package access method to facilitate testing
    void doAttrValidation(JsonObject actualOutput, Map<String, Collection<String>> attrs) throws IdMUnitException {
        JsonObject expectedOutput = mapToJsonObject(attrs);
        List<String> failures = jsonMatches(expectedOutput, actualOutput);

        if (failures.size() > 0) {
            StringBuilder sbFailures = new StringBuilder();
            for (String failure : failures) {
                sbFailures.append(failure).append("\n");
            }
            throw new IdMUnitFailureException(sbFailures.toString());
        }
    }

    void doAttrExactValidation(JsonObject actualOutput, Map<String, Collection<String>> attrs) throws IdMUnitException {
        JsonObject expectedOutput = mapToJsonObject(attrs);
        List<String> failures = jsonExactMatches(expectedOutput, actualOutput);

        if (failures.size() > 0) {
            StringBuilder sbFailures = new StringBuilder();
            for (String failure : failures) {
                sbFailures.append(failure).append("\n");
            }
            throw new IdMUnitFailureException(sbFailures.toString());
        }
    }

    public void opValidateObjectDoesNotExist(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String objectType = ConnectorUtil.getSingleValue(attrs, "objectType");
        if (objectType == null) {
            throw new IdMUnitException("No object type provided");
        }
        attrs.remove("objectType");
        String userName = ConnectorUtil.getSingleValue(attrs, "userName");

        RestClient.Response response = rest.executeGet("/managed/" + objectType + "?_queryId=for-userName&uid=" + userName);

        JsonObject r = new JsonParser().parse(response.messageBody).getAsJsonObject();
        JsonElement objects = r.get("result");
        if (objects.isJsonArray() && objects.getAsJsonArray().size() > 0) {
            throw new IdMUnitFailureException("There is a user that exists with this username");
        }
    }

    public void opValidateObjectExact(Map<String, Collection<String>> attrs) throws IdMUnitException {
        attrs = new HashMap<String, Collection<String>>(attrs);
        String objectType = ConnectorUtil.getSingleValue(attrs, "objectType");
        if (objectType == null) {
            throw new IdMUnitException("No object type provided");
        }
        attrs.remove("objectType");
        String id = ConnectorUtil.getSingleValue(attrs, "_id");
        if (id == null) {
            String userName = ConnectorUtil.getSingleValue(attrs, "userName");
            if (userName == null) {
                throw new IdMUnitException("No '_id' or 'userName' specified for the user");
            }

            id = getIdFromUserName(userName);
        }

        RestClient.Response response = rest.executeGet("/managed/" + objectType + "/" + id + "?_fields=*,*_ref");

        /*
        {"_id":"joe","_rev":"1","mail":"joe@example.com","sn":"smith","passwordAttempts":"0","lastPasswordAttempt":"Thu Oct 09 2014 15:22:57 GMT-0600 (MDT)","address2":"","givenName":"joe","effectiveRoles":["openidm-authorized"],"country":"","city":"","lastPasswordSet":"","postalCode":"","description":"My first user","accountStatus":"active","telephoneNumber":"555-123-1234","roles":["openidm-authorized"],"effectiveAssignments":{},"postalAddress":"","userName":"joe","stateProvince":""}
         */

        JsonObject actualOutput = new JsonParser().parse(response.messageBody).getAsJsonObject();
        doAttrExactValidation(actualOutput, attrs);
    }

    private String getIdFromUserName(String userName) throws IdMUnitException {
        RestClient.Response response = rest.executeGet("/managed/user?_queryId=for-userName&uid=" + userName);
        // {"result":[{"userName":"tuser2","mail":"tuser@example.com","sn":"User","passwordAttempts":"0","lastPasswordAttempt":"Mon Oct 13 2014 15:42:27 GMT-0600 (MDT)","address2":"","givenName":"Test","effectiveRoles":["openidm-authorized"],"country":"","city":"","lastPasswordSet":"","postalCode":"","_id":"tuser2_id","_rev":"7","description":"My first user","accountStatus":"active","telephoneNumber":"555-555-1212","roles":["openidm-authorized"],"effectiveAssignments":null,"postalAddress":"","stateProvince":""}],"resultCount":1,"pagedResultsCookie":null,"remainingPagedResults":-1}

        JsonObject output = new JsonParser().parse(response.messageBody).getAsJsonObject();
        JsonElement result = output.get("result");
        if (result == null || !result.isJsonArray()) {
            throw new IdMUnitException("Field 'result' in missing or is not an array: " + response.messageBody);
        }

        if (result.getAsJsonArray().size() != 1) {
            throw new IdMUnitException("The 'result' array must contain one item: " + response.messageBody);
        }

        JsonElement user = result.getAsJsonArray().get(0);
        if (!user.isJsonObject()) {
            throw new IdMUnitException("The first item in the 'result' array is not an object: " + response.messageBody);
        }

        JsonElement id = user.getAsJsonObject().get("_id");
        if (id == null) {
            throw new IdMUnitException("'_id' is missing from the user results: " + response.messageBody);
        }

        return id.getAsString();
    }

    private String removeAttr(Map<String, Collection<String>> attrs, String attrName) throws IdMUnitException {
        Collection<String> values = attrs.remove(attrName);
        if (values == null) {
            return null;
        }

        if (values.size() > 1) {
            throw new IdMUnitException("Expected '" + attrName + "' to only have a single valued but it has " + values.size());
        }

        Iterator<String> i = values.iterator();
        if (i.hasNext()) {
            return i.next();
        } else {
            return null;
        }
    }

    private static class TrustAllX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
