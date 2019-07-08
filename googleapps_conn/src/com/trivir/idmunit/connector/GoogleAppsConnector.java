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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trivir.idmunit.connector.api.AliasApi;
import com.trivir.idmunit.connector.api.GroupApi;
import com.trivir.idmunit.connector.api.SendAsApi;
import com.trivir.idmunit.connector.api.UserApi;
import com.trivir.idmunit.connector.api.resource.Alias;
import com.trivir.idmunit.connector.api.resource.Group;
import com.trivir.idmunit.connector.api.resource.SendAs;
import com.trivir.idmunit.connector.api.resource.User;
import com.trivir.idmunit.connector.api.util.RestUtil;
import com.trivir.idmunit.connector.rest.RestClient;
import com.trivir.idmunit.connector.util.*;
import lombok.Getter;
import lombok.Setter;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static com.trivir.idmunit.connector.api.AliasApi.getAlias;
import static com.trivir.idmunit.connector.api.SendAsApi.getSendAs;
import static com.trivir.idmunit.connector.util.EntityConverter.*;
import static com.trivir.idmunit.connector.util.JavaUtil.*;
import static com.trivir.idmunit.connector.util.MiscUtil.removeBlanks;
import static java.net.HttpURLConnection.*;

public class GoogleAppsConnector extends AbstractConnector {

    public static final String SYNTHETIC_ATTR_OBJECT_CLASS = "objectClass";

    public static final String CONFIG_SUPER_USER_EMAIL = "superUserEmail";
    public static final String CONFIG_SERVICE_ACCOUNT_EMAIL = "serviceEmail";
    public static final String CONFIG_P12KEY_FILE = "p12keyFile";
    public static final String CONFIG_PRIVATE_KEY = "privateKey";
    public static final String CONFIG_SCOPES = "scopes";

    public static final String ADMIN_EMAIL = "admin@idmunit.org";

    static final String EMPTY_VAL = "[EMPTY]";

    private static final String[] SCOPES_DEFAULT = new String[]{
        "https://www.googleapis.com/auth/admin.directory.user",
        "https://www.googleapis.com/auth/admin.directory.group",
        "https://www.googleapis.com/auth/admin.directory.group.member",
        "https://www.googleapis.com/auth/admin.directory.orgunit",
        "https://www.googleapis.com/auth/admin.directory.user",
        "https://www.googleapis.com/auth/admin.directory.user.alias",
        "https://www.googleapis.com/auth/admin.directory.user.security",
        "https://www.googleapis.com/auth/admin.directory.userschema",
        "https://www.googleapis.com/auth/apps.licensing",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile",
    };
    private static Logger log = LoggerFactory.getLogger(GoogleAppsConnector.class);

    //Missing scopes?
    // "https://www.googleapis.com/auth/userinfo.email",
    // "https://www.googleapis.com/auth/userinfo.profile",

/*    private static final String[] SCOPES_DEFAULT = new String[]{
            "https://mail.google.com/",
            "https://www.googleapis.com/auth/admin.directory.group",
            "https://www.googleapis.com/auth/admin.directory.group.member",
            "https://www.googleapis.com/auth/admin.directory.group.member.readonly",
            "https://www.googleapis.com/auth/admin.directory.group.readonly",
            "https://www.googleapis.com/auth/admin.directory.orgunit",
            "https://www.googleapis.com/auth/admin.directory.orgunit.readonly",
            "https://www.googleapis.com/auth/admin.directory.user",
            "https://www.googleapis.com/auth/admin.directory.user.alias",
            "https://www.googleapis.com/auth/admin.directory.user.alias.readonly",
            "https://www.googleapis.com/auth/admin.directory.user.readonly",
            "https://www.googleapis.com/auth/admin.directory.user.security",
            "https://www.googleapis.com/auth/admin.directory.userschema",
            "https://www.googleapis.com/auth/admin.directory.userschema.readonly",
            "https://www.googleapis.com/auth/apps.licensing",
            "https://www.googleapis.com/auth/gmail.modify",
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/gmail.settings.basic",
            "https://www.googleapis.com/auth/gmail.settings.sharing"
    };*/

    @Getter
    private RestClient restClient;
    private String serviceAccount;
    private PrivateKey privateKey;

    // For validating passwords:
    @Setter
    @Getter
    private List<String> cookies;
    private HttpsURLConnection conn;

    public void tearDown() {
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        serviceAccount = config.get(CONFIG_SERVICE_ACCOUNT_EMAIL);
        String keyFile = config.get(CONFIG_P12KEY_FILE);
        String key = config.get(CONFIG_PRIVATE_KEY);
        String superUserEmail = config.get(CONFIG_SUPER_USER_EMAIL);
        String scopeStr = config.get(CONFIG_SCOPES);

        if (!isBlank(keyFile, false)) {
            try {
                privateKey = JWTUtil.loadKey(keyFile);
            } catch (IOException e) {
                throw new IdMUnitException("Error loading PKCS12 key file: " + keyFile, e);
            } catch (KeyStoreException e) {
                throw new IdMUnitException("Error loading PKCS12 key file: " + keyFile, e);
            } catch (NoSuchAlgorithmException e) {
                throw new IdMUnitException("Error loading PKCS12 key file: " + keyFile, e);
            } catch (UnrecoverableEntryException e) {
                throw new IdMUnitException("Error loading PKCS12 key file: " + keyFile, e);
            } catch (CertificateException e) {
                throw new IdMUnitException("Error loading PKCS12 key file: " + keyFile, e);
            }
        } else if (!isBlank(key, false)) {
            try {
                privateKey = JWTUtil.pemStringToPrivateKey(key);
            } catch (InvalidKeySpecException e) {
                throw new IdMUnitException("Error decoding privateKey", e);
            }
        } else {
            throw new IdMUnitException("Must specify either privateKey or p12keyFile");
        }

        restClient = RestUtil.newRestClient(serviceAccount, privateKey, superUserEmail, scopeStr);
    }

    public void opCreateObject(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);

        if (isBlank(objectClass)) { //cannot be blank
            throw new IdMUnitException("No objectClass specified.");
        }

        if (Group.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            createGroup(data);
        } else if (User.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);
            if (isBlank(username)) { //cannot be blank
                throw new IdMUnitException("Username must be specified.");
            }

            boolean useFamilyNamePlaceholder = false;
            if (isBlank(ConnectorUtil.getSingleValue(data, User.Schema.ATTR_FAMILY_NAME), false)) {
                useFamilyNamePlaceholder = true;
                data.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList("placeholder-value-delete-me"));
            }

            //TODO: why are fewer attributes supported for create than modify?
            UserApi.insertUser(restClient, mapToUser(data));

            if (useFamilyNamePlaceholder) {
                HashMap<String, Collection<String>> modify = new HashMap<String, Collection<String>>();
                modify.put(User.Schema.ATTR_FAMILY_NAME, Arrays.asList(""));
                modify.put(User.Schema.ATTR_USERNAME, data.get(User.Schema.ATTR_USERNAME));
                modify.put(User.Schema.ATTR_GIVEN_NAME, data.get(User.Schema.ATTR_GIVEN_NAME));
                modifyUser(modify);
            }

        } else if (Alias.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            AliasApi.insertAlias(restClient, mapToAlias(data));
        } else if (SendAs.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            SendAs sendAs = mapToSendAs(data);
            RestClient rest = RestUtil.newRestClient(serviceAccount, privateKey, sendAs.getUserId(), JavaUtil.join(SendAsApi.SCOPES, ","));
            SendAsApi.createSendAs(rest, mapToSendAs(data));
        } else {
            throw new IdMUnitException("objectClass '" + objectClass + "' not supported");
        }
    }

    private void createGroup(Map<String, Collection<String>> data) throws IdMUnitException {
        final String groupEmail = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_EMAIL);
        final String groupName = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_NAME) == null ? groupEmail : ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_NAME);
        final String groupDescription = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_DESCRIPTION) == null ? "" : ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_DESCRIPTION);

        if (isBlank(groupEmail, false)) {
            throw new IdMUnitException("groupEmail must be specified.");
        } else if (!validEmail(groupEmail)) {
            throw new IdMUnitException("groupEmail must be a valid email address.");
        }
        JsonObject json = new JsonObject();
        json.addProperty("email", groupEmail);
        json.addProperty("name", groupName);
        json.addProperty("description", groupDescription);

        RestClient.Response response = restClient.executePost(GroupApi.Path.PATH_ROOT, json);
        if (response.getStatusCode() >= HTTP_BAD_REQUEST) {
            System.err.println(response.getMessageBody());
            String message = JsonUtil.parseError(response.getMessageBody());
            throw new IdMUnitException(String.format("Error unable to create group '%s': %s", groupEmail, message));
        }

    }

    public void opDeleteObject(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);

        if (isBlank(objectClass)) {  //cannot be blank
            throw new IdMUnitException("No objectClass specified.");
        }

        if (Group.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            GroupApi.deleteGroup(restClient, ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_EMAIL));
        } else if (User.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            UserApi.deleteUser(restClient, ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME));
        } else if (Alias.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            AliasApi.deleteAlias(restClient, mapToAlias(data));
        } else if (SendAs.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            SendAsApi.deleteSendAs(restClient, mapToSendAs(data));
        } else {
            throw new IdMUnitException("objectClass '" + objectClass + "' not supported");
        }
    }

    public void opModifyObject(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);

        if (isBlank(objectClass)) { //cannot be blank
            throw new IdMUnitException("No objectClass specified.");
        }

        if (User.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            modifyUser(data);
        } else if (SendAs.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            SendAs sendAs = mapToSendAs(data);
            RestClient rest = RestUtil.newRestClient(serviceAccount, privateKey, sendAs.getUserId(), JavaUtil.join(SendAsApi.SCOPES, ","));
            SendAsApi.updateSendAs(rest, mapToSendAs(data));
        } else {
            throw new IdMUnitException("objectClass '" + objectClass + "' not supported");
        }
    }

    //TODO: Add error handling
    private void modifyUser(Map<String, Collection<String>> data) throws IdMUnitException {
        String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);
        String givenName = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_GIVEN_NAME);
        String familyName = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_FAMILY_NAME);
        String password = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_PASSWORD);
        String suspended = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_SUSPENDED);
        String mustChangePassword = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_CHANGE_PASSWORD);
        String orgUnitPath = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_OU);
        if (isBlank(orgUnitPath)) {
            //Note: both "ou "and "orgUnitPath" map to the Google attribute orgUnitPath in this connector; maintain
            // lookup order for backward compatibility
            orgUnitPath = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_ORG_UNIT_PATH);
            if (!isBlank(orgUnitPath)) {
                //Note: "ou" to be the original attribute name based upon existing tests even thought it's called
                // "orgUnitPath" in Google
                //TODO: Remove "orgUnitPath"
                log.warn("WARN: Attribute [" + User.Schema.ATTR_ORG_UNIT_PATH + "] will be deprecated in favor of [" +
                        User.Schema.ATTR_OU + "] in a future release");
            }
        }
        String orgName = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_ORG_NAME);
        String orgTitle = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_ORG_TITLE);
        String orgDepartment = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_ORG_DEPARTMENT);
        String orgDescription = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_ORG_DESCRIPTION);
        String orgOffice = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_ORG_OFFICE);
        String workPhone = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_PHONE_WORK);
        String homePhone = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_PHONE_HOME);
        String mobilePhone = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_PHONE_MOBILE);
        String strIncludeInGlobalAddressList = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST);

        if (isBlank(username)) { //cannot be blank
            throw new IdMUnitException("Username must be specified.");
        }

        if (isBlank(givenName, false) &&
                isBlank(familyName, false) &&
                isBlank(password, false) &&
                isBlank(suspended, false)) {
            throw new IdMUnitException(String.format("You must specify at least one of %s, %s, %s, or %s.", User.Schema.ATTR_GIVEN_NAME, User.Schema.ATTR_FAMILY_NAME, User.Schema.ATTR_PASSWORD, User.Schema.ATTR_SUSPENDED));
        }

        Boolean includeInGlobalAddressList = null;
        if (!isBlank(strIncludeInGlobalAddressList)) {
            try {
                includeInGlobalAddressList = Boolean.parseBoolean(strIncludeInGlobalAddressList);
            } catch (IllegalArgumentException e) {
                throw new IdMUnitException(String.format("Attribute '%s' have a a boolean value: true or false)", User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST));
            }
        }

        // I'm not sure why this call is here. My best guess is it is to check if the user exists
        UserApi.getUser(restClient, username);

        JsonObject userJson = new JsonObject();

        JsonObject nameInnerObject = new JsonObject();
        if (givenName != null) {
            nameInnerObject.addProperty("givenName", givenName);
        }
        if (familyName != null) {
            nameInnerObject.addProperty("familyName", familyName);
        }
        if (nameInnerObject.has("givenName") || nameInnerObject.has("familyName")) {
            userJson.add("name", nameInnerObject);
        }

        JsonObject orgInnerObject = new JsonObject();

        if (orgName != null) {
            orgInnerObject.addProperty("name", orgName);
        }
        if (orgTitle != null) {
            orgInnerObject.addProperty("title", orgTitle);
        }
        if (orgDepartment != null) {
            orgInnerObject.addProperty("department", orgDepartment);
        }
        if (orgDepartment != null) {
            orgInnerObject.addProperty("description", orgDescription);
        }
        if (orgOffice != null) {
            orgInnerObject.addProperty("location", orgOffice);
        }

        if (orgInnerObject.has("name") ||
                orgInnerObject.has("title") ||
                orgInnerObject.has("department") ||
                orgInnerObject.has("description") ||
                orgInnerObject.has("location")) {
            JsonArray orgArray = new JsonArray();
            orgArray.add(orgInnerObject);
            userJson.add("organizations", orgArray);
        }

        //phone numbers
        JsonArray phoneArray = new JsonArray();
        if (!isBlank(workPhone, false)) {
            JsonObject workObject = new JsonObject();
            workObject.addProperty("type", "work");
            workObject.addProperty("value", workPhone);
            phoneArray.add(workObject);
        }
        if (!isBlank(homePhone, false)) {
            JsonObject homeObject = new JsonObject();
            homeObject.addProperty("type", "home");
            homeObject.addProperty("value", homePhone);
            phoneArray.add(homeObject);
        }
        if (!isBlank(mobilePhone, false)) {
            JsonObject mobileObject = new JsonObject();
            mobileObject.addProperty("type", "mobile");
            mobileObject.addProperty("value", mobilePhone);
            phoneArray.add(mobileObject);
        }
        if (phoneArray.size() > 0) {
            userJson.add("phones", phoneArray);
        }

        if (password != null) {
            userJson.addProperty("password", password);
        }

        if (suspended != null && !"".equals(suspended)) {
            String t = "TRUE";
            boolean value = suspended.equalsIgnoreCase(t);
            userJson.addProperty("suspended", value);
        }

        if (mustChangePassword != null && !"".equals(mustChangePassword)) {
            String t = "TRUE";
            boolean value = mustChangePassword.equalsIgnoreCase(t);
            userJson.addProperty("changePasswordAtNextLogin", value);
        }

        if (orgUnitPath != null && !"".equals(orgUnitPath)) {
            userJson.addProperty(User.Schema.ATTR_ORG_UNIT_PATH, orgUnitPath);
        }

        //set to true, false, or remove property
        userJson.addProperty(User.Schema.ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST, includeInGlobalAddressList);

        String path = String.format(UserApi.Path.PATH_USER, username);
        RestClient.Response response = restClient.executePut(path, userJson);

        if (response.getStatusCode() != HTTP_OK) {
            String message = JsonUtil.parseError(response.getMessageBody());
            throw new IdMUnitException(String.format("Error unable to update user '%s'. '%s'", username, message));
        }
    }

    public void opClearAttr(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);

        if (isBlank(objectClass)) { //cannot be blank
            throw new IdMUnitException("No objectClass specified.");
        }

        if (User.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            clearAttrs(data);
        } else {
            throw new IdMUnitException("objectClass '" + objectClass + "' not supported");
        }
    }

    private void clearAttrs(Map<String, Collection<String>> data) throws IdMUnitException {
        String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);
        data.remove(User.Schema.ATTR_USERNAME);
        JsonObject userJson = new JsonObject();
        JsonObject nameJson = new JsonObject();
        for (String attrName : data.keySet()) {
            if (!User.Schema.CLEARABLE_ATTRS.contains(attrName)) {
                throw new IdMUnitException("Attribute '" + attrName + "' cannot be cleared. Supported attributes are: " + User.Schema.CLEARABLE_ATTRS);
            }
            String attrValue = ConnectorUtil.getSingleValue(data, attrName);
            if ("*".equals(attrValue)) {
                if (attrName.equalsIgnoreCase(User.Schema.ATTR_FAMILY_NAME) || attrName.equalsIgnoreCase(User.Schema.ATTR_GIVEN_NAME)) {
                    nameJson.addProperty(attrName, "");
                } else {
                    userJson.addProperty(attrName, (String)null);
                }
            } else {
                throw new IdMUnitException("You must specify '*' as the attribute value for the removeAttr operation.");
            }
        }
        userJson.add("name", nameJson);
        String path = String.format(UserApi.Path.PATH_USER, username);
        RestClient.Response response = restClient.executePut(path, userJson);
        if (response.getStatusCode() != HTTP_OK) {
            String message = JsonUtil.parseError(response.getMessageBody());
            throw new IdMUnitException(String.format("Error unable to remove attrs for user '%s'. '%s'", username, message));
        }
    }

    public void opRenameObject(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);

        if (isBlank(objectClass)) { //cannot be blank
            throw new IdMUnitException("No objectClass specified.");
        }

        if (Group.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            renameGroup(data);
        } else if (User.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            renameUser(data);
        } else {
            throw new IdMUnitException("objectClass '" + objectClass + "' not supported");
        }
    }

    private void renameGroup(Map<String, Collection<String>> data) throws IdMUnitException {
        final String groupEmail = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_EMAIL);
        final String newGroupEmail = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_NEW_GROUP_EMAIL);
        final String groupName = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_NAME) == null ? groupEmail : ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_NAME);
        final String groupDescription = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_DESCRIPTION) == null ? "" : ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_DESCRIPTION);

        if (isBlank(groupEmail, false) || isBlank(newGroupEmail, false)) {
            throw new IdMUnitException(String.format("Both '%s' and '%s' must be specified.", Group.Schema.ATTR_GROUP_EMAIL, Group.Schema.ATTR_NEW_GROUP_EMAIL));
        } else if (!validEmail(groupEmail) || !validEmail(newGroupEmail)) {
            throw new IdMUnitException(String.format("Both '%s' and '%s' must be valid email addresses.", groupEmail, newGroupEmail));
        }

        Group groupEntry = GroupApi.getGroup(restClient, groupEmail);
        if (groupEntry == null) {
            throw new IdMUnitException(String.format("Error unable to retrieve group '%s', for modification", groupEmail));
        }

        JsonObject json = new JsonObject();
        json.addProperty("email", newGroupEmail);
        json.addProperty("name", groupName);
        json.addProperty("description", groupDescription);

        String path = GroupApi.Path.PATH_ROOT + "/" + groupEmail;
        RestClient.Response response = restClient.executePut(path, json);
        if (response.getStatusCode() != HTTP_OK) {
            String message = JsonUtil.parseError(response.getMessageBody());
            throw new IdMUnitException(String.format("Error unable to create group '%s': %s", groupEmail, message));
        }
    }

    public void opAddGroupMemeber(Map<String, Collection<String>> data) throws IdMUnitException {
        final String groupEmail = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_EMAIL);
        final String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);
        //mapping prevents NullPointerException on call to groupRole.toUpperCase()
        final String groupRole = mapNull(ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_ROLE), "");

        if (isBlank(groupEmail, false) || isBlank(username)) {  //username cannot be blank
            throw new IdMUnitException(String.format("Both '%s' and '%s' must be specified.", Group.Schema.ATTR_GROUP_EMAIL, User.Schema.ATTR_USERNAME));
        } else if (!validEmail(groupEmail) || !validEmail(username)) {
            throw new IdMUnitException(String.format("Both '%s' and '%s' must be valid email addresses.", groupEmail, username));
        }
        String role = groupRole.toUpperCase();

        if (!"OWNER".equals(role) && !"MEMBER".equals(role) && !"MANAGER".equals(role)) {
            throw new IdMUnitException(String.format("Group role: '%s' must be OWNER, MEMBER, or MANAGER.", groupRole));
        }

        //verify group exists
        GroupApi.getGroup(restClient, groupEmail);

        JsonObject memberJson = new JsonObject();
        memberJson.addProperty("email", username);
        if (!isBlank(groupRole, false)) {
            //if (groupRole != null || "".equals(groupRole)) {
            memberJson.addProperty("role", groupRole);
        }

        String path = GroupApi.Path.PATH_ROOT + "/" + groupEmail + "/members";
        RestClient.Response response = restClient.executePost(path, memberJson);

        if (response.getStatusCode() != HTTP_OK) {
            String message = JsonUtil.parseError(response.getMessageBody());
            throw new IdMUnitException(String.format("Error unable to add user '%s' to group '%s'. %s", username, groupEmail, message));
        }

    }

    public void opRemoveGroupMemeber(Map<String, Collection<String>> data) throws IdMUnitException {
        final String groupEmail = ConnectorUtil.getSingleValue(data, Group.Schema.ATTR_GROUP_EMAIL);
        final String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);

        if (isBlank(groupEmail, false) || isBlank(username)) {  //username cannot be blank
            throw new IdMUnitException(String.format("Both '%s' and '%s' must be specified.", Group.Schema.ATTR_GROUP_EMAIL, User.Schema.ATTR_USERNAME));
        } else if (!validEmail(groupEmail) || !validEmail(username)) {
            throw new IdMUnitException(String.format("Both '%s' and '%s' must be valid email addresses.", groupEmail, username));
        }

        GroupApi.getGroup(restClient, groupEmail);

        String path = GroupApi.Path.PATH_ROOT + "/" + groupEmail + "/members/" + username;
        RestClient.Response response = restClient.executeDelete(path);
        if (response.getStatusCode() != HTTP_OK && response.getStatusCode() != HTTP_NO_CONTENT) {
            if (response.getMessageBody() == null || response.getMessageBody().length() == 0) {
                throw new IdMUnitException(String.format("Error %d deleting group member '%s' of group '%s': '%s'",
                        response.getStatusCode(), username, groupEmail, response.getReasonPhrase()));
            } else {
                String msg = JsonUtil.parseError(response.getMessageBody());
                if (!"Resource Not Found: groupKey".equals(msg)) {
                    throw new IdMUnitException(String.format("Error deleting group member '%s' of group '%s': '%s'", username, groupEmail, msg));
                }
            }
        }
    }

    private void renameUser(Map<String, Collection<String>> data) throws IdMUnitException {
        String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);
        String newUsername = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_NEW_USERNAME);

        if (isBlank(username)) { //cannot be blank
            throw new IdMUnitException("Username must be specified.");
        }
        if (isBlank(newUsername)) {  //cannot be blank
            throw new IdMUnitException("newUsername must be specified for rename operations.");
        }

        JsonObject userJson = new JsonObject();
        userJson.addProperty("primaryEmail", newUsername);

        String path = String.format(UserApi.Path.PATH_USER, username);
        RestClient.Response response = restClient.executePut(path, userJson);

        if (response.getStatusCode() != HTTP_OK) {
            String message = JsonUtil.parseError(response.getMessageBody());

            throw new IdMUnitException(String.format("Error unable to update user '%s'. '%s'", username, message));

        }
    }

    public void opValidateObject(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        String objectClass = ConnectorUtil.getSingleValue(data, SYNTHETIC_ATTR_OBJECT_CLASS);
        data.remove(SYNTHETIC_ATTR_OBJECT_CLASS);

        if (isBlank(objectClass)) { //cannot be blank
            throw new IdMUnitException("No objectClass specified.");
        }

        if (Group.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            validateGroup(data);
        } else if (User.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            validateUser(data);
        } else if (Alias.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            validateAlias(data);
        } else if (SendAs.Schema.CLASS_NAME.equalsIgnoreCase(objectClass)) {
            validateSendAs(data);
        } else {
            throw new IdMUnitException("objectClass '" + objectClass + "' not supported");
        }
    }

    public void opValidatePassword(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        String username = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_USERNAME);
        String password = ConnectorUtil.getSingleValue(data, User.Schema.ATTR_PASSWORD);

        if (!validatePassword(username, password)) {
            throw new IdMUnitFailureException("Password validation failed for user: [" + username + "] password: [" + password + "]");
        }
    }

    private void validateGroup(Map<String, Collection<String>> srcAttrs) throws IdMUnitException {
        String groupEmail = ConnectorUtil.getSingleValue(srcAttrs, Group.Schema.ATTR_GROUP_EMAIL);

        if (isBlank(groupEmail, false)) {
            throw new IdMUnitException("groupEmail must be specified.");
        }

        Group groupEntry = GroupApi.getGroup(restClient, groupEmail);
        Map<String, Collection<String>> destAttrs;

        try {
            destAttrs = attrsFromGroup(groupEntry);
        } catch (IdMUnitException e) {
            throw new IdMUnitException("Error retrieving group attributes.", e);
        }

        validateImpl(srcAttrs, destAttrs, Collections.singleton(Group.Schema.ATTR_GROUP_ID));
    }

    private void validateUser(Map<String, Collection<String>> srcAttrs) throws IdMUnitException {
        String username = ConnectorUtil.getSingleValue(srcAttrs, User.Schema.ATTR_USERNAME);
        if (isBlank(username)) {  //cannot be blank
            throw new IdMUnitException("Username must be specified");
        }

        User userEntry = UserApi.getUser(restClient, username);

        if (userEntry == null) {
            throw new IdMUnitFailureException(String.format("Unable to retrieve user '%s'.", username));
        }

        Map<String, Collection<String>> destAttrs = userToMap(userEntry);
        validateImpl(srcAttrs, destAttrs, Collections.singleton(User.Schema.ATTR_USERNAME));
    }

    private void validateImpl(Map<String, Collection<String>> srcAttrs, Map<String, Collection<String>> destAttrs, Collection<String> excludeAttrs) throws IdMUnitException {

        List<String> errors = new ArrayList<String>();

        for (String attrName : srcAttrs.keySet()) {

            if (excludeAttrs.contains(attrName)) {
                //skip excluded attributes
                continue;
            }
            log.info(String.format("%s is being validated...", attrName));

            //TODO: normalizing of non-values should happen in each Resource class (e.g., User)
            //remove null and "" values (non-values); treat whitespace (e.g., " ") as a value

            //ensure Collection implementations are mutable
            Collection<String> expectedValues = toMutableCollection(srcAttrs.get(attrName));
            expectedValues = removeBlanks(expectedValues, false);
            Collection<String> actualValues = toMutableCollection(destAttrs.get(attrName));
            actualValues = removeBlanks(actualValues, false);

            //TODO: sort values so multi-valued comparison output is easier for end user

            if (!isNullOrEmpty(expectedValues) && expectedValues.contains(EMPTY_VAL)) {
                // special case: check to see if dest attr is blank

                if (!isNullOrEmpty(actualValues)) {
                    errors.add("Validation failed: Dest should be empty. Attribute [" + attrName + "] not equal. Expected dest value(s): []. Actual dest value(s): [" + join(actualValues) + "].");
                }
            } else {
                // normal case

                //if not equal...
                if (!(isNullOrEmpty(expectedValues) && isNullOrEmpty(actualValues))) {
                    if (isNullOrEmpty(actualValues) && !isNullOrEmpty(expectedValues)) {
                        //the src has  values and the dest doesn't (src does, dest doesn't)
                        errors.add("Validation failed: Dest has no value(s). Attribute [" + attrName + "] not equal. Expected dest value(s): [" + join(expectedValues) + "]. Actual dest value(s): [].");
                    } else if (isNullOrEmpty(expectedValues) && !isNullOrEmpty(actualValues)) {
                        //the src doesn't have values and the dest does (src doesn't, dest does)
                        errors.add("Validation failed: Src has no value(s). Attribute [" + attrName + "] not equal. Expected dest value(s): []. Actual dest value(s): [" + join(actualValues) + "].");
                    } else if (!expectedValues.containsAll(actualValues) || !actualValues.containsAll(expectedValues)) {
                        //both src and dest have values & sets are not equal
                        errors.add("Validation failed: Attribute [" + attrName + "] not equal. Expected dest value(s): [" + join(expectedValues) + "]. Actual dest value(s): [" + join(actualValues) + "].");
                    }
                }
            }

        }

        handleValidationErrors(errors);
    }

    private void validateAlias(Map<String, Collection<String>> srcMap) throws IdMUnitException {
        final String msgAttrNotPopulated = "Error: %s attribute is empty and should be populated";
        final String msgResourceNotFound = "%s %s not found for %s";

        Alias src = mapToAlias(srcMap);

        String userKey = src.getUserKey();
        String aliasEmail = src.getAlias();
        if (isBlank(userKey)) {
            throw new IdMUnitException(String.format(msgAttrNotPopulated, Alias.Schema.ATTR_USERKEY));
        } else if (isBlank(aliasEmail)) {
            throw new IdMUnitException(String.format(msgAttrNotPopulated, Alias.Schema.ATTR_ALIAS));
        }

        Alias dest = getAlias(restClient, src);
        if (dest == null) {
            throw new IdMUnitFailureException(String.format(msgResourceNotFound, Alias.Schema.CLASS_NAME, aliasEmail, userKey));
        }

        Map<String, Collection<String>> destMap = EntityConverter.aliasToMap(dest);
        validateImpl(srcMap, destMap, Arrays.asList(Alias.Schema.ATTR_USERKEY, Alias.Schema.ATTR_ALIAS));
    }

    private void validateSendAs(Map<String, Collection<String>> srcMap) throws IdMUnitException {
        final String msgAttrNotPopulated = "Error: %s attribute is empty and should be populated";
        final String msgResourceNotFound = "%s %s not found for %s";

        SendAs src = mapToSendAs(srcMap);
        String userId = src.getUserId();
        String sendAsEmail = src.getSendAsEmail();
        if (isBlank(userId)) {
            throw new IdMUnitException(String.format(msgAttrNotPopulated, SendAs.Schema.ATTR_USERID));
        } else if (isBlank(sendAsEmail)) {
            throw new IdMUnitException(String.format(msgAttrNotPopulated, SendAs.Schema.ATTR_SEND_AS_EMAIL));
        }

        SendAs dest = getSendAs(serviceAccount, privateKey, sendAsEmail, src);
        if (dest == null) {
            throw new IdMUnitFailureException(String.format(msgResourceNotFound, SendAs.Schema.CLASS_NAME, sendAsEmail, userId));
        }

        Map<String, Collection<String>> destMap = EntityConverter.sendAsToMap(dest);
        validateImpl(srcMap, destMap, Arrays.asList(SendAs.Schema.ATTR_USERID, SendAs.Schema.ATTR_SEND_AS_EMAIL));
    }

    private static void handleValidationErrors(Collection<String> errors) throws IdMUnitException {
        final String msgValidationFailed = "%s\r\n[%d] error(s) found";

        if (errors.size() > 0) {
            StringBuilder failMessages = new StringBuilder("");
            for (String error : errors) {
                failMessages.append(error);
                failMessages.append("\r\n");
            }
            throw new IdMUnitException(String.format(msgValidationFailed, failMessages.toString(), errors.size()));
        }
    }

    private Map<String, Collection<String>> attrsFromGroup(Group groupEntry) throws IdMUnitException {
        Map<String, Collection<String>> groupAttrs = new HashMap<String, Collection<String>>();

        final String groupName = groupEntry.name;
        final String groupDescription = groupEntry.description;
        final String groupEmail = groupEntry.email;

        groupAttrs.put(Group.Schema.ATTR_GROUP_NAME, Collections.singletonList(groupName));
        groupAttrs.put(Group.Schema.ATTR_GROUP_DESCRIPTION, Collections.singletonList(groupDescription));
        groupAttrs.put(Group.Schema.ATTR_GROUP_EMAIL, Collections.singletonList(groupEmail));

        return groupAttrs;
    }

    private boolean validEmail(String email) {
        final String validEmailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

        return email.matches(validEmailPattern);
    }

    // Returns true if password was correct.
    private boolean validatePassword(String username, String password) throws IdMUnitException {
        String url = "https://accounts.google.com/ServiceLoginAuth";            //URL to post the authentication.

        //NOTE: We've found cookies to unreliable in this test as they are not always present. Instead, we're
        // relying up the title of the login-in page to determine whether the login was successful. If the title
        // of the response page is the same as that of the login-page, we assume the login failed. This isn't
        // fool-proof, but it's definitely more robust that relying on a hard-coded, English-language string.

        try {

            // 1. Turn on cookies. Otherwise, you'll get a enable or disable cookies page
            CookieHandler.setDefault(new CookieManager());

            // 2. Get the login page so that we extract page title and form parameters.
            String loginPage = getPageContent(url);
            System.out.println("Login page: " + loginPage);

            if (isBlank(loginPage)) {
                throw new IdMUnitException("Could not get login page!");
            }

            String loginPageTitle = MiscUtil.getTitleFromHtmlPage(loginPage);
            System.out.println("Login page title: " + loginPageTitle);

            if (isBlank(loginPageTitle)) {
                throw new IdMUnitException("No title in login page!");
            }

            String postParams = getFormParams(loginPage, username, password);

            // 3. Login so we can get the  response page's title.
            String responsePage = sendPost(url, postParams);
            System.out.println("Response page: " + responsePage);

            if (isBlank(responsePage)) {
                throw new IdMUnitException("Did not get a response after logging-in!");
            }

            String responsePageTitle = MiscUtil.getTitleFromHtmlPage(responsePage);
            System.out.println("Response page title: " + loginPageTitle);

            if (isBlank(responsePageTitle)) {
                throw new IdMUnitException("No <title> in response page!");
            }

            // 4. Compare the login and reponse page titles. If they're the same, the login failed.
            return !loginPageTitle.equals(responsePageTitle);

        } catch (IOException ioe) {
            throw new IdMUnitException("Failed to validate password.", ioe);
        }

    }

    private String sendPost(String url, String postParams) throws IOException {
        String userAgent = "Mozilla/5.0";

        URL obj = new URL(url);
        conn = (HttpsURLConnection)obj.openConnection();

        // Acts like a browser
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Host", "accounts.google.com");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        if (!isNullOrEmpty(cookies)) {
            for (String cookie : cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
            }
        }
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Referer", "https://accounts.google.com/ServiceLoginAuth");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

        conn.setDoOutput(true);
        conn.setDoInput(true);

        // Send post request
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(postParams);
        wr.flush();
        wr.close();

        int responseCode = conn.getResponseCode();
        //System.out.println("\nSending 'POST' request to URL : " + url);
        //System.out.println("Post parameters : " + postParams);
        //System.out.println("Response Code : " + responseCode);

        // TODO: better exception to throw here?
        if (responseCode != 200) {
            throw new IOException("Could not get page content to test google apps password! HTTP response code: " + responseCode);
        }

        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        //System.out.println("======" + response.toString());

        return response.toString();
    }

    private String getFormParams(String html, String username, String password) throws UnsupportedEncodingException {

        //System.out.println("Extracting form's data...");


        //TODO: remove jsoup dependency: for now, we're using it to copy all of the input params, and replace Email and Passwd with the passed in user name and password.
        Document doc = Jsoup.parse(html);

        // Google form id
        Element loginform = doc.getElementById("gaia_loginform");
        Elements inputElements = loginform.getElementsByTag("input");
        List<String> paramList = new ArrayList<String>();
        for (Element inputElement : inputElements) {
            String key = inputElement.attr("name");
            String value = inputElement.attr("value");

            if ("Email".equals(key)) {
                value = username;
            } else if (inputElement.attr("id").equals("Passwd-hidden")) {
                value = password;
                key = "Passwd";
            }
            paramList.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
        }

        // build parameters list
        StringBuilder result = new StringBuilder();
        for (String param : paramList) {
            if (result.length() == 0) {
                result.append(param);
            } else {
                result.append("&").append(param);
            }
        }
        return result.toString();
    }

    private String getPageContent(String url) throws IOException, IdMUnitException {
        String userAgent = "Mozilla/5.0";

        URL obj = new URL(url);
        conn = (HttpsURLConnection)obj.openConnection();

        // default is GET
        conn.setRequestMethod("GET");

        conn.setUseCaches(false);

        // act like a browser
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        if (!isNullOrEmpty(cookies)) {
            for (String cookie : cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
            }
        }

        //int responseCode = conn.getResponseCode();
        //System.out.println("\nSending 'GET' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);

        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        //try {
//          Thread.sleep(500);
        //} catch (Exception e) {
//          System.out.println("woke wihle sleeping . .");
        //}

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Get the response cookies
/*      List<String> cookiesTemp = conn.getHeaderFields().get("Set-Cookie");
        if (cookiesTemp == null) {
            // TODO: find out why this is null some times, and put together a solution; For now, we've found retries at least work around the problem.
            throw new IdMUnitException("The Set-Cookie header was null; need to retry on the IdMUnit spreadsheet.");
        }
        setCookies(cookiesTemp);*/

        return response.toString();

    }

}
