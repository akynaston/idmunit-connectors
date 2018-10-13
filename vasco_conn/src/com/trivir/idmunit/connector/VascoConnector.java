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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.idmunit.Failures;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class VascoConnector extends AbstractConnector {
    protected static final String STR_SUCCESS = "...SUCCESS";
    static final String ATTR_URL = "url";
    static final String ATTR_REQUEST = "request";
    static final String ATTR_RESPONSE = "response";
    //Attributes names
    static final String USER_ID = "USERID";
    static final String DOMAIN = "DOMAIN";
    static final String LOCAL_AUTH = "LOCAL_AUTH";
    static final String BACKEND_AUTH = "BACKEND_AUTH";
    static final String DISABLED = "DISABLED";
    static final String LOCKED = "LOCKED";
    static final String CREATE_TIME = "CREATE_TIME";
    static final String MODIFY_TIME = "MODIFY_TIME";
    static final String HAS_DP = "HAS_DP";
    static final String OFFLINE_AUTH_ENABLED = "OFFLINE_AUTH_ENABLED";
    static final String MOBILE = "MOBILE";
    static final String DIGIPASS_SERIAL = "DIGIPASS_SERIAL";
    static final String ASSIGNED_DIGIPASS = "ASSIGNED_DIGIPASS";
    protected static final String WRONG_DIGIPASS_ATTR_NAME_ERROR_MSG = "Error: Please use " + ASSIGNED_DIGIPASS + " on a validation. " + DIGIPASS_SERIAL + " is only for assigning the digipass key.";
    //Digipass attributes
    static final String GRACE_PERIOD_DAYS = "GRACE_PERIOD_DAYS";
    static final String ASSIGNED_USER_ORG_UNIT = "ASSIGNED_USER_ORG_UNIT";
    //User attributes
    static final String ATTR_GROUP = "ATTR_GROUP";
    static final String NAME = "NAME";
    static final String USAGE_QUALIFIER = "USAGE_QUALIFIER";
    static final String VALUE = "VALUE";
    static final String OPTIONS = "OPTIONS";
    private static Marker fatal = MarkerFactory.getMarker("FATAL");
    private static Logger log = LoggerFactory.getLogger(VascoConnector.class);
    protected String server;
    protected String username;
    protected String password;
    protected String sessionId;
    Map<String, String> attrData = new TreeMap<String, String>();
    Map<String, String> auxAttrData = new TreeMap<String, String>();

    private static void validateResponse(Document responseDocument) throws XPathExpressionException, IdMUnitException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathExpression errorCodexPath = xPath.compile("//results/resultCodes/returnCode/node()");
        XPathExpression errorStatusCodeEnumxPath = xPath.compile("//results/resultCodes/statusCodeEnum/node()");
        XPathExpression errorMsgxPath = xPath.compile("//results/errorStack/errors/errorDesc/node()");
        XPathExpression faultStringxPath = xPath.compile("//Fault/faultstring/node()");

        String errorCode = (String)errorCodexPath.evaluate(responseDocument, XPathConstants.STRING);
        String errorEnumCode = (String)errorStatusCodeEnumxPath.evaluate(responseDocument, XPathConstants.STRING);
        String errorMsg = (String)errorMsgxPath.evaluate(responseDocument, XPathConstants.STRING);
        String faultString = (String)faultStringxPath.evaluate(responseDocument, XPathConstants.STRING);

        if (errorMsg == null || errorMsg.trim().equalsIgnoreCase("")) {
            errorMsg = faultString;
        }

        String fullErrorMsg = errorMsg;
        if (errorCode.length() > 0 && errorEnumCode.length() > 0) {
            fullErrorMsg = fullErrorMsg + " (Error Code: " + errorEnumCode + " " + errorCode + ")";
        }

        if (!"0".equalsIgnoreCase(errorCode) || faultString.length() > 0) {
            log.warn(fullErrorMsg);
            throw new IdMUnitException(fullErrorMsg);
        }
    }

    private static void validateDeletedResponse(Document responseDocument) throws XPathExpressionException, IdMUnitException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathExpression errorCodexPath = xPath.compile("//results/resultCodes/returnCode/node()");
        XPathExpression errorMsgxPath = xPath.compile("//results/errorStack/errors/errorDesc/node()");
        String errorCode = (String)errorCodexPath.evaluate(responseDocument, XPathConstants.STRING);
        String errorMsg = (String)errorMsgxPath.evaluate(responseDocument, XPathConstants.STRING);
        String fullErrorMsg = errorMsg + " (Error Code: " + errorCode + ")";
        if (!"0".equalsIgnoreCase(errorCode)) {
            log.warn(fullErrorMsg);
        } else {
            log.info("The user has been deleted.");
        }
    }

    private static Document createDocument(String xmlString) throws IdMUnitException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputSource source = new InputSource(new StringReader(xmlString));
        try {
            return factory.newDocumentBuilder().parse(source);
        } catch (ParserConfigurationException e) {
            throw new IdMUnitException("Error parsing API response.", e);
        } catch (IOException e) {
            throw new IdMUnitException("Error parsing API response.", e);
        } catch (SAXException e) {
            throw new IdMUnitException("Error parsing API response.", e);
        }
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        server = config.get(BasicConnector.CONFIG_SERVER);
        username = config.get(BasicConnector.CONFIG_USER);
        password = config.get(BasicConnector.CONFIG_PASSWORD);

        if (server == null || username == null || password == null) {
            Failures failures = new Failures();
            if (server == null) {
                failures.add("\"<server>\"");
            }
            if (username == null) {
                failures.add("\"<username>\"");
            }
            if (password == null) {
                failures.add("\"<password>\"");
            }
            if (failures.hasFailures()) {
                String errorMsg = "Required parameter(s) are missing from the idmunit-config:\n" + failures.toString();
                log.error(fatal, errorMsg);
                throw new IdMUnitException(errorMsg);
            }
        }

        final String url = server;
        final String request = String.format(VascoSoap.LOGON_REQUEST, username, password);
        Document responseDocument = getResponse(url, request);
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression sessionIdXpath = xPath.compile("//results/resultAttribute/attributes/attributeID[text()=\"CREDFLD_SESSION_ID\"]/parent::node()/value");
            Node sessionIdNode = (Node)sessionIdXpath.evaluate(responseDocument, XPathConstants.NODE);
            validateResponse(responseDocument);
            sessionId = sessionIdNode != null ? sessionIdNode.getTextContent() : null;
        } catch (XPathExpressionException e) {
            log.error(fatal, "The logon document returned did not contain CREDFLD_SESSION_ID.");
            throw new IdMUnitException("The logon document returned did not contain CREDFLD_SESSION_ID.", e);
        }

        log.info("Connection Successful");
        log.debug("The conneciton was established with this connection informaiton:");
        log.debug("Server:    " + server);
        log.debug("Username:  " + username);
        log.debug("Password:  " + password);
        log.debug("SessionId: " + sessionId);

    }

    public void tearDown() throws IdMUnitException {
        final String url = server;
        final String request = String.format(VascoSoap.LOGOFF_REQUEST, sessionId);

        Document response = getResponse(url, request);
        try {
            validateResponse(response);
        } catch (XPathExpressionException e) {
            String errorMsg = "The logoff request failed.";
            log.warn(errorMsg);
            throw new IdMUnitException(errorMsg, e);
        }
    }

    public void opCreateUser(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException, XPathExpressionException {
        HashSet<String> requiredAttrs = new HashSet<String>();
        requiredAttrs.add(USER_ID);
        requiredAttrs.add(DOMAIN);
        requiredAttrs.add(LOCAL_AUTH);
        requiredAttrs.add(BACKEND_AUTH);
        requiredAttrs.add(DISABLED);
        requiredAttrs.add(LOCKED);
        validateRequiredAttrsExist(requiredAttrs, expectedAttrs);

        //TODO allow for any attr to be set on create.
        StringBuilder xmlBodySb = new StringBuilder("");
        for (String expectedAttrName : expectedAttrs.keySet()) {
            if (expectedAttrName.equalsIgnoreCase(DIGIPASS_SERIAL)) {
                throw new IdMUnitException("ERROR: Please use the AssignDigipass operation to assign a DIGIPASS_SERIAL.");
            }
            if (expectedAttrName.startsWith(VascoSoap.USER_ATTR_PREFIX)) {
                continue;
            }

            String type;
            String expectedValueForXML = expectedAttrs.get(expectedAttrName).toArray()[0].toString();
            if (expectedAttrName.equalsIgnoreCase(DISABLED) ||
                    expectedAttrName.equalsIgnoreCase(LOCKED)) {
                type = "boolean";
                expectedValueForXML = expectedAttrs.get(expectedAttrName).toArray()[0].toString().toLowerCase();
            } else if (expectedAttrName.equalsIgnoreCase(OPTIONS)) {
                type = "unsignedInt";
            } else {
                type = "string";
            }

            xmlBodySb.append(
                    "<attributes> " +
                            "<value xsi:type=\"xsd:" + type + "\">" + expectedValueForXML + "</value> " +
                            "<attributeID>" + VascoSoap.USER_EXECUTE_PREFIX + expectedAttrName + "</attributeID> " +
                            "</attributes> ");
        }

        final String request = String.format(
                VascoSoap.CREATE_USER_REQUEST_NEW,
                sessionId,
                xmlBodySb.toString());
        Document response = getResponse(server, request);
        validateResponse(response);

        log.info("The user has been created.");

        //Do the work to add userAttributes (Like Radius Attributes)
        List<Map<String, Collection<String>>> userAttributeList = getUserAttributes(expectedAttrs);
        for (Map<String, Collection<String>> ua : userAttributeList) { //Iterate over the userAttributes, validate them, and set them.
            HashSet<String> requiredAttrsForUserAttributeAdd = new HashSet<String>();
            requiredAttrs.add(USER_ID);
            requiredAttrs.add(DOMAIN);
            requiredAttrs.add(ATTR_GROUP);
            requiredAttrs.add(NAME);
            requiredAttrs.add(USAGE_QUALIFIER);
            requiredAttrs.add(VALUE);
            validateRequiredAttrsExist(requiredAttrsForUserAttributeAdd, ua);

            final String requestForUserAttribute = String.format(
                    VascoSoap.CREATE_USER_ATTRIBUTE_REQUEST,
                    sessionId,
                    expectedAttrs.get(USER_ID).toArray()[0],
                    expectedAttrs.get(DOMAIN).toArray()[0],
                    ua.get(ATTR_GROUP).toArray()[0],
                    ua.get(NAME).toArray()[0],
                    ua.get(USAGE_QUALIFIER).toArray()[0],
                    ua.get(VALUE).toArray()[0],
                    ua.get(OPTIONS).toArray()[0]);
            Document userAttributeResponse = getResponse(server, requestForUserAttribute);
            validateResponse(userAttributeResponse);
            log.info("User attribute " + ua.get(NAME) + " has been added to " + expectedAttrs.get(USER_ID));
        } //end for each radius attr.
    } //opCreateUser

    public void opDeleteUser(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException, XPathExpressionException {
        HashSet<String> requiredAttrs = new HashSet<String>();
        requiredAttrs.add(USER_ID);
        requiredAttrs.add(DOMAIN);
        validateRequiredAttrsExist(requiredAttrs, expectedAttrs);

        final String request = String.format(
                VascoSoap.DELETE_USER_REQUST,
                sessionId,
                expectedAttrs.get(USER_ID).toArray()[0],
                expectedAttrs.get(DOMAIN).toArray()[0]);
        Document response = getResponse(server, request);
        validateDeletedResponse(response);
    }

    public void opValidateUser(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException, XPathExpressionException {
        log.info("Validating the account attributes...");
        Failures failures = new Failures();
        HashSet<String> requiredAttrs = new HashSet<String>();
        requiredAttrs.add(USER_ID);
        requiredAttrs.add(DOMAIN);
        validateRequiredAttrsExist(requiredAttrs, expectedAttrs);

        final String request = String.format(
                VascoSoap.VIEW_USER_REQUEST,
                sessionId,
                expectedAttrs.get(USER_ID).toArray()[0],
                expectedAttrs.get(DOMAIN).toArray()[0]);
        Document responseDocument = getResponse(server, request);

        log.debug("AVK: response document was: [");
        log.debug(responseDocument.toString());

        StringWriter outputString = new StringWriter();

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        DOMSource source = new DOMSource(responseDocument);
        StreamResult result = new StreamResult(outputString);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        //System.out.println("========================================================================================================================================");
        //System.out.println("Test: [" + testName + "]");
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.debug(outputString.toString());

        log.debug("]");

        validateResponse(responseDocument);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        //Check the account attributes
        for (String expectedAttrName : expectedAttrs.keySet()) {
            if (expectedAttrName.contains(VascoSoap.USER_ATTR_PREFIX)) {
                continue;
            }
            if (expectedAttrName.equalsIgnoreCase(DIGIPASS_SERIAL)) {
                throw new IdMUnitException(WRONG_DIGIPASS_ATTR_NAME_ERROR_MSG);
            }

            String vascoAttrName = VascoSoap.USER_EXECUTE_PREFIX + expectedAttrName;

            String expectedAttrValue = expectedAttrs.get(expectedAttrName).toArray()[0].toString();
            String xPathToAttr = "//results/resultAttribute/attributes/attributeID[text()=\"" + vascoAttrName + "\"]/parent::node()/value/text()";
            log.debug("xPATH to validate this account attribute: " + xPathToAttr);
            XPathExpression errorCodexPath = xPath.compile(xPathToAttr);
            String actualAttrValue = (String)errorCodexPath.evaluate(responseDocument, XPathConstants.STRING);
            Collection<String> currentAttrCollection = new ArrayList<String>(Arrays.asList(new String[]{actualAttrValue}));

            //Handle digipass ids. Needed since we don't know the order of the returned digipass Ids.
            if (expectedAttrName.equalsIgnoreCase(ASSIGNED_DIGIPASS)) {

                String splitter = ","; //This isn't a multi valued attr, so don't use what IdMUnitis using.

                List<String> expectedDigipassIDsList = Arrays.asList(expectedAttrValue.split(splitter));
                List<String> actualDigipassIDsList = Arrays.asList(actualAttrValue.split(splitter));

                Collections.sort(expectedDigipassIDsList);
                Collections.sort(actualDigipassIDsList);

                StringBuilder expectedDigiPassSB = new StringBuilder("");
                StringBuilder actualDigiPassSB = new StringBuilder("");

                for (String s : expectedDigipassIDsList) {
                    expectedDigiPassSB.append(s);
                    expectedDigiPassSB.append(splitter);
                }
                expectedDigiPassSB.deleteCharAt(expectedDigiPassSB.length() - 1);
                for (String s : actualDigipassIDsList) {
                    actualDigiPassSB.append(s);
                    actualDigiPassSB.append(splitter);
                }
                actualDigiPassSB.deleteCharAt(actualDigiPassSB.length() - 1);

                expectedAttrValue = expectedDigiPassSB.toString();
                actualAttrValue = actualDigiPassSB.toString();
            }

            if ("DISABLED".equalsIgnoreCase(expectedAttrName) || "LOCKED".equalsIgnoreCase(expectedAttrName)) {
                expectedAttrValue = expectedAttrValue.toLowerCase();
            }


            //Compare the account attributes
            if (actualAttrValue == null || actualAttrValue.trim().length() == 0) { //If null or empty, fail.
                failures.add("Validation failed for account attribute [" + expectedAttrName + "] expected value: [" + expectedAttrValue + "] but the attribute value did not exist in the application.");

            } else if (!actualAttrValue.matches(expectedAttrValue)) { //If the actual attr does not match the expected, fail.
                failures.add("Validation failed for account attribute [" + expectedAttrName + "]", expectedAttrs.get(expectedAttrName), currentAttrCollection);
            } else { //Log the success.
                log.info(STR_SUCCESS + ": validating account attribute: [" + expectedAttrName + "] EXPECTED: [" + expectedAttrValue + "] ACTUAL: [" + actualAttrValue + "]");
            }
        }

        List<Map<String, Collection<String>>> userAttributeList = getUserAttributes(expectedAttrs);
        if (userAttributeList.size() > 0) {
            log.info("...Validating the user attributes...");
        } else {
            log.info("...No user attributes to validate.");
        }
        for (Map<String, Collection<String>> ua : userAttributeList) { //Iterate over the userAttributes, validate them, and set them.
            //Sending the request for which attrs we want to validate.
            StringBuilder sb = new StringBuilder();
            for (String propertyName : ua.keySet()) {
                sb.append("<attributeID>UATTFLD_");
                sb.append(propertyName);
                sb.append("</attributeID>\n");
            }

            final String requestForUserAttribute = String.format(
                    VascoSoap.USER_ATTRIBUTE_QUERY_REQUEST,
                    sessionId,
                    expectedAttrs.get(USER_ID).toArray()[0],
                    expectedAttrs.get(DOMAIN).toArray()[0],
                    sb.toString());
            Document userAttributeResponse = getResponse(server, requestForUserAttribute);
            validateResponse(userAttributeResponse);
            for (String propertyName : ua.keySet()) {
                String userAttributeName = (String)ua.get(NAME).toArray()[0];
                String expectedPropertyValue = (String)ua.get(propertyName).toArray()[0];

                String xPathToAttrName = "//results/resultAttribute/attributeList/attributes/value[text()=\"" + userAttributeName + "\"]";
                XPathExpression attrNameXpathExpression = xPath.compile(xPathToAttrName);
                String temp = (String)attrNameXpathExpression.evaluate(userAttributeResponse, XPathConstants.STRING);

                if (temp == null || "".equalsIgnoreCase(temp)) {
                    failures.add("Validation failed for the user attribute [" + VascoSoap.USER_ATTR_PREFIX + userAttributeName + "]. The user attribute did not exist in the application.");
                    break;
                }


                if (propertyName.equalsIgnoreCase(NAME)) {
                    continue;
                }

                String xPathToPropertyValue = "//results/resultAttribute/attributeList/attributes/value[text()=\"" + userAttributeName + "\"]/parent::node()/parent::node()/attributes/attributeID[text()=\"" + VascoSoap.ATTR_EXECUTE_PREFIX + propertyName + "\"]/parent::node()/value";
                log.debug("xPATH to validate this user attribute: " + xPathToPropertyValue);

                XPathExpression xPathEx = xPath.compile(xPathToPropertyValue);
                String actualPropertyValue = (String)xPathEx.evaluate(userAttributeResponse, XPathConstants.STRING);

                Collection<String> actualAttrCollection = new ArrayList<String>(Arrays.asList(new String[]{actualPropertyValue}));
                Collection<String> expectedAttrCollection = new ArrayList<String>(Arrays.asList(new String[]{expectedPropertyValue}));
                if (actualPropertyValue == null || actualPropertyValue.trim().length() == 0) { //If null or empty, fail.
                    failures.add("Validation failed for the user attribute [" + VascoSoap.USER_ATTR_PREFIX + userAttributeName + " -> " + propertyName + "] expected value: [" + expectedPropertyValue + "] but the attribute value did not exist in the application.");
                } else if (!actualPropertyValue.matches(expectedPropertyValue)) { //If the actual attr does not match the expected, fail.
                    failures.add("Validation failed for the user attribute [" + VascoSoap.USER_ATTR_PREFIX + userAttributeName + " -> " + propertyName + "]", expectedAttrCollection, actualAttrCollection);
                } else { //Log the success.
                    log.info(STR_SUCCESS + ": validating the user attribute: [" + VascoSoap.USER_ATTR_PREFIX + userAttributeName + " -> " + propertyName + "] EXPECTED: [" + expectedPropertyValue + "] ACTUAL: [" + actualPropertyValue + "]");
                }
            } //end each property
        }

        if (failures.hasFailures()) {
            throw new IdMUnitFailureException(failures.toString());
        } else {
            log.info("The user has been validated.");
        }
    }

    public void opAssignDigipass(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException, XPathExpressionException {
        HashSet<String> requiredAttrs = new HashSet<String>();
        requiredAttrs.add(DIGIPASS_SERIAL);
        requiredAttrs.add(DOMAIN);
        requiredAttrs.add(USER_ID);
        requiredAttrs.add(GRACE_PERIOD_DAYS);
        validateRequiredAttrsExist(requiredAttrs, expectedAttrs);

        String assignedUserOrg = "";

        //Handle the optional ASSIGNED_USER_ORG_UNIT attribute.
        if (expectedAttrs.containsKey(ASSIGNED_USER_ORG_UNIT)) {
            assignedUserOrg =
                    "<attributes> " +
                            "<value xsi:type=\"xsd:string\">" + (String)expectedAttrs.get(ASSIGNED_USER_ORG_UNIT).toArray()[0] + "</value> " +
                            "<attributeID>DIGIPASSFLD_ASSIGNED_USER_ORG_UNIT</attributeID> " +
                            "</attributes> ";
        }

        String[] digipassValues = ((String)expectedAttrs.get(DIGIPASS_SERIAL).toArray()[0]).split(",");
        for (String s : digipassValues) {
            final String request = String.format(
                    VascoSoap.ASSIGN_DIGIPASS_REQUEST,
                    sessionId,
                    s,
                    expectedAttrs.get(DOMAIN).toArray()[0],
                    expectedAttrs.get(USER_ID).toArray()[0],
                    expectedAttrs.get(GRACE_PERIOD_DAYS).toArray()[0],
                    assignedUserOrg);
            Document responseDocument = getResponse(server, request);


            validateResponse(responseDocument);
            log.info("Digipass [" + s + "] assigned to [" + expectedAttrs.get(USER_ID).toArray()[0] + "].");
        }
    } //End opAssignDigipass

    public void opUnAssignDigipass(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException, XPathExpressionException {
        HashSet<String> requiredAttrs = new HashSet<String>();
        requiredAttrs.add(DIGIPASS_SERIAL);
        validateRequiredAttrsExist(requiredAttrs, expectedAttrs);

        String[] digipassValues = ((String)expectedAttrs.get(DIGIPASS_SERIAL).toArray()[0]).split(",");
        for (String s : digipassValues) {
            final String request = String.format(
                    VascoSoap.UNASSIGN_DIGIPASS_REQUEST,
                    sessionId,
                    s);
            Document responseDocument = getResponse(server, request);

            validateResponse(responseDocument);
            String msg = "";

            if (expectedAttrs.get(USER_ID) != null) {
                msg = "Digipass [" + s + "] unassigned from [" + expectedAttrs.get(USER_ID).toArray()[0] + "].";
            } else {
                msg = "Digipass [" + s + "] unassigned.";
            }
            log.info(msg);
        }
    } //End opAssignDigipass

    public void opModifyUser(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException, XPathExpressionException {
        log.info("Modifying the account attributes...");

        if (expectedAttrs.containsKey(DIGIPASS_SERIAL)) {
            throw new IdMUnitException("Invalid attribute for this operation: " + DIGIPASS_SERIAL + ". This can only be modified with the AssignDigipass and UnAssignDigipass operations.");
        }

        //Make sure the user exists before continuing. Otherwise the update will create the user.
        Map<String, Collection<String>> userToValidate = new HashMap<String, Collection<String>>();
        userToValidate.put(USER_ID, expectedAttrs.get(USER_ID));
        userToValidate.put(DOMAIN, expectedAttrs.get(DOMAIN));
        opValidateUser(userToValidate);

        HashSet<String> requiredAttrs = new HashSet<String>();
        requiredAttrs.add(USER_ID);
        requiredAttrs.add(DOMAIN);
        validateRequiredAttrsExist(requiredAttrs, expectedAttrs);

        //Create the request to modify the account attributes
        StringBuilder xmlBodySb = new StringBuilder("");
        for (String c : expectedAttrs.keySet()) {
            if (c.startsWith(VascoSoap.USER_ATTR_PREFIX)) {
                throw new IdMUnitException("ERROR. Can't modify any user attributes. (" + c + ")");
            }

            String type = "string";
            if (c.equalsIgnoreCase(DISABLED) ||
                    c.equalsIgnoreCase(LOCKED)) {
                type = "boolean";
            }
            xmlBodySb.append(
                    "<attributes> " +
                            "<value xsi:type=\"xsd:" + type + "\">" + expectedAttrs.get(c).toArray()[0] + "</value> " +
                            "<attributeID>" + VascoSoap.USER_EXECUTE_PREFIX + c + "</attributeID> " +
                            "</attributes> ");
        }
        final String request = String.format(
                VascoSoap.USER_UPDATE_REQUEST,
                sessionId,
                expectedAttrs.get(USER_ID).toArray()[0],
                expectedAttrs.get(DOMAIN).toArray()[0],
                xmlBodySb.toString());
        Document responseDocument = getResponse(server, request);
        validateResponse(responseDocument);

    } //opModifyUser

    public void opValidateCon(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException, XPathExpressionException {
        //Used simply to validate a conneciton can be made.
    }

    public List<Map<String, Collection<String>>> getUserAttributes(Map<String, Collection<String>> expectedAttrs) {
        List<Map<String, Collection<String>>> currentUserAttrs = new ArrayList<Map<String, Collection<String>>>();
        for (String attrName : expectedAttrs.keySet()) {
            Map<String, Collection<String>> userAttrProperties = new HashMap<String, Collection<String>>();
            if (!attrName.startsWith(VascoSoap.USER_ATTR_PREFIX)) {
                continue; //Stop here if this isn't a user attribute
            }
            String radiusAttrNameAndValues = (String)expectedAttrs.get(attrName).toArray()[0];
            String[] radiusAttrValues = radiusAttrNameAndValues.split(",");
            for (String s : radiusAttrValues) {
                s = s.trim();
                String[] attrValPair = s.split(":");
                userAttrProperties.put(attrValPair[0], Arrays.asList(attrValPair[1]));
            }
            userAttrProperties.put(NAME, Arrays.asList(attrName.replaceAll(VascoSoap.USER_ATTR_PREFIX, "")));
            if (userAttrProperties.get(OPTIONS) == null ||
                    (!userAttrProperties.get(OPTIONS).toArray()[0].toString().equalsIgnoreCase("1") &&
                            !userAttrProperties.get(OPTIONS).toArray()[0].toString().equalsIgnoreCase("true"))) {
                userAttrProperties.put(OPTIONS, new ArrayList<String>(Arrays.asList(new String[]{"0"})));
            } else {
                userAttrProperties.put(OPTIONS, new ArrayList<String>(Arrays.asList(new String[]{"1"})));
            }
            currentUserAttrs.add(userAttrProperties);
        } //end for
        return currentUserAttrs;
    } //end getUserAttributes

    public void validateRequiredAttrsExist(HashSet<String> requiredAttrs, Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        Failures failures = new Failures();

        for (String s : requiredAttrs) {
            if (expectedAttrs.get(s) == null) {
                failures.add(s);
            }
        }

        if (failures.hasFailures()) {
            throw new IdMUnitException("Missing required attribute(s):\n" + failures.toString());
        }
    }

    public Document getResponse(String url, String request) throws IdMUnitException {
        log.debug("VASCO Request: " + request);

        /*
        System.out.println();
        System.out.println();
        System.out.println("---------------------------------------------------------------");
        System.out.println("Request : " + request);
        // */

        if (url == null) {
            throw new IdMUnitException("The url is null. Verify that the connector setup ran.");
        }

        StringEntity requestEntity;

        try {
            requestEntity = new StringEntity(request, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IdMUnitException("Error building request entity.", e);
        }

        requestEntity.setContentType("text/xml");
        HttpClient client = buildHttpClient(url, username, password);
        HttpPost post = new HttpPost(url);
        post.setEntity(requestEntity);

        ResponseHandler<String> handler = new ResponseHandler<String>() {
            public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                if (httpResponse.getStatusLine().getStatusCode() == 401) {
                    throw new IOException("Authentication failed. Please check your authentication credentials.");
                }

                HttpEntity responseEntity = httpResponse.getEntity();
                if (responseEntity != null) {
                    String response = EntityUtils.toString(responseEntity);
                    log.debug("VASCO Response: " + response);
                    /*
                    System.out.println("Response: " + response);
                    System.out.println("---------------------------------------------------------------");
                    System.out.println();
                    System.out.println();
                    //*/

                    return response;
                } else {
                    return null;
                }
            }

        };

        String response;
        try {
            response = client.execute(post, handler);
        } catch (IOException e) {
            throw new IdMUnitException("Error sending request.", e);
        }
        log.debug("Response: " + response);


        Document document = null;
        try {
            document = createDocument(response);
        } catch (IdMUnitException e) {
            throw new IdMUnitException(e);
        }

        return document;
    }

    public HttpClient buildHttpClient(String urlString, String clientUsername, String clientPassword) throws IdMUnitException {
        String protocol = urlString.substring(0, 5).equalsIgnoreCase("https") ? "https" : "http";
        DefaultHttpClient httpClient = new DefaultHttpClient();

        if (clientUsername == null && clientPassword != null) {
            throw new IdMUnitException("SoapClientConnector username isn't setup correctly in the idmunit-config file.");
        }

        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            throw new IdMUnitException("Soap URL has bad syntax.", e);
        }

        if (clientUsername != null) {
            this.username = clientUsername.trim();
            this.password = clientPassword == null ? "" : clientPassword.trim();
            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_SCHEME),
                    new UsernamePasswordCredentials(clientUsername, clientPassword));
        }

        if ("http".equalsIgnoreCase(protocol)) {
            return httpClient; //If it isn't https, then we are done here.
        }

        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new IdMUnitException("SSLContext Instance doesn't exist", e);
        }

        X509TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        try {
            ctx.init(null, new TrustManager[]{tm}, null);
        } catch (KeyManagementException e) {
            throw new IdMUnitException("SSLContext failed to initialize.", e);
        }
        SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        ClientConnectionManager ccm = httpClient.getConnectionManager();
        SchemeRegistry sr = ccm.getSchemeRegistry();
        sr.register(new Scheme(protocol, uri.getPort(), ssf));

        return httpClient;
    }

    @SuppressWarnings("unused")
    private void printExpectedAttrs(Map<String, Collection<String>> expectedAttrs) {
        log.info("----- Test User Details -----");
        for (String attrName : expectedAttrs.keySet()) {
            String attrValue = (String)expectedAttrs.get(attrName).toArray()[0];
            log.info(attrName + " -> " + attrValue);
        }
        log.info("-----  END User Details -----");
    }

    @SuppressWarnings("unused")
    private void printUAExpectedAttrs(Map<String, Collection<String>> expectedAttrs) {
        System.out.println("----- Test User Details -----");
        for (String attrName : expectedAttrs.keySet()) {
            if (!attrName.startsWith(VascoSoap.USER_ATTR_PREFIX)) {
                continue;
            }
            String attrValue = (String)expectedAttrs.get(attrName).toArray()[0];
            System.out.println(attrName + " -> " + attrValue);
        }
        System.out.println("-----  END User Details -----");
    }
}
