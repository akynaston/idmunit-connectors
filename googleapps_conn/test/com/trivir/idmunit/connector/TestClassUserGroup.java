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

import com.trivir.idmunit.connector.api.GroupApi;
import com.trivir.idmunit.connector.api.UserApi;
import com.trivir.idmunit.connector.api.resource.Group;
import com.trivir.idmunit.connector.api.resource.User;
import com.trivir.idmunit.connector.util.EntityConverter;
import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.ConnectorUtil;

import java.util.*;

import static com.trivir.idmunit.connector.ConfigTests.TEST_DOMAIN;
import static com.trivir.idmunit.connector.GoogleAppsConnector.*;
import static com.trivir.idmunit.connector.api.UserApi.listUsersInDomain;
import static com.trivir.idmunit.connector.util.TestUtil.deleteObjectSuppressed;
import static com.trivir.idmunit.connector.util.TestUtil.waitTimeSeconds;

public class TestClassUserGroup extends TestCase {

    private static final String USER_NAME = "ttester@idmunit.org";
    private static final String BAD_DOMAIN = "somedomainthatwillneverbeusedinthisoranyuniverseacrossalltimeandspace.org";
    private static final String BAD_USER_NAME = "BWayne@" + BAD_DOMAIN;
    private static final String GIVEN_NAME = "Timtim";
    private static final String FAMILY_NAME = "Tester";
    private static final String PASSWORD = "password123";
    private static final String GROUP_ROLE = "MEMBER";
    private static final String GROUP_EMAIL = "group1@idmunit.org";
    private static final String GROUP_NAME = "Group 1";
    private static final String GROUP_DESCRIPTION = "This is group #1";
    private static final String NEW_GROUP_EMAIL = "changedgroup@idmunit.org";

    private Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
    private Map<String, Collection<String>> dataGroup = new HashMap<String, Collection<String>>();
    private GoogleAppsConnector conn = null;

    {
        data.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        data.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        data.put(User.Schema.ATTR_GIVEN_NAME, Arrays.asList(GIVEN_NAME));
        data.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList(FAMILY_NAME));
        data.put(User.Schema.ATTR_PASSWORD, Arrays.asList(PASSWORD));
        data.put(Group.Schema.ATTR_GROUP_EMAIL, Arrays.asList(GROUP_EMAIL));
        data.put(Group.Schema.ATTR_GROUP_NAME, Arrays.asList(GROUP_NAME));
        data.put(Group.Schema.ATTR_GROUP_DESCRIPTION, Arrays.asList(GROUP_DESCRIPTION));
        data.put(Group.Schema.ATTR_GROUP_ROLE, Arrays.asList(GROUP_ROLE));
        data.put(Group.Schema.ATTR_GROUP_EMAIL, Arrays.asList(GROUP_EMAIL));
    }

    {
        dataGroup.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(Group.Schema.CLASS_NAME));
        dataGroup.put(Group.Schema.ATTR_GROUP_EMAIL, Arrays.asList(GROUP_EMAIL));
        dataGroup.put(Group.Schema.ATTR_GROUP_NAME, Arrays.asList(GROUP_NAME));
        dataGroup.put(Group.Schema.ATTR_GROUP_DESCRIPTION, Arrays.asList(GROUP_DESCRIPTION));
    }

    protected void setUp() throws Exception {
        conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        String superUser = "admin@" + TEST_DOMAIN;
        String serviceEmail = "shim-and-conn-test@shim-and-conn-dev.iam.gserviceaccount.com";
        String privateKey = "-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCxWnnpRSk7yZ0a\\ng7/x0YgH7EOg+6RM8W3mCNlj4nwnwywdf7v/aeYXsBGju3ylluKvtsLfHH/dni0/\\nbdzNBoxhlir5glFXs+ZgYDjieAGQU8oBkwwLhGZNOWadoLJ/4BKwUDy7Zgzu7KGu\\nf8irt358TXvYXrMKZYcOumcWHLtW3ciqECwuwDVYU1GgxEhfwghN1b1kgOB1CQyN\\nfw2C369qIa2m8/+Q0leqEiHk1J/xkF/XV2urElKYnIbNAVa+CieSbCvEtQBvROnM\\nNSdccTfh/dU1WvvnNjyXmg87pAUTlNTHzuVREq9FOQDFSvrAvrpYO9PrNen4AQob\\nA8VhVmltAgMBAAECggEAfq2MZJVU5XKVt6mhgX1Td61HhQYZDihogiWR+Wl9mv0q\\nVou1YbNneUX244d4eeJzWmTlfm2h208vLJ4xV3S08sNLQNrXdRh3liFEoGZtX4Sp\\nxkQdF2DjnYdBh5ePyAzp7GvzZTt4Q3Rb7AMz94tiWjESI7NImUV5mYiFN2MgYONn\\naREw0D+4ppbpqM18SYJp05DdCSwIyOC5UO3EsKEJS+kKR3L1j+y6+evFtcNLBxTG\\nsEmORdtI5jSdEMlk9hKp4fzAm+dsF5Ie6vzvIfuxSyOnI5nVOFWdpEHRUsxbCj7H\\nOsy+KGrGC1ewQg6RR4BhTqi66VnNZODWV08RjDWNwQKBgQD492FUDIoiP4BTRjIY\\nXc3qqi8fznYlYMeeYB2mJ6PO3nLvag3RJPIcLSzeywYksKyPScDKDlIF+/G1ufqX\\n1DWj35hFqKq1NxffowxI89KkM71t+DdvhMKLnc7D39ommAml9BBojIBvKEYZ7Fd0\\nlAORhU20eefOaemPpo9TUENcsQKBgQC2XSoAluP/2FgNt5sAs9mNToqmemnEkAA1\\nWlVkD96vCAqQMBs3utw1N5Gm0kqCb/uZh/h523sQyqQBrceqHlPDQkPyIyZESl6V\\nl7vvYmABvyUeL2mOMwWcJrkoGnGQs7hed9i143s4A14EDsaxzIlpLNN7LmgmxfcV\\n3zbsH5JXfQKBgQC/LY9ScDKea+7Jg2yyY03dNgPrw6nbt/5xclMyJNxX3V+a0vB7\\nOoij9FixWGuGPxizCyp8vhRkPfx01LRGZJEwHmGalBNKBl1RwK2NU5Xbu1NqH6HK\\nA8M0XODKbpng6vz1r33uGn4BXYa/H0pk3cgDtb5eqQHE8nWEdp02l7qycQKBgGRi\\nrHhel2uCwBXs+BpO5nbuwUwbHpXhXvv/mfnW8pIPLyFoGdN3vTheOoNGR1W+JxXA\\nz3rk4r2/jsCN1NdEkn9tvtFPoAT/m0llmUKROKA9hEU1fDmWxIPMnSgCRnmNNPRr\\nrJOTgYS39czuBVpiaVHIJzIrvZF6cCVOFoGsb3ZRAoGBAL8hNDkBK1xrHFaiekiH\\nLSqKFMc9QaQk/7VUBTxUYdNHLJdfyW9t2IEqSpRrHO9oXY/7FB/N8ZDm8bOFDNPf\\nYjvxjTOkiNj4+GG+ga8IoyHqmMRucIVCMZ0GueINswEQvFxkmjnbrMTKgiyrCtdb\\nwKkq9hDYu4zA+NwTOIqBMK2n\\n-----END PRIVATE KEY-----\\n";

        config.put(CONFIG_SUPER_USER_EMAIL, superUser);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, serviceEmail);
        config.put(CONFIG_PRIVATE_KEY, privateKey);
        conn.setup(config);

        deleteObjectSuppressed(conn, dataGroup);
        deleteObjectSuppressed(conn, data);

    }

    protected void tearDown() throws Exception {
        conn.opDeleteObject(dataGroup);
        conn.opDeleteObject(data);

        conn = null;
    }

    public void testListUsersByDomain() throws IdMUnitException {
        List<User> users = listUsersInDomain(conn.getRestClient(), "idmunit.org");
        boolean found = false;
        for (User user : users) {
            if (user != null) {
                if (ADMIN_EMAIL.equals(user.getPrimaryEmail())) {
                    found = true;
                }
            }
        }

        assertTrue(found);
    }

    public void testDeleteGroupDoesNotExist() throws IdMUnitException {

        GroupApi.deleteGroup(conn.getRestClient(), ConnectorUtil.getSingleValue(dataGroup, Group.Schema.ATTR_GROUP_EMAIL));
        assertTrue(true);
    }

    public void testGroupValidation() {
        try {
            conn.opCreateObject(dataGroup);
            conn.opValidateObject(dataGroup);
        } catch (IdMUnitException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    //[EMPTY]" positive test (field is actually empty)
    public void testGroupValidationAttrEmpty() {
        try {
            Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(dataGroup);
            conn.opCreateObject(map);
            map.put("adminCreated", Arrays.asList("[EMPTY]"));
            conn.opValidateObject(map);
        } catch (IdMUnitException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    //[EMPTY]" negative test (field is not empty)
    public void testGroupValidationAttrNotEmpty() {
        try {
            Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(dataGroup);
            conn.opCreateObject(map);
            map.put(Group.Schema.ATTR_GROUP_DESCRIPTION, Arrays.asList("[EMPTY]"));

            try {
                conn.opValidateObject(map);
            } catch (IdMUnitException e) {
                String msg = e.getMessage().toLowerCase();
                if (!msg.contains("groupdescription") && !msg.contains("not equal")) {
                    throw e;
                }
            }
        } catch (IdMUnitException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testAddMemberGroup() {
        try {
            conn.opCreateObject(dataGroup);
            conn.opAddGroupMemeber(data);
        } catch (IdMUnitException e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }

    //NOTE: this test is timing-sensitive
    //NOTE: adjusted timing upward significantly as 3 seconds was grossly insufficient
    public void testRemoveGroupMember() {
        final int waitSeconds = 30;

        try {
            conn.opCreateObject(dataGroup);
            conn.opAddGroupMemeber(data);
            waitTimeSeconds(waitSeconds);
            Group group = GroupApi.getGroup(conn.getRestClient(), GROUP_EMAIL);
            assertTrue(group.directMembersCount == 1);
            conn.opRemoveGroupMemeber(data);
            waitTimeSeconds(waitSeconds);
            group = GroupApi.getGroup(conn.getRestClient(), GROUP_EMAIL);
            assertTrue(group.directMembersCount == 0);
        } catch (IdMUnitException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testDeleteGroup() throws IdMUnitException {
        conn.opDeleteObject(dataGroup);
    }

    public void testValidateGroupDoesNotExist() {
        try {
            conn.opValidateObject(dataGroup);
            assertTrue(false);
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().equals(String.format("Error group '%s' does not exist", GROUP_EMAIL)));
        }
    }

    public void testAddGroupMemberGroupDoesNotExistException() {
        try {
            conn.opDeleteObject(dataGroup);
            conn.opAddGroupMemeber(data);
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().equals(String.format("Error group '%s' does not exist", GROUP_EMAIL)));
        }
    }

    //TODO: test group exist and user already exist
    public void testAddGroupMemberAlreadyExistException() throws IdMUnitException {
        try {
            conn.opCreateObject(dataGroup);
            conn.opAddGroupMemeber(data);
            conn.opAddGroupMemeber(data);
        } catch (IdMUnitException e) {

            conn.opDeleteObject(dataGroup);
            if (!(e.getMessage().equals(String.format("Error unable to add user '%s' to group '%s'. Member already exists.", USER_NAME, GROUP_EMAIL)))) {
                System.err.println(e.getMessage());
            }
            assertTrue(e.getMessage().equals(String.format("Error unable to add user '%s' to group '%s'. Member already exists.", USER_NAME, GROUP_EMAIL)));
        }
    }

    public void testGroupRename() throws IdMUnitException {

        try {
            conn.opCreateObject(dataGroup);
        } catch (IdMUnitException e) {
            throw new IdMUnitException("Could not continue test, failed creating group", e);
        }

        Map<String, Collection<String>> dataGroupTemp = new HashMap<String, Collection<String>>();
        dataGroupTemp.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(Group.Schema.CLASS_NAME));
        dataGroupTemp.put(Group.Schema.ATTR_GROUP_EMAIL, Arrays.asList(GROUP_EMAIL));
        dataGroupTemp.put(Group.Schema.ATTR_NEW_GROUP_EMAIL, Arrays.asList(NEW_GROUP_EMAIL));
        dataGroupTemp.put(Group.Schema.ATTR_GROUP_NAME, Arrays.asList("Group 1"));
        dataGroupTemp.put(Group.Schema.ATTR_GROUP_DESCRIPTION, Arrays.asList("This is the changed group! success!"));

        try {
            conn.opRenameObject(dataGroupTemp);
        } catch (IdMUnitException e) {
            throw new IdMUnitException("Note: Failed to rename group: Run the test again and it should pass: TODO: find out why it doesn't pass sometimes!", e);
        }

        dataGroupTemp.remove(Group.Schema.ATTR_GROUP_EMAIL);
        dataGroupTemp.put(Group.Schema.ATTR_GROUP_EMAIL, Arrays.asList(NEW_GROUP_EMAIL));

        dataGroupTemp.remove(Group.Schema.ATTR_NEW_GROUP_EMAIL);
        //TODO: should I be validating certain fields within a group?

        try {
            conn.opValidateObject(dataGroupTemp);

        } catch (IdMUnitException e) {
            e.printStackTrace();
        }
        try {
            conn.opDeleteObject(dataGroupTemp);
        } catch (IdMUnitException e) {
            e.printStackTrace();
        }

    }

    //TODO: test creating a group outside of owned domains

    public void testGroupModify() {

    }

    //TODO: test invalid email address for group and username  - THIS WILL ALWAYS

    //TODO: test organization does not exist and add when modifying a user

    // If User exist he will be deleted
    public void testUserDoesNotExistException() throws IdMUnitException {
        try {
            conn.opValidateObject(data);
            conn.opDeleteObject(data);
        } catch (IdMUnitException e) {
            if (!e.getMessage().equals(String.format("Error user '%s' does not exist", USER_NAME))) {
                throw e;
            }
        }
    }

    public void testCreateUserWithIncludeInGlobalAddressListTrue() throws IdMUnitException {
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        map.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Collections.singleton("true"));

        conn.opCreateObject(map);
        User user = UserApi.getUser(conn.getRestClient(), USER_NAME);
        assertTrue(user.getIncludeInGlobalAddressList());
    }

    public void testCreateUserWithIncludeInGlobalAddressListFalse() throws IdMUnitException {
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        map.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Collections.singleton("false"));

        conn.opCreateObject(map);
        User user = UserApi.getUser(conn.getRestClient(), USER_NAME);
        assertFalse(user.getIncludeInGlobalAddressList());
    }


    public void testUpdateUser() throws IdMUnitException {
        conn.opCreateObject(data);
        User user = UserApi.getUser(conn.getRestClient(), USER_NAME);

        assertTrue(user.getGivenName().equals(GIVEN_NAME));
        assertTrue(user.getFamilyName().equals(FAMILY_NAME));

        final String newGivenName = "Tomtom";
        final String newFamilyName = "TesterMod";
        final String ou = "/";
        final String workPhone = "800-333-4545";
        final String homePhone = "127-169-0001";
        final String mobilePhone = "867-5309";
        final String orgTitle = "BOSS MAN";
        final String orgDepartment = "THE DEPARTMENT";
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        map.put(User.Schema.ATTR_GIVEN_NAME, Arrays.asList(newGivenName));
        map.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList(newFamilyName));
        map.put(User.Schema.ATTR_OU, Arrays.asList(ou));
        map.put(User.Schema.ATTR_PHONE_WORK, Arrays.asList(workPhone));
        map.put(User.Schema.ATTR_PHONE_HOME, Arrays.asList(homePhone));
        map.put(User.Schema.ATTR_PHONE_MOBILE, Arrays.asList(mobilePhone));
        map.put(User.Schema.ATTR_ORG_TITLE, Arrays.asList(orgTitle));
        map.put(User.Schema.ATTR_ORG_DEPARTMENT, Arrays.asList(orgDepartment));

        conn.opModifyObject(map);
        waitTimeSeconds(10);
        conn.opValidateObject(map);

        User newuser = UserApi.getUser(conn.getRestClient(), USER_NAME);

        if (!(newuser.getGivenName().equals(newGivenName))) {
            System.err.println("givenName actual: [" + newuser.getGivenName() + "] - Expected: [" + newGivenName + "]");
            assertTrue(false);
        }
        if (!(newuser.getFamilyName().equals(newFamilyName))) {
            System.err.println("familyName actual: [" + newuser.getFamilyName() + "] - Expected: [" + newFamilyName + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgUnitPath().equals(ou))) {
            System.err.println("OrgUnitPath actual: [" + newuser.getOrgUnitPath() + "] - Expected: [" + ou + "]");
            assertTrue(false);
        }
        if (!(newuser.getWorkPhone().equals(workPhone))) {
            System.err.println("OrgUnitPath actual: [" + newuser.getWorkPhone() + "] - Expected: [" + workPhone + "]");
            assertTrue(false);
        }
        if (!(newuser.getHomePhone().equals(homePhone))) {
            System.err.println("OrgUnitPath actual: [" + newuser.getHomePhone() + "] - Expected: [" + homePhone + "]");
            assertTrue(false);
        }
        if (!(newuser.getMobilePhone().equals(mobilePhone))) {
            System.err.println("OrgUnitPath actual: [" + newuser.getMobilePhone() + "] - Expected: [" + mobilePhone + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgDepartment().equals(orgDepartment))) {
            System.err.println("OrgUnitPath actual: [" + newuser.getOrgDepartment() + "] - Expected: [" + orgDepartment + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgTitle().equals(orgTitle))) {
            System.err.println("OrgUnitPath actual: [" + newuser.getOrgTitle() + "] - Expected: [" + orgTitle + "]");
            assertTrue(false);
        }
    }

    public void testRemoveUserAttrs() throws IdMUnitException {
        final String ou = "/";
        final String workPhone = "800-333-4545";
        final String homePhone = "127-169-0001";
        final String mobilePhone = "867-5309";
        final String orgTitle = "BOSS MAN";
        final String orgDepartment = "THE DEPARTMENT";
        Map<String, Collection<String>> userData = data;
        userData.put(User.Schema.ATTR_OU, Arrays.asList(ou));
        userData.put(User.Schema.ATTR_PHONE_WORK, Arrays.asList(workPhone));
        userData.put(User.Schema.ATTR_PHONE_HOME, Arrays.asList(homePhone));
        userData.put(User.Schema.ATTR_PHONE_MOBILE, Arrays.asList(mobilePhone));
        userData.put(User.Schema.ATTR_ORG_TITLE, Arrays.asList(orgTitle));
        userData.put(User.Schema.ATTR_ORG_DEPARTMENT, Arrays.asList(orgDepartment));
        conn.opCreateObject(userData);
        waitTimeSeconds(10);
        conn.opModifyObject(userData);
        waitTimeSeconds(10);

//        conn.opValidateObject(userData);
        User user = UserApi.getUser(conn.getRestClient(), USER_NAME);

        assertTrue(user.getGivenName().equals(GIVEN_NAME));
        assertTrue(user.getFamilyName().equals(FAMILY_NAME));
        assertTrue(user.getOrgUnitPath().equals(ou));
        assertTrue(user.getOrgTitle().equals(orgTitle));
        assertTrue(user.getOrgDepartment().equals(orgDepartment));
        assertTrue(user.getWorkPhone().equals(workPhone));
        assertTrue(user.getHomePhone().equals(homePhone));
        assertTrue(user.getMobilePhone().equals(mobilePhone));

        Map<String, Collection<String>> removeAttrs = new HashMap<String, Collection<String>>();
        removeAttrs.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        removeAttrs.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        removeAttrs.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList("*"));
        removeAttrs.put(User.Schema.ATTR_GIVEN_NAME, Arrays.asList("*"));
        removeAttrs.put("phones", Arrays.asList("*"));
        removeAttrs.put("organizations", Arrays.asList("*"));

        conn.opClearAttr(removeAttrs);
        waitTimeSeconds(10);
        user = UserApi.getUser(conn.getRestClient(), USER_NAME);

        assertNull(user.getGivenName());
        assertNull(user.getFamilyName());
        assertNull(user.getOrgTitle());
        assertNull(user.getOrgDepartment());
        assertNull(user.getWorkPhone());
        assertNull(user.getHomePhone());
        assertNull(user.getMobilePhone());
    }

    public void testValidateUserPasswordSuccess() throws IdMUnitException {
        conn.opCreateObject(data);
        User user = UserApi.getUser(conn.getRestClient(), USER_NAME);                //We have to run getUser before validating password.
        assertTrue(user.getGivenName().equals(GIVEN_NAME));
        assertTrue(user.getFamilyName().equals(FAMILY_NAME));

        final String newPassword = "TheActualPasswordValue1234";
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList(newPassword));

        conn.opModifyObject(map);                            //Testing showed we had to change the password before validating it for the first time.
        waitTimeSeconds(10);

        conn.opValidatePassword(map);
    }

    public void testValidateUserPasswordFailure() throws IdMUnitException {
        final String newPassword = "TheActualPasswordValue1234";
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList(newPassword));

        conn.opCreateObject(data);

        conn.opModifyObject(map);
        waitTimeSeconds(10);

        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList("TheWrongPassword"));

        // Should fail on password:
        try {
            conn.opValidatePassword(map);
            fail("Password validation should have failed!");
        } catch (IdMUnitFailureException ife) {
            assertEquals("Password validation failed for user: [ttester@idmunit.org] password: [TheWrongPassword]", ife.getMessage());
        }
    }

    public void testValidateUserPasswordOnNormalGoogleUser() throws IdMUnitException {
        final String userName = "testaccnt07@gmail.com";
        final String newPassword = "Trivir1234";

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(userName));
        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList(newPassword));

        conn.opValidatePassword(map);
    }

    //NOTE: this test is no longer valid as we routinely delete all Google Users
/*    public void testValidateUserPasswordOnFCPSGoogleUser() throws IdMUnitException {
        //FCPS Goolge Apps Credentials for ckynaston are here: https://src.trivir.com/customer/FairfaxCountyPublicSchools/trunk/IDM/env/Environment-Info-VPNandRDP.txt
        final String userName = "CKynaston@fcpsschools.net";
        final String newPassword = "Trivir02";

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList({User.Schema.CLASS_NAME}));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList({userName}));
        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList({newPassword}));

        conn.opValidatePassword(map);
    }*/


    public void testDeletingUserDoesNotExistException() throws IdMUnitException {
        conn.opDeleteObject(data);
        try {
            conn.opValidateObject(data);
            assertTrue(false);
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().equals(String.format("Error user '%s' does not exist", USER_NAME)));
        }
    }

    public void testDeletingNonExistingUser() throws IdMUnitException {
        final String username = "IdontExist@idmunit.org";
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(username));
        conn.opDeleteObject(map);
    }

    public void testRenameUser() throws Exception {
        final String newGivenName = "Tomtom";
        final String newFamilyName = "TesterMod";
        final String newPassword = "password123";
        final String newUserName = "ChangedName@idmunit.org";

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        map.put(User.Schema.ATTR_GIVEN_NAME, Arrays.asList(newGivenName));
        map.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList(newFamilyName));
        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList(newPassword));
        map.put(User.Schema.ATTR_NEW_USERNAME, Arrays.asList(newUserName));

        try {
            conn.opCreateObject(map);
            waitTimeSeconds(2);
            conn.opRenameObject(map);
            waitTimeSeconds(2);
            UserApi.getUser(conn.getRestClient(), newUserName);

            //Overwrite username attribute with new one for deletion
            map.put(User.Schema.ATTR_USERNAME, Arrays.asList(newUserName));
            waitTimeSeconds(2);
            conn.opDeleteObject(map);

        } catch (IdMUnitException e) {
            conn.opDeleteObject(map);
            throw e;
        }
    }

    //TODO: test simple create to test default values & complex create to make sure all values are set
    public void testCreateUser() throws IdMUnitException {
        conn.opCreateObject(data);
        waitTimeSeconds(5);
        User newuser = UserApi.getUser(conn.getRestClient(), USER_NAME);
        String value;

        value = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_GIVEN_NAME);
        if (!(newuser.getGivenName().equals(value))) {
            System.err.println("givenName actual: [" + newuser.getGivenName() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_FAMILY_NAME);
        if (!(newuser.getFamilyName().equals(value))) {
            System.err.println("familyName actual: [" + newuser.getFamilyName() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = "/";
        if (!(newuser.getOrgUnitPath().equals(value))) {
            System.err.println("orgUnitPath actual: [" + newuser.getOrgUnitPath() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = null;
        if (newuser.getWorkPhone() != value) {
            System.err.println("workPhone actual: [" + newuser.getWorkPhone() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = null;
        if (newuser.getHomePhone() != value) {
            System.err.println("homePhone actual: [" + newuser.getHomePhone() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = null;
        if (newuser.getMobilePhone() != value) {
            System.err.println("mobilePhone actual: [" + newuser.getMobilePhone() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = null;
        if (newuser.getOrgDepartment() != value) {
            System.err.println("department actual: [" + newuser.getOrgDepartment() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = null;
        if (newuser.getOrgTitle() != value) {
            System.err.println("title actual: [" + newuser.getOrgTitle() + "] - Expected: [" + value + "]");
            assertTrue(false);
        }

        value = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST);
        if (newuser.getIncludeInGlobalAddressList() != Boolean.TRUE) {
            System.err.println("includeInGlobalAddressList actual: [" + newuser.getIncludeInGlobalAddressList() + "] - Expected: [" + Boolean.TRUE.toString() + "]");
            assertTrue(false);
        }

        conn.opDeleteObject(data);
    }

    //TODO: this test no longer passes because Google no longer allows one to set phone numbers to ""; need to use
    // a different attribute for this test than a phone number
    public void testValidateNoValue() throws IdMUnitException {

        //NOTE: users are created without phonenumbers
        Map<String, Collection<String>> dataCopy = new HashMap<String, Collection<String>>(data);
        conn.opCreateObject(dataCopy);

        User user = UserApi.getUser(conn.getRestClient(), USER_NAME);

        Map<String, Collection<String>> map = EntityConverter.userToMap(user);
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        map.put(User.Schema.ATTR_PHONE_WORK, Arrays.asList(GoogleAppsConnector.EMPTY_VAL));

        conn.opValidateObject(map);

        conn.opDeleteObject(map);
    }

    public void testModifiyUser() throws IdMUnitException {
        final String newGivenName = "Tomtom";
        final String newFamilyName = "TesterMod";
        final String newPassword = "notapassword";
        final String ou = "/AutomatedTests/org1";
        final String workPhone = "800-333-4545";
        final String homePhone = "127-169-0001";
        final String mobilePhone = "867-5309";
        final String orgTitle = "BOSS MAN";
        final String orgDepartment = "THE DEPARTMENT";
        final String orgDescription = "Need I say more?";
        final String orgName = "Trivir";
        final String orgOffice = "Lehi";
        final String includeInGlobalAddressList = Boolean.FALSE.toString();
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(USER_NAME));
        map.put(User.Schema.ATTR_GIVEN_NAME, Arrays.asList(newGivenName));
        map.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList(newFamilyName));
        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList(newPassword));
        map.put(User.Schema.ATTR_OU, Arrays.asList(ou));
        map.put(User.Schema.ATTR_PHONE_WORK, Arrays.asList(workPhone));
        map.put(User.Schema.ATTR_PHONE_HOME, Arrays.asList(homePhone));
        map.put(User.Schema.ATTR_PHONE_MOBILE, Arrays.asList(mobilePhone));
        map.put(User.Schema.ATTR_ORG_TITLE, Arrays.asList(orgTitle));
        map.put(User.Schema.ATTR_ORG_DEPARTMENT, Arrays.asList(orgDepartment));
        map.put(User.Schema.ATTR_ORG_DESCRIPTION, Arrays.asList(orgDescription));
        map.put(User.Schema.ATTR_ORG_NAME, Arrays.asList(orgName));
        map.put(User.Schema.ATTR_ORG_OFFICE, Arrays.asList(orgOffice));
        map.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Arrays.asList(includeInGlobalAddressList));

        conn.opCreateObject(data);
        conn.opModifyObject(map);
        waitTimeSeconds(5);
        User newuser = UserApi.getUser(conn.getRestClient(), USER_NAME);

        if (!(newuser.getGivenName().equals(newGivenName))) {
            System.err.println("givenName actual: [" + newuser.getGivenName() + "] - Expected: [" + newGivenName + "]");
            assertTrue(false);
        }
        if (!(newuser.getFamilyName().equals(newFamilyName))) {
            System.err.println("familyName actual: [" + newuser.getFamilyName() + "] - Expected: [" + newFamilyName + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgUnitPath().equals(ou))) {
            System.err.println("orgUnitPath actual: [" + newuser.getOrgUnitPath() + "] - Expected: [" + ou + "]");
            assertTrue(false);
        }
        if (!(newuser.getWorkPhone().equals(workPhone))) {
            System.err.println("workPhone actual: [" + newuser.getWorkPhone() + "] - Expected: [" + workPhone + "]");
            assertTrue(false);
        }
        if (!(newuser.getHomePhone().equals(homePhone))) {
            System.err.println("homePhone actual: [" + newuser.getHomePhone() + "] - Expected: [" + homePhone + "]");
            assertTrue(false);
        }
        if (!(newuser.getMobilePhone().equals(mobilePhone))) {
            System.err.println("mobilePhone actual: [" + newuser.getMobilePhone() + "] - Expected: [" + mobilePhone + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgTitle().equals(orgTitle))) {
            System.err.println("orgTitle actual: [" + newuser.getOrgTitle() + "] - Expected: [" + orgTitle + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgDepartment().equals(orgDepartment))) {
            System.err.println("orgDepartment actual: [" + newuser.getOrgDepartment() + "] - Expected: [" + orgDepartment + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgDescription().equals(orgDescription))) {
            System.err.println("orgDescription actual: [" + newuser.getOrgDescription() + "] - Expected: [" + orgDescription + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgName().equals(orgName))) {
            System.err.println("orgName actual: [" + newuser.getOrgName() + "] - Expected: [" + orgName + "]");
            assertTrue(false);
        }
        if (!(newuser.getOrgOffice().equals(orgOffice))) {
            System.err.println("orgOffice actual: [" + newuser.getOrgOffice() + "] - Expected: [" + orgOffice + "]");
            assertTrue(false);
        }
        if (newuser.getIncludeInGlobalAddressList() != Boolean.FALSE) {
            System.err.println("includeInGlobalAddressList actual: [" + newuser.getIncludeInGlobalAddressList() + "] - Expected: [" + Boolean.FALSE.toString() + "]");
            assertTrue(false);
        }

        map.remove(User.Schema.ATTR_PASSWORD);
        conn.opValidateObject(map);

        conn.opDeleteObject(map);
    }

    public void testCreateAndValidateUserWithOrgUnitPath() throws IdMUnitException {

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        conn.opDeleteObject(map);

        //same as ou
        map.put(User.Schema.ATTR_ORG_UNIT_PATH, Collections.singletonList("/"));
        conn.opCreateObject(map);

        map.remove(User.Schema.ATTR_PASSWORD);
        map.remove(Group.Schema.ATTR_GROUP_DESCRIPTION);
        map.remove(Group.Schema.ATTR_GROUP_EMAIL);
        map.remove(Group.Schema.ATTR_GROUP_NAME);
        map.remove(Group.Schema.ATTR_GROUP_ROLE);
        conn.opValidateObject(map);

        conn.opDeleteObject(map);
    }

    public void testCreateAndValidateUserWithOu() throws IdMUnitException {

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        conn.opDeleteObject(map);

        //same as orgUnitPath
        map.put(User.Schema.ATTR_OU, Collections.singletonList("/"));
        conn.opCreateObject(map);

        map.remove(User.Schema.ATTR_PASSWORD);
        map.remove(Group.Schema.ATTR_GROUP_DESCRIPTION);
        map.remove(Group.Schema.ATTR_GROUP_EMAIL);
        map.remove(Group.Schema.ATTR_GROUP_NAME);
        map.remove(Group.Schema.ATTR_GROUP_ROLE);
        conn.opValidateObject(map);

        conn.opDeleteObject(map);
    }

    //TODO: validate simple User and complex User
    public void testValidateUser() throws IdMUnitException {

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        map.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Arrays.asList(Boolean.FALSE.toString()));

        conn.opCreateObject(map);
        //password isn't returned; user validator doesn't check group attributes
        map.remove(User.Schema.ATTR_PASSWORD);
        map.remove(Group.Schema.ATTR_GROUP_DESCRIPTION);
        map.remove(Group.Schema.ATTR_GROUP_EMAIL);
        map.remove(Group.Schema.ATTR_GROUP_NAME);
        map.remove(Group.Schema.ATTR_GROUP_ROLE);
        conn.opValidateObject(map);

        conn.opDeleteObject(map);
    }

    public void testValidateUserFail() throws IdMUnitException {

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        map.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Arrays.asList(Boolean.FALSE.toString()));

        conn.opCreateObject(map);
        //password isn't returned; user validator doesn't check group attributes
        map.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList("other"));
        map.remove(User.Schema.ATTR_PASSWORD);
        map.remove(Group.Schema.ATTR_GROUP_DESCRIPTION);
        map.remove(Group.Schema.ATTR_GROUP_EMAIL);
        map.remove(Group.Schema.ATTR_GROUP_NAME);
        map.remove(Group.Schema.ATTR_GROUP_ROLE);

        try {
            conn.opValidateObject(map);
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            if (!msg.contains("familyname") && !msg.contains("not equal")) {
                throw e;
            }
        }

        conn.opDeleteObject(map);
    }

    //[EMPTY]" positive test (field is actually empty)
    public void testUserAttrEmpty() throws IdMUnitException {

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        map.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Arrays.asList(Boolean.FALSE.toString()));

        conn.opCreateObject(map);
        //check for non-value
        map.put(User.Schema.ATTR_ORG_UNIT_PATH, Arrays.asList(GoogleAppsConnector.EMPTY_VAL));
        //password isn't returned; user validator doesn't check group attributes
        map.remove(User.Schema.ATTR_PASSWORD);
        map.remove(Group.Schema.ATTR_GROUP_DESCRIPTION);
        map.remove(Group.Schema.ATTR_GROUP_EMAIL);
        map.remove(Group.Schema.ATTR_GROUP_NAME);
        map.remove(Group.Schema.ATTR_GROUP_ROLE);
        conn.opValidateObject(map);

        conn.opDeleteObject(map);
    }

    //[EMPTY]" negative test (field is not empty)
    public void testUserAttrNotEmpty() throws IdMUnitException {

        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
        map.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Arrays.asList(Boolean.FALSE.toString()));

        conn.opCreateObject(map);
        //check for non-value
        map.put(User.Schema.ATTR_GIVEN_NAME, Arrays.asList(GoogleAppsConnector.EMPTY_VAL));
        //password isn't returned; user validator doesn't check group attributes
        map.remove(User.Schema.ATTR_PASSWORD);
        map.remove(Group.Schema.ATTR_GROUP_DESCRIPTION);
        map.remove(Group.Schema.ATTR_GROUP_EMAIL);
        map.remove(Group.Schema.ATTR_GROUP_NAME);
        map.remove(Group.Schema.ATTR_GROUP_ROLE);
        try {
            conn.opValidateObject(map);
            fail();
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            if (!msg.contains("givenname") && !msg.contains("not equal")) {
                throw e;
            }
        }

        conn.opDeleteObject(map);
    }

    public void testFamilyNamePlaceholder() throws IdMUnitException {
        final String newFamilyName = "";

        try {
            conn.opDeleteObject(data);

            Map<String, Collection<String>> map = new HashMap<String, Collection<String>>(data);
            map.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList(""));

            conn.opCreateObject(map);
            waitTimeSeconds(5);
            User newuser = UserApi.getUser(conn.getRestClient(), USER_NAME);

            if (!(newuser.getFamilyName().equals(""))) {
                System.err.println("familyName actual: [" + newuser.getFamilyName() + "] - Expected: [" + newFamilyName + "]");
                fail();
            }
        } finally {
            conn.opDeleteObject(data);
        }
    }

    public void testFailCreateUser() throws IdMUnitException {
        final String newGivenName = "Tomtom";
        final String newFamilyName = "TesterMod";
        final String newPassword = "notapassword";
        final String ou = "/AutomatedTests/org1";
        final String workPhone = "800-333-4545";
        final String homePhone = "127-169-0001";
        final String mobilePhone = "867-5309";
        final String orgTitle = "BAT MAN";
        final String orgDepartment = "WAYNE ENTERPRISES";
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));
        map.put(User.Schema.ATTR_USERNAME, Arrays.asList(BAD_USER_NAME));
        map.put(User.Schema.ATTR_GIVEN_NAME, Arrays.asList(newGivenName));
        map.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList(newFamilyName));
        map.put(User.Schema.ATTR_PASSWORD, Arrays.asList(newPassword));
        map.put(User.Schema.ATTR_OU, Arrays.asList(ou));
        map.put(User.Schema.ATTR_PHONE_WORK, Arrays.asList(workPhone));
        map.put(User.Schema.ATTR_PHONE_HOME, Arrays.asList(homePhone));
        map.put(User.Schema.ATTR_PHONE_MOBILE, Arrays.asList(mobilePhone));
        map.put(User.Schema.ATTR_ORG_TITLE, Arrays.asList(orgTitle));
        map.put(User.Schema.ATTR_ORG_DEPARTMENT, Arrays.asList(orgDepartment));

        try {
            conn.opCreateObject(map);
            fail("Expected exception: domain not authorized.");
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("404"));
            assertTrue(msg.contains("domain not found"));
        }
    }
}
