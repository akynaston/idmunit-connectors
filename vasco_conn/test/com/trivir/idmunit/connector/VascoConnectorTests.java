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
import org.idmunit.IdMUnitFailureException;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;

public class VascoConnectorTests extends TestCase {
    //Attr names
    static final String USER_ID = VascoConnector.USER_ID;
    static final String DOMAIN = VascoConnector.DOMAIN;
    static final String CREATE_TIME = VascoConnector.CREATE_TIME;
    static final String MODIFY_TIME = VascoConnector.MODIFY_TIME;
    static final String HAS_DP = VascoConnector.HAS_DP;
    //static final String STATUS = VascoConnector.STATUS; //Identikey Server Documentation says this is reserved for future use. It is ALWAYS -1 and can't be changed.
    static final String LOCAL_AUTH = VascoConnector.LOCAL_AUTH;
    static final String BACKEND_AUTH = VascoConnector.BACKEND_AUTH;
    static final String DISABLED = VascoConnector.DISABLED;
    static final String LOCKED = VascoConnector.LOCKED;
    static final String OFFLINE_AUTH_ENABLED = VascoConnector.OFFLINE_AUTH_ENABLED;
    static final String MOBILE = VascoConnector.MOBILE;
    static final String DIGIPASS_SERIAL = VascoConnector.DIGIPASS_SERIAL;
    static final String ASSIGNED_DIGIPASS = VascoConnector.ASSIGNED_DIGIPASS;
    static final String GRACE_PERIOD = VascoConnector.GRACE_PERIOD_DAYS;
    //Test connection info
    static final String TEST_SERVER_URL = "https://10.34.176.78:8888";
    static final String TEST_SERVER_DOMAIN = "goldlnk.rootlnka.net";
    static final String TEST_ADMIN_USER = "external_user_sync";
    static final String TEST_ADMIN_PASS = "P@ssw0rd!@#";
    static final String EXPECTED_ERROR_SUCCESS = "Expected error message recieved.";
    static final String TEST_USER_NAME = "trivirtest";
    static final String TEST_USER_NAME3 = "trivirtest3";
    //    final static String TEST_DIGIPASS1 = "1470580016";
    static final String TEST_DIGIPASS1 = "1470580061";
    //static final String TEST_DIGIPASS2 = "1470580023";
    static final String TEST_DIGIPASS2 = "1470580078";
    //static final String TEST_DIGIPASS3 = "1470580030";
    //static final String TEST_DIGIPASS3 = "1470580092";
    static final String TEST_DIGIPASS3 = "1470580108";


    //////////  Below are the digipasses we are using in testing.
    /////////   If one is commented out it is because we lost the ability to assign that digipass.
    /////////   It says the digipass is not available, even though it is.
    static final String TEST_DIGIPASS4 = "1470580047";
    static final String TEST_DIGIPASS5 = "1470580054";
    //Test user data
    Map<String, Collection<String>> trivirUserData = new HashMap<String, Collection<String>>();
    Map<String, Collection<String>> trivirUserWithRadiusAttrs1 = new HashMap<String, Collection<String>>();
    private VascoConnector conn = null;
/*
x    1470580016
x    1470580023
x    1470580030
x    1470580047
x    1470580054
x    1470580061
x    1470580078
x    1470580085
x    1470580092
x    1470580108*/
    /*Failed to assign. Not sure why



     */

    protected void setUp() throws Exception {
        conn = new VascoConnector();

        Map<String, String> config = new TreeMap<String, String>();
        config.put("server", TEST_SERVER_URL);
        config.put("user", TEST_ADMIN_USER);
        config.put("password", TEST_ADMIN_PASS);

        trivirUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirUserData.put(LOCAL_AUTH, Arrays.asList("Default"));
        trivirUserData.put(BACKEND_AUTH, Arrays.asList("Default"));
        trivirUserData.put(DISABLED, Arrays.asList("false"));
        trivirUserData.put(LOCKED, Arrays.asList("false"));

        trivirUserWithRadiusAttrs1.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirUserWithRadiusAttrs1.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirUserWithRadiusAttrs1.put(HAS_DP, Arrays.asList("Unassigned"));
        trivirUserWithRadiusAttrs1.put(LOCAL_AUTH, Arrays.asList("Default"));
        trivirUserWithRadiusAttrs1.put(BACKEND_AUTH, Arrays.asList("Default"));
        trivirUserWithRadiusAttrs1.put(DISABLED, Arrays.asList("false"));
        trivirUserWithRadiusAttrs1.put(LOCKED, Arrays.asList("false"));
        trivirUserWithRadiusAttrs1.put(OFFLINE_AUTH_ENABLED, Arrays.asList("Default"));
        trivirUserWithRadiusAttrs1.put("ua:Callback-id", Arrays.asList(
                "ATTR_GROUP:External_Groupcaid, " +
                        "USAGE_QUALIFIER:REPLYcaId, " +
                        "VALUE:test@test.comcaId, " +
                        "OPTIONS:1"));
        trivirUserWithRadiusAttrs1.put("ua:Callback-id2", Arrays.asList(
                "ATTR_GROUP:External_Groupcaid2, " +
                        "USAGE_QUALIFIER:REPLYcaId2, " +
                        "VALUE:test@test.comcaId2, " +
                        "OPTIONS:false"));
        trivirUserWithRadiusAttrs1.put("ua:Radius-Example", Arrays.asList(
                "ATTR_GROUP:External_Group-RE, " +
                        "USAGE_QUALIFIER:REPLY-RE, " +
                        "VALUE:test@test.com-RE, " +
                        "OPTIONS:true"));
        trivirUserWithRadiusAttrs1.put("ua:NON-Radius-Example", Arrays.asList(
                "ATTR_GROUP:External_Group-NON-RE, " +
                        "USAGE_QUALIFIER:REPLY-NON-RE, " +
                        "VALUE:test@test.com-NON-RE"));

        conn.setup(config);
        conn.opDeleteUser(trivirUserData); //Delete the user if the user is still present.
    }

    protected void tearDown() throws Exception {
        conn.opDeleteUser(trivirUserData);
        conn.tearDown();
        conn = null;
    }

    public void testConnections() throws XPathExpressionException, IdMUnitException {
        conn.opValidateCon(trivirUserData);
    }

    public void testCreateUser() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);
    }

    public void testCreateUserWithUserAttributes() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserWithRadiusAttrs1);
    }

    public void testCreateDuplicateUser() throws IdMUnitException, XPathExpressionException {
        try {
            conn.opCreateUser(trivirUserData);
            conn.opCreateUser(trivirUserData);
        } catch (IdMUnitException e) {
            if (!e.getMessage().contains("object already exists")) {
                throw e; //Throw the exception if we didn't get the expected result.
            }
        }
    }

    public void testDeleteUserThatExists() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);
        conn.opDeleteUser(trivirUserData);
    }

    public void testDeleteUserThatDoesNotExist() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);
        conn.opDeleteUser(trivirUserData);
        conn.opDeleteUser(trivirUserData); //Just validate no exception is thrown when deleting something that isn't there.
    }

    public void testSuccessValidateUser() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);
        Map<String, Collection<String>> trivirTempUserData = new HashMap<String, Collection<String>>();
        trivirTempUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTempUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData.put(CREATE_TIME, Arrays.asList(".*"));
        trivirTempUserData.put(MODIFY_TIME, Arrays.asList(".*"));
        trivirTempUserData.put(HAS_DP, Arrays.asList("Unassigned"));
        trivirTempUserData.put(LOCAL_AUTH, Arrays.asList("Default"));
        trivirTempUserData.put(BACKEND_AUTH, Arrays.asList("Default"));
        trivirTempUserData.put(DISABLED, Arrays.asList("false"));
        trivirTempUserData.put(LOCKED, Arrays.asList("false"));
        trivirTempUserData.put(OFFLINE_AUTH_ENABLED, Arrays.asList("Default"));

        conn.opValidateUser(trivirTempUserData);
    }

    public void testSuccessValidateUserWithUserAttributes() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserWithRadiusAttrs1);
        conn.opValidateUser(trivirUserWithRadiusAttrs1);
    }

    public void testValidateUserBadData() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);
        Map<String, Collection<String>> trivirTempUserData = new HashMap<String, Collection<String>>();
        trivirTempUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTempUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData.put(HAS_DP, Arrays.asList("BadVal"));
        trivirTempUserData.put(LOCAL_AUTH, Arrays.asList("BadVal"));
        trivirTempUserData.put(BACKEND_AUTH, Arrays.asList("BadVal"));
        trivirTempUserData.put(DISABLED, Arrays.asList("true"));
        trivirTempUserData.put(LOCKED, Arrays.asList("true"));
        trivirTempUserData.put(OFFLINE_AUTH_ENABLED, Arrays.asList("BadVal"));
        trivirTempUserData.put(MOBILE, Arrays.asList("2215457897"));

        trivirTempUserData.put("INVALID_ATTR_NAME", Arrays.asList("InvalidAttrValue"));

        try {
            conn.opValidateUser(trivirTempUserData);
        } catch (IdMUnitException e) {
            String expectedErrorMsg =
                    "Validation failed for account attribute [LOCAL_AUTH] expected:<[BadVal]> but was:<[Default]>\r\n" +
                            "Validation failed for account attribute [MOBILE] expected value: [2215457897] but the attribute value did not exist in the application.\r\n" +
                            "Validation failed for account attribute [OFFLINE_AUTH_ENABLED] expected:<[BadVal]> but was:<[Default]>\r\n" +
                            "Validation failed for account attribute [INVALID_ATTR_NAME] expected value: [InvalidAttrValue] but the attribute value did not exist in the application.\r\n" +
                            "Validation failed for account attribute [DISABLED] expected:<[true]> but was:<[false]>\r\n" +
                            "Validation failed for account attribute [HAS_DP] expected:<[BadVal]> but was:<[Unassigned]>\r\n" +
                            "Validation failed for account attribute [LOCKED] expected:<[true]> but was:<[false]>\r\n" +
                            "Validation failed for account attribute [BACKEND_AUTH] expected:<[BadVal]> but was:<[Default]>";
            if (!e.getMessage().equalsIgnoreCase(expectedErrorMsg)) {
                System.out.println(e.getMessage());
                throw new IdMUnitException("Wrong error message recieved: ", e);
            } else {
                System.out.println(EXPECTED_ERROR_SUCCESS);
            }
        }
    }

    public void testValidateUserBadUserAttributeData() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserWithRadiusAttrs1);
        Map<String, Collection<String>> trivirUserWithRadiusAttrs3AdditionalAttrs = new HashMap<String, Collection<String>>();

        try {
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(USER_ID, Arrays.asList(TEST_USER_NAME));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(CREATE_TIME, Arrays.asList(".*"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(MODIFY_TIME, Arrays.asList(".*"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(HAS_DP, Arrays.asList("Unassigned"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(LOCAL_AUTH, Arrays.asList("BADVALUE"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(BACKEND_AUTH, Arrays.asList("Default"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(DISABLED, Arrays.asList("false"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(LOCKED, Arrays.asList("false"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put(OFFLINE_AUTH_ENABLED, Arrays.asList("Default"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put("ua:Callback-id", Arrays.asList(
                    "ATTR_GROUP:External_Groupcaid, " +
                            "USAGE_QUALIFIER:REPLYcaId, " +
                            "VALUE:test@test.comcaId, " +
                            "OPTIONS:1"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put("ua:Callback-id2", Arrays.asList(
                    "ATTR_GROUP:External_Groupcaid2, " +
                            "USAGE_QUALIFIER:REPLYcaId2, " +
                            "VALUE:test@test.comcaId2, " +
                            "OPTIONS:false"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put("ua:Radius-Example", Arrays.asList(
                    "ATTR_GROUP:External_Group-RE, " +
                            "USAGE_QUALIFIER:REPLY-RE, " +
                            "VALUE:test@test.com-RE, " +
                            "OPTIONS:true"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put("ua:NON-Radius-Example", Arrays.asList(
                    "ATTR_GROUP:External_Group-NON-RE, " +
                            "USAGE_QUALIFIER:REPLY-NON-RE, " +
                            "VALUE:BAD_External_Group_Value"));
            trivirUserWithRadiusAttrs3AdditionalAttrs.put("ua:BAD_UA_ATTR", Arrays.asList(
                    "ATTR_GROUP:External_Group-NON-RE, " +
                            "USAGE_QUALIFIER:REPLY-NON-RE, " +
                            "VALUE:test@test.com-NON-RE"));

            conn.opValidateUser(trivirUserWithRadiusAttrs3AdditionalAttrs);

        } catch (IdMUnitException e) {
            String expectedErrorMsg =
                    "Validation failed for account attribute [LOCAL_AUTH] expected:<[BADVALUE]> but was:<[Default]>\r\n" +
                            "Validation failed for the user attribute [ua:BAD_UA_ATTR]. The user attribute did not exist in the application.\r\n" +
                            "Validation failed for the user attribute [ua:NON-Radius-Example -> VALUE] expected:<[BAD_External_Group_Value]> but was:<[test@test.com-NON-RE]>";
            if (!e.getMessage().equalsIgnoreCase(expectedErrorMsg)) {
                System.out.println(e.getMessage());
                throw new IdMUnitException("Wrong error message recieved: ", e);
            } else {
                System.out.println(EXPECTED_ERROR_SUCCESS);
            }
        }
    }

    public void testValidateUserWhomDoesNOTExist() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);
        try {
            Map<String, Collection<String>> userWhomDoesNotExist = new HashMap<String, Collection<String>>();
            userWhomDoesNotExist.put(USER_ID, Arrays.asList("InValidName134d"));
            userWhomDoesNotExist.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
            conn.opValidateUser(userWhomDoesNotExist);
        } catch (IdMUnitException e) {
            if (!e.getMessage().contains("Object not found")) {
                throw new IdMUnitException(e);
            }
        }
    }

    public void testValidateUserWrongDomain() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);
        try {
            Map<String, Collection<String>> trivirTempUserData = new HashMap<String, Collection<String>>();
            trivirTempUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
            trivirTempUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN + "InvalidValue"));
            conn.opValidateUser(trivirTempUserData);
        } catch (IdMUnitException e) {
            if (!e.getMessage().contains("Insufficient permission to perform the specified action")) {
                throw new IdMUnitException(e);
            }
        }
        System.out.println(EXPECTED_ERROR_SUCCESS);
    }

    public void testValidateUserWhomDoesNotExist() throws IdMUnitException, XPathExpressionException, InterruptedException {
        Map<String, Collection<String>> nonExistingUser = new HashMap<String, Collection<String>>();
        nonExistingUser.put(USER_ID, Arrays.asList(TEST_USER_NAME + "Invalid987"));
        nonExistingUser.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));

        try {
            conn.opValidateUser(nonExistingUser);
        } catch (IdMUnitException e) {
            String expectedMsg = "Object not found. (Error Code: STAT_NOT_FOUND -1)";
            if (!e.getMessage().equalsIgnoreCase(expectedMsg)) {
                System.out.println(e.getMessage());
                throw e;
            }
        }
        System.out.println();
    } //end testmodifyUserErrorWithDigipassValue


    //The Assign and Unassign were done in the same tests to reduce the number of needed DIGIPASS values.
    public void testAssignAndUnnasignDigipassSuccess() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);


        Map<String, Collection<String>> trivirTempUserData = new HashMap<String, Collection<String>>();
        trivirTempUserData.put(DIGIPASS_SERIAL, Arrays.asList(TEST_DIGIPASS1 + "," + TEST_DIGIPASS2));
        trivirTempUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTempUserData.put(GRACE_PERIOD, Arrays.asList("3"));

        //Assign the Digipass
        conn.opAssignDigipass(trivirTempUserData);

        Map<String, Collection<String>> trivirTempUserDataForValidateAssigned = new HashMap<String, Collection<String>>();
        trivirTempUserDataForValidateAssigned.put(ASSIGNED_DIGIPASS, Arrays.asList(TEST_DIGIPASS1 + "," + TEST_DIGIPASS2));
        trivirTempUserDataForValidateAssigned.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserDataForValidateAssigned.put(USER_ID, Arrays.asList(TEST_USER_NAME));

        Map<String, Collection<String>> trivirTempUserDataForAssignUnassign = new HashMap<String, Collection<String>>();
        trivirTempUserDataForAssignUnassign.put(DIGIPASS_SERIAL, Arrays.asList(TEST_DIGIPASS1 + "," + TEST_DIGIPASS2));
        trivirTempUserDataForAssignUnassign.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserDataForAssignUnassign.put(USER_ID, Arrays.asList(TEST_USER_NAME));

        Map<String, Collection<String>> trivirTempUserDataForValidateUnassigned = new HashMap<String, Collection<String>>();
        trivirTempUserDataForValidateUnassigned.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserDataForValidateUnassigned.put(USER_ID, Arrays.asList(TEST_USER_NAME));

        //Validate the Digipass assigned
        conn.opValidateUser(trivirTempUserDataForValidateAssigned);

        //Unassign the digipass
        conn.opUnAssignDigipass(trivirTempUserDataForAssignUnassign);

        //Assert that validating the old ASSIGNED_DIGIPASS values faiesl
        try {
            conn.opValidateUser(trivirTempUserDataForValidateAssigned);
        } catch (IdMUnitFailureException e) {
            //ignore exception
        }

        //Assert the the rest of the user data is good.
        conn.opValidateUser(trivirTempUserDataForValidateUnassigned);

    } //end testAssignedDigipassSuccess

    public void testAssignDigipassWrongColumnName() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);

        Map<String, Collection<String>> trivirTempUserData = new HashMap<String, Collection<String>>();
        trivirTempUserData.put(DIGIPASS_SERIAL, Arrays.asList(TEST_DIGIPASS1 + "," + TEST_DIGIPASS2));
        trivirTempUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTempUserData.put(GRACE_PERIOD, Arrays.asList("3"));

        conn.opAssignDigipass(trivirTempUserData);

        Map<String, Collection<String>> trivirTempUserData2 = new HashMap<String, Collection<String>>();
        trivirTempUserData2.put(DIGIPASS_SERIAL, Arrays.asList(TEST_DIGIPASS1 + "," + TEST_DIGIPASS2));
        trivirTempUserData2.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData2.put(USER_ID, Arrays.asList(TEST_USER_NAME));

        try {
            conn.opValidateUser(trivirTempUserData2);
        } catch (IdMUnitException e) {
            if (!e.getMessage().equalsIgnoreCase(VascoConnector.WRONG_DIGIPASS_ATTR_NAME_ERROR_MSG)) {
                System.out.println(e.getMessage());
                throw e;
            }
        }


    } //end testAssignedDigipassWRONG COLUMN NAME

    public void testAssignDigipassFailure() throws IdMUnitException, XPathExpressionException {
        conn.opCreateUser(trivirUserData);

        Map<String, Collection<String>> trivirTempUserData = new HashMap<String, Collection<String>>();
        trivirTempUserData.put(DIGIPASS_SERIAL, Arrays.asList(TEST_DIGIPASS4 + "," + TEST_DIGIPASS5));
        trivirTempUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTempUserData.put(GRACE_PERIOD, Arrays.asList("3"));

        conn.opAssignDigipass(trivirTempUserData);

        Map<String, Collection<String>> trivirTempUserData2 = new HashMap<String, Collection<String>>();
        trivirTempUserData2.put(ASSIGNED_DIGIPASS, Arrays.asList("0000000000"));
        trivirTempUserData2.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData2.put(USER_ID, Arrays.asList(TEST_USER_NAME));

        try {
            conn.opValidateUser(trivirTempUserData2);
        } catch (IdMUnitException e) {
            if (!e.getMessage().startsWith("Validation failed for account attribute [" + ASSIGNED_DIGIPASS + "] expected:<[0000000000]> but was:<[")) {
                System.out.println(e.getMessage());
                throw e;
            }
        }
    } //end testAssignDigipassFailure

    public void testAssignDigipassErrorAlreadyAssigned() throws IdMUnitException, XPathExpressionException, InterruptedException {
        conn.opCreateUser(trivirUserData);

        Map<String, Collection<String>> trivirTempUserData = new HashMap<String, Collection<String>>();
        trivirTempUserData.put(DIGIPASS_SERIAL, Arrays.asList(TEST_DIGIPASS3));
        trivirTempUserData.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTempUserData.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTempUserData.put(GRACE_PERIOD, Arrays.asList("3"));
        conn.opAssignDigipass(trivirTempUserData);
        try {
            conn.opAssignDigipass(trivirTempUserData);
        } catch (IdMUnitException e) {
            if (!e.getMessage().equalsIgnoreCase("Digipass already assigned to user (Error Code: STAT_DIGIPASS_NOT_AVAILABLE -1)")) {
                System.out.println(e.getMessage());
                throw e;
            }
        }
        System.out.println(EXPECTED_ERROR_SUCCESS);
    } //end testAssignDigipassErrorAlreadyAssigned

    public void testModifyUserAccountAttrs() throws IdMUnitException, XPathExpressionException, InterruptedException {
        conn.opCreateUser(trivirUserData);

        Map<String, Collection<String>> trivirTestUserModified = new HashMap<String, Collection<String>>();
        trivirTestUserModified.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTestUserModified.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTestUserModified.put(HAS_DP, Arrays.asList("Unassigned"));
        trivirTestUserModified.put(LOCAL_AUTH, Arrays.asList("None"));
        trivirTestUserModified.put(BACKEND_AUTH, Arrays.asList("None"));
        trivirTestUserModified.put(DISABLED, Arrays.asList("true"));
        trivirTestUserModified.put(LOCKED, Arrays.asList("true"));
        trivirTestUserModified.put(OFFLINE_AUTH_ENABLED, Arrays.asList("Default"));

        conn.opModifyUser(trivirTestUserModified);
    }

    public void testModifyUserAccountRadiusAttrsError() throws IdMUnitException, XPathExpressionException, InterruptedException {
        conn.opCreateUser(trivirUserData);
        try {
            conn.opModifyUser(trivirUserWithRadiusAttrs1);
        } catch (IdMUnitException e) {
            if (!e.getMessage().startsWith("ERROR. Can't modify any user attributes.")) {
                System.out.println(e.getMessage());
                throw e;
            }
        }
        System.out.println(EXPECTED_ERROR_SUCCESS);
    }

    public void testModifyUserErrorWithDigipassValue() throws IdMUnitException, XPathExpressionException, InterruptedException {
        conn.opCreateUser(trivirUserData);

        Map<String, Collection<String>> trivirTestUserModified = new HashMap<String, Collection<String>>();
        trivirTestUserModified.put(USER_ID, Arrays.asList(TEST_USER_NAME));
        trivirTestUserModified.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));
        trivirTestUserModified.put(DIGIPASS_SERIAL, Arrays.asList(TEST_DIGIPASS1));

        try {
            conn.opModifyUser(trivirTestUserModified);
        } catch (IdMUnitException e) {
            String expectedMsg = "Invalid attribute for this operation: " + DIGIPASS_SERIAL + ". This can only be modified with the AssignDigipass and UnAssignDigipass operations.";
            if (!e.getMessage().equalsIgnoreCase(expectedMsg)) {
                System.out.println(e.getMessage());
                throw e;
            }
        }
    } //end testmodifyUserErrorWithDigipassValue

    public void testModifyErrorUserDoesNotExist() throws IdMUnitException, XPathExpressionException, InterruptedException {
        Map<String, Collection<String>> trivirTestUserUserDoesNotExist = new HashMap<String, Collection<String>>();
        trivirTestUserUserDoesNotExist.put(USER_ID, Arrays.asList(TEST_USER_NAME + "Invalid987"));
        trivirTestUserUserDoesNotExist.put(DOMAIN, Arrays.asList(TEST_SERVER_DOMAIN));

        try {
            conn.opModifyUser(trivirTestUserUserDoesNotExist);
        } catch (IdMUnitException e) {
            String expectedMsg = "Object not found. (Error Code: STAT_NOT_FOUND -1)";
            if (!e.getMessage().equalsIgnoreCase(expectedMsg)) {
                System.out.println(e.getMessage());
                throw e;
            }
        }
    }
} //End VascoConnector Tests class
