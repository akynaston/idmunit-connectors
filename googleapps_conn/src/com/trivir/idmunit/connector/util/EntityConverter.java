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

package com.trivir.idmunit.connector.util;

import com.trivir.idmunit.connector.api.resource.Alias;
import com.trivir.idmunit.connector.api.resource.SendAs;
import com.trivir.idmunit.connector.api.resource.SmtpMsa;
import com.trivir.idmunit.connector.api.resource.User;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.ConnectorUtil;

import java.util.*;

import static com.trivir.idmunit.connector.GoogleAppsConnector.SYNTHETIC_ATTR_OBJECT_CLASS;
import static com.trivir.idmunit.connector.api.resource.SendAs.Schema.*;
import static com.trivir.idmunit.connector.api.resource.SmtpMsa.Factory.newSmtpMsa;
import static com.trivir.idmunit.connector.api.resource.SmtpMsa.Schema.*;
import static com.trivir.idmunit.connector.util.JavaUtil.*;

public class EntityConverter {

    public static Alias mapToAlias(Map<String, Collection<String>> data) throws IdMUnitException {
        if (isNullOrEmpty(data)) {
            return new Alias();
        }

        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        if (objectClass != null) {
            if (!Alias.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
                throw new IllegalArgumentException(String.format("Attribute '%s' must have value '%s'", SYNTHETIC_ATTR_OBJECT_CLASS, Alias.Schema.CLASS_NAME));
            }
            data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);
        }

        return Alias.Factory.newAlias(
            ConnectorUtil.getSingleValue(data, Alias.Schema.ATTR_USERKEY),
            ConnectorUtil.getSingleValue(data, Alias.Schema.ATTR_ALIAS),
            ConnectorUtil.getSingleValue(data, Alias.Schema.ATTR_PRIMARY_EMAIL));
    }

    //TODO: flush out to handle all User attributes
    public static User mapToUser(Map<String, Collection<String>> data) throws IdMUnitException {
        if (isNullOrEmpty(data)) {
            return new User();
        }

        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        if (objectClass != null) {
            if (!User.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
                throw new IllegalArgumentException(String.format("Attribute '%s' must have value '%s'", SYNTHETIC_ATTR_OBJECT_CLASS, User.Schema.CLASS_NAME));
            }
            data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);
        }

        String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);
        String givenName = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_GIVEN_NAME);
        String familyName = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_FAMILY_NAME);
        String password = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_PASSWORD);
        String orgUnitPath = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_ORG_UNIT_PATH);
        String strIncludeInGlobalAddressList = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST);
        Boolean includeInGlobalAddressList = null;
        if (!isBlank(strIncludeInGlobalAddressList)) {
            try {
                includeInGlobalAddressList = Boolean.parseBoolean(strIncludeInGlobalAddressList);
            } catch (IllegalArgumentException e) {
                throw new IdMUnitException(String.format("Attribute '%s' have a a boolean value: true or false)", User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST));
            }
        }

        User user = new User();
        user.setPrimaryEmail(username);
        user.setGivenName(givenName);
        user.setFamilyName(familyName);
        user.setPassword(password);
        user.setOrgUnitPath(orgUnitPath);
        user.setIncludeInGlobalAddressList(includeInGlobalAddressList);

        return user;
    }

    public static Map<String, Collection<String>> aliasToMap(Alias alias) {
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

        if (alias != null) {
            alias.normalize();
            map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Collections.singletonList(Alias.Schema.CLASS_NAME));
            if (alias.getUserKey() != null) {
                map.put(Alias.Schema.ATTR_USERKEY, Collections.singletonList(alias.getUserKey()));
            }
            if (alias.getAlias() != null) {
                map.put(Alias.Schema.ATTR_ALIAS, Collections.singletonList(alias.getAlias()));
            }
            if (alias.getPrimaryEmail() != null) {
                map.put(Alias.Schema.ATTR_PRIMARY_EMAIL, Collections.singletonList(alias.getPrimaryEmail()));
            }
        }

        return map;
    }

    public static SendAs mapToSendAs(Map<String, Collection<String>> data) throws IdMUnitException {
        if (isNullOrEmpty(data)) {
            return new SendAs();
        }

        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        if (objectClass != null) {
            if (!SendAs.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
                throw new IllegalArgumentException(String.format("Attribute '%s' must have value '%s'", SYNTHETIC_ATTR_OBJECT_CLASS, SendAs.Schema.CLASS_NAME));
            }
            data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);
        }

        String userId = mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_USERID), null);
        String sendAsEmail = mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_SEND_AS_EMAIL), null);
        String displayName = mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_DISPLAY_NAME), null);
        String replyToAddress = mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_REPLY_TO_ADDRESS), null);
        String signature = mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_SIGNATURE), null);
        String verificationStatus = mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_VERIFICATION_STATUS), null);
        Boolean isDefault = Boolean.parseBoolean(mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_IS_DEFAULT), null));
        Boolean isPrimary = Boolean.parseBoolean(mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_IS_PRIMARY), null));

        Boolean treatAsAlias;
        String s = mapBlank(ConnectorUtil.getSingleValue(data, SendAs.Schema.ATTR_TREAT_AS_ALIAS), null);
        treatAsAlias = (isBlank(s)) ? Boolean.TRUE : Boolean.parseBoolean(s);

        SmtpMsa smtpMsa = null;
        //smtpHost is required if Smtp attributes are present
        String smtpHost = mapBlank(ConnectorUtil.getSingleValue(data, ATTR_HOST), null);
        if (smtpHost != null) {
            String smtpPassword = mapBlank(ConnectorUtil.getSingleValue(data, SmtpMsa.Schema.ATTR_PASSWORD), null);
            String smtpPort = mapBlank(ConnectorUtil.getSingleValue(data, SmtpMsa.Schema.ATTR_PORT), null);
            String smtpSecurityMode = mapBlank(ConnectorUtil.getSingleValue(data, SmtpMsa.Schema.ATTR_SECURITY_MODE), null);
            String smtpUsername = mapBlank(ConnectorUtil.getSingleValue(data, SmtpMsa.Schema.ATTR_USERNAME), null);

            smtpMsa = newSmtpMsa(
                smtpHost,
                smtpPort,
                smtpUsername,
                smtpPassword,
                smtpSecurityMode);
        }

        return SendAs.Factory.newSendAs(
            userId,
            displayName,
            sendAsEmail,
            replyToAddress,
            signature,
            verificationStatus,
            isDefault,
            isPrimary,
            treatAsAlias,
            smtpMsa);
    }

    public static Map<String, Collection<String>> sendAsToMap(SendAs sendAs) {
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

        if (sendAs != null) {
            sendAs.normalize();
            map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Collections.singletonList(SendAs.Schema.CLASS_NAME));
            if (sendAs.getUserId() != null) {
                map.put(ATTR_USERID, Collections.singletonList(sendAs.getUserId()));
            }
            if (sendAs.getSendAsEmail() != null) {
                map.put(ATTR_SEND_AS_EMAIL, Collections.singletonList(sendAs.getSendAsEmail()));
            }
            if (sendAs.getReplyToAddress() != null) {
                map.put(ATTR_REPLY_TO_ADDRESS, Collections.singletonList(sendAs.getReplyToAddress()));
            }
            if (sendAs.getSignature() != null) {
                map.put(ATTR_SIGNATURE, Collections.singletonList(sendAs.getSignature()));
            }
            if (sendAs.getDisplayName() != null) {
                map.put(ATTR_DISPLAY_NAME, Collections.singletonList(sendAs.getDisplayName()));
            }
            if (sendAs.getVerificationStatus() != null) {
                map.put(ATTR_VERIFICATION_STATUS, Collections.singletonList(sendAs.getVerificationStatus()));
            }
            if (sendAs.getIsDefault() != null) {
                map.put(ATTR_IS_DEFAULT, Collections.singletonList(sendAs.getIsDefault().toString()));
            }
            if (sendAs.getIsPrimary() != null) {
                map.put(ATTR_IS_PRIMARY, Collections.singletonList(sendAs.getIsPrimary().toString()));
            }
            if (sendAs.getTreatAsAlias() != null) {
                map.put(ATTR_TREAT_AS_ALIAS, Collections.singletonList(sendAs.getTreatAsAlias().toString()));
            }
            map.putAll(smtpMsaToMap(sendAs.getSmtpMsa()));
        }

        return map;
    }

    private static Map<String, Collection<String>> smtpMsaToMap(SmtpMsa smtpMsa) {
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

        if (smtpMsa != null) {
            smtpMsa.normalize();
            if (smtpMsa.getHost() != null) {
                map.put(ATTR_HOST, Collections.singletonList(smtpMsa.getHost()));
            }
            if (smtpMsa.getPort() != null) {
                map.put(ATTR_PORT, Collections.singletonList(smtpMsa.getPort()));
            }
            if (smtpMsa.getUsername() != null) {
                map.put(ATTR_USERNAME, Collections.singletonList(smtpMsa.getUsername()));
            }
            if (smtpMsa.getPassword() != null) {
                map.put(ATTR_PASSWORD, Collections.singletonList(smtpMsa.getPassword()));
            }
            if (smtpMsa.getSecurityMode() != null) {
                map.put(ATTR_SECURITY_MODE, Collections.singletonList(smtpMsa.getSecurityMode()));
            }
        }

        return map;
    }

/*    public static Map<String, Collection<String>> toMap(String username, User user) {
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

        map.put(SYNTHETIC_ATTR_OBJECT_CLASS, Arrays.asList(User.Schema.CLASS_NAME));

        if (!isBlank(username)) {
            map.put(User.Schema.ATTR_USERNAME, Collections.singleton(username));
        }

        if (user != null) {
            if (!isBlank(user.getGivenName())) {
                map.put(User.Schema.ATTR_GIVEN_NAME, Collections.singleton(user.getGivenName()));
            }
            if (!isBlank(user.getOrgDepartment())) {
                map.put(User.Schema.ATTR_ORG_DEPARTMENT, Collections.singleton(user.getOrgDepartment()));
            }
            if (!isBlank(user.getFamilyName())) {
                map.put(User.Schema.ATTR_FAMILY_NAME, Collections.singleton(user.getFamilyName()));
            }
            if (!isBlank(user.getPassword())) {
                map.put(User.Schema.ATTR_PASSWORD, Collections.singleton(user.getPassword()));
            }
            if (!isBlank(user.getOrgUnitPath())) {
                map.put(User.Schema.ATTR_OU, Collections.singleton(user.getOrgUnitPath()));
            }
            if (!isBlank(user.getOrgTitle())) {
                map.put(User.Schema.ATTR_ORG_TITLE, Collections.singleton(user.getOrgTitle()));
            }
            if (!isBlank(user.getHomePhone())) {
                map.put(User.Schema.ATTR_PHONE_HOME, Collections.singleton(user.getHomePhone()));
            }
            if (!isBlank(user.getMobilePhone())) {
                map.put(User.Schema.ATTR_PHONE_MOBILE, Collections.singleton(user.getMobilePhone()));
            }
            if (!isBlank(user.getWorkPhone())) {
                map.put(User.Schema.ATTR_PHONE_WORK, Collections.singleton(user.getWorkPhone()));
            }
            if (user.getChangePasswordAtNextLogin() != null) {
                map.put(User.Schema.ATTR_CHANGE_PASSWORD, Collections.singleton(user.getChangePasswordAtNextLogin().toString()));
            }
            if (user.getSuspended() != null) {
                map.put(User.Schema.ATTR_SUSPENDED, Collections.singleton(user.getSuspended().toString()));
            }

            Collection<String> aliases = (user == null) ? new LinkedList() : user.aliases;
            map.put(User.Schema.ATTR_ALIAS, aliases);
        }

        return map;
    }*/

    //TODO: review for accuracy; merge with toMap(String username, User user)
    public static Map<String, Collection<String>> userToMap(User userEntry) throws IdMUnitFailureException {
        Map<String, Collection<String>> userAttrs = new HashMap<String, Collection<String>>();

        final String givenName = userEntry.getGivenName();
        final String familyName = userEntry.getFamilyName();
        final String suspended = userEntry.suspended.toString();
        final String changePasswordAtNextLogin = userEntry.changePasswordAtNextLogin.toString();
        final Boolean includeInGobalAddressList = userEntry.getIncludeInGlobalAddressList();
        final String strIncludeInGlobalAddressList = (includeInGobalAddressList == null) ? null : includeInGobalAddressList.toString();

        userAttrs.put(User.Schema.ATTR_GIVEN_NAME, Collections.singletonList(givenName));
        userAttrs.put(User.Schema.ATTR_FAMILY_NAME, Collections.singletonList(familyName));
        userAttrs.put(User.Schema.ATTR_SUSPENDED, Collections.singletonList(suspended));
        userAttrs.put(User.Schema.ATTR_CHANGE_PASSWORD, Collections.singletonList(changePasswordAtNextLogin));
        userAttrs.put(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, Collections.singletonList(strIncludeInGlobalAddressList));

        // check optional Attributes
        List<String> aliases = userEntry.aliases;
        if (!isNullOrEmpty(aliases)) {
            //if (aliases != null || !aliases.isEmpty()) {
            userAttrs.put(User.Schema.ATTR_ALIAS, aliases);
        }

        final List<Map<String, String>> organizations = userEntry.organizations;
        final List<Map<String, String>> phones = userEntry.phones;
        final List<Map<String, String>> employeeId = userEntry.externalIds;
        final String ou = userEntry.orgUnitPath;

        if (!phones.isEmpty()) {
            ArrayList<String> workList = new ArrayList<String>();
            ArrayList<String> homeList = new ArrayList<String>();
            ArrayList<String> mobileList = new ArrayList<String>();

            for (Map<String, String> map : phones) {
                String type = map.get("type");
                if (type != null) {
                    if ("work".equalsIgnoreCase(type)) {
                        String number = map.get("value");
                        if (number != null) {
                            workList.add(number);
                        }
                    } else if ("home".equalsIgnoreCase(type)) {
                        String number = map.get("value");
                        if (number != null) {
                            homeList.add(number);
                        }
                    } else if ("mobile".equalsIgnoreCase(type)) {
                        String number = map.get("value");
                        if (number != null) {
                            mobileList.add(number);
                        }
                    }
                }
            }
            if (!workList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_PHONE_WORK, workList);
            }
            if (!homeList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_PHONE_HOME, homeList);
            }
            if (!mobileList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_PHONE_MOBILE, mobileList);
            }

        }

        if (!organizations.isEmpty()) {

            ArrayList<String> titleList = new ArrayList<String>();
            ArrayList<String> nameList = new ArrayList<String>();
            ArrayList<String> departmentList = new ArrayList<String>();
            ArrayList<String> locationList = new ArrayList<String>();
            ArrayList<String> descriptionList = new ArrayList<String>();

            for (Map<String, String> map : organizations) {
                String title = map.get("title");
                String name = map.get("name");
                String department = map.get("department");
                String location = map.get("location");
                String description = map.get("description");

                if (title != null) {
                    titleList.add(title);
                }
                if (name != null) {
                    nameList.add(name);
                }
                if (department != null) {
                    departmentList.add(department);
                }
                if (location != null) {
                    locationList.add(location);
                }
                if (description != null) {
                    descriptionList.add(description);
                }
            }
            if (!titleList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_ORG_TITLE, titleList);
            }
            if (!nameList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_ORG_NAME, nameList);
            }
            if (!departmentList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_ORG_DEPARTMENT, departmentList);
            }
            if (!locationList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_ORG_OFFICE, locationList);
            }
            if (!descriptionList.isEmpty()) {
                userAttrs.put(User.Schema.ATTR_ORG_DESCRIPTION, descriptionList);
            }
        }

        // employee could have multiple id
        if (!isNullOrEmpty(employeeId)) {
            ArrayList<String> idList = new ArrayList<String>();
            for (Map<String, String> map : employeeId) {
                String id = map.get("value");
                if (id != null) {
                    idList.add(id);
                }
            }
            if (!idList.isEmpty()) {
                //TODO: is this right? it looks like a bug
                userAttrs.put(User.Schema.ATTR_OU, idList);
            }
            //userAttrs.put(ATTR_EMPLOYEE_ID, new ArrayList(){{add(employeeId);}});
        }
        if (!isBlank(ou)) {
            userAttrs.put(User.Schema.ATTR_OU, Collections.singletonList(ou));
        }

        return userAttrs;

    }
}
