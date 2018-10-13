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

package com.trivir.idmunit.connector.api.resource;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
//TODO: add normalize method (see SendAs)
public final class User {

    public List<Map<String, String>> addresses = new ArrayList<Map<String, String>>();
    public List<String> aliases = new ArrayList<String>();
    public Boolean changePasswordAtNextLogin = false;
    public String creationTime = "";
    public List<Map<String, String>> emails = new ArrayList<Map<String, String>>();
    public List<Map<String, String>> externalIds = new ArrayList<Map<String, String>>();
    public List<Map<String, String>> ims = new ArrayList<Map<String, String>>();
    public Boolean includeInGlobalAddressList = null;
    public Boolean ipWhitelisted = false;
    public Boolean isAdmin = false;
    public Boolean isDelegatedAdmin = false;
    public String kind;
    public String lastLoginTime = "";
    public Map<String, String> name = new HashMap<String, String>();
    public List<Map<String, String>> organizations = new ArrayList<Map<String, String>>();
    public String orgUnitPath = "";
    public String password = "";
    public List<Map<String, String>> phones = new ArrayList<Map<String, String>>();
    public String primaryEmail = "";
    public List<Map<String, String>> relations = new ArrayList<Map<String, String>>();
    public Boolean suspended = false;

    public String getGivenName() {
        return name.get("givenName");
    }

    public void setGivenName(String givenName) {
        name.put("givenName", givenName);
    }

    public String getFullName() {
        return name.get("fullName");
    }

    public void setFullName(String fullName) {
        name.put("fullName", fullName);
    }

    public String getFamilyName() {
        return name.get("familyName");
    }

    public void setFamilyName(String familyName) {
        name.put("familyName", familyName);
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrgDepartment() {
        final String key = "department";

        return getOrgAttrValue(key);
    }

    public String getOrgTitle() {
        final String key = "title";

        return getOrgAttrValue(key);
    }

    public String getOrgDescription() {
        final String key = "description";

        return getOrgAttrValue(key);
    }

    public String getOrgName() {
        final String key = "name";

        return getOrgAttrValue(key);
    }

    public String getOrgOffice() {
        final String key = "location";

        return getOrgAttrValue(key);
    }

    public String getWorkPhone() {
        final String key = "work";

        return getPhoneAttrValue(key);
    }

    public String getHomePhone() {
        final String key = "home";

        return getPhoneAttrValue(key);
    }

    public String getMobilePhone() {
        final String key = "mobile";

        return getPhoneAttrValue(key);
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    public Boolean getChangePasswordAtNextLogin() {
        return changePasswordAtNextLogin;
    }

    public void setChangePasswordAtNextLogin(Boolean changePasswordAtNextLogin) {
        this.changePasswordAtNextLogin = changePasswordAtNextLogin;
    }

    public String getOrgUnitPath() {
        return orgUnitPath;
    }

    public void setOrgUnitPath(String orgUnitPath) {
        this.orgUnitPath = orgUnitPath;
    }


    //TODO: Add all attributes to toString function.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("primaryEmail : " + primaryEmail + "\n");
        sb.append("kind: " + kind + "\n");
        sb.append("isAdmin: " + isAdmin + "\n");
        sb.append("name: " + name + "\n");
        sb.append("password: " + password + "\n");
        sb.append("aliases: " + aliases + "\n");
        sb.append("address: " + addresses + "\n");
        sb.append("organizations: " + organizations + "\n");
        sb.append("relations: " + relations + "\n");
        sb.append("emails: " + emails + "\n");
        sb.append("phones: " + phones + "\n");
        sb.append("ims: " + ims + "\n");
        sb.append("changePasswordAtNextLogin: " + changePasswordAtNextLogin + "\n");
        sb.append("suspended: " + suspended + "\n");
        sb.append("externalIds: " + externalIds + "\n");
        sb.append("creationTime: " + creationTime + "\n");
        sb.append("lastLoginTime: " + lastLoginTime + "\n");
        sb.append("orgUnitPath: " + orgUnitPath + "\n");
        return sb.toString();
    }

    private String getOrgAttrValue(String key) {

        Iterator<Map<String, String>> itr = organizations.iterator();
        while (itr.hasNext()) {
            Map<String, String> map = itr.next();
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }

        return null;
    }

    private String getPhoneAttrValue(String key) {

        Iterator<Map<String, String>> itr = phones.iterator();
        while (itr.hasNext()) {
            Map<String, String> map = itr.next();
            if (map.containsValue(key)) {
                return map.get("value");
            }
        }

        return null;
    }

    //NOTE: These attributes used to be declared in 2 places: GoogleAppsConnector and in JUnit tests; I aggregated them
    //  here so they're declared only once
    public static final class Schema {

        //case-insensitive
        public static final String CLASS_NAME = "User";

        public static final String ATTR_ALIAS = "alias";
        public static final String ATTR_CHANGE_PASSWORD = "changePasswordAtNextLogin";
        public static final String ATTR_FAMILY_NAME = "familyName";
        public static final String ATTR_GIVEN_NAME = "givenName";
        public static final String ATTR_NEW_USERNAME = "newUsername";
        public static final String ATTR_ORG_DEPARTMENT = "orgDepartment";
        public static final String ATTR_ORG_DESCRIPTION = "orgDescription";
        public static final String ATTR_ORG_NAME = "orgName";
        public static final String ATTR_ORG_OFFICE = "orgOffice";
        public static final String ATTR_ORG_TITLE = "orgTitle";
        public static final String ATTR_OU = "ou";
        public static final String ATTR_PASSWORD = "password";
        public static final String ATTR_PHONE_HOME = "homePhone";
        public static final String ATTR_PHONE_MOBILE = "mobilePhone";
        public static final String ATTR_PHONE_WORK = "workPhone";
        public static final String ATTR_SUSPENDED = "suspended";
        public static final String ATTR_USERNAME = "username";
        public static final String ATTR_ORG_UNIT_PATH = "orgUnitPath";
        public static final String ATTR_INCLUDE_IN_GLOBAL_ADDRESS_LIST = "includeInGlobalAddressList";

        //clearable attrs
        private static final String ATTR_PHONES = "phones";
        private static final String ATTR_ORGANIZATIONS = "organizations";
        @SuppressWarnings("checkstyle:DeclarationOrder")
        public static final Set CLEARABLE_ATTRS;

        static {
            Set<String> clearAttrs = new HashSet<String>();
            clearAttrs.add(ATTR_FAMILY_NAME);
            clearAttrs.add(ATTR_GIVEN_NAME);
            clearAttrs.add(ATTR_ORGANIZATIONS);
            clearAttrs.add(ATTR_PHONES);
            CLEARABLE_ATTRS = clearAttrs;
        }

        //unused attributes
        //TODO: delete or add to the readme.txt file
        @SuppressWarnings("unused")
        private static final String ATTR_ADDRESS = "address";
        @SuppressWarnings("unused")
        private static final String ATTR_EMAIL_PERMISSION = "emailPermission";
        @SuppressWarnings("unused")
        private static final String ATTR_EMAIL_WORK = "workEmail";
        @SuppressWarnings("unused")
        private static final String ATTR_QUERY = "query";
        @SuppressWarnings("unused")
        private static final String ATTR_EMPLOYEE_ID = "employeeId";
    }
}
