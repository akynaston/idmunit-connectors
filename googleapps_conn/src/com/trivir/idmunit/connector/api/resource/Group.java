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

//TODO: add normalize method (see SendAs)
public final class Group {
    public String kind;
    public String id;
    public String email;
    public String name;
    public long directMembersCount;
    public String description;
    //public boolean adminCreated;

    public String toString() {
        return "name: " + name + ", email: " + email + ", description: " + description;
    }

    //NOTE: These attributes used to be declared in 2 places: GoogleAppsConnector and in JUnit tests; I aggregated them
    //  here so they're declared only once
    public static final class Schema {

        //case-insensitive
        public static final String CLASS_NAME = "Group";

        public static final String ATTR_GROUP_DESCRIPTION = "groupDescription";
        public static final String ATTR_GROUP_EMAIL = "groupEmail";
        public static final String ATTR_GROUP_ID = "groupId";
        public static final String ATTR_GROUP_MEMBERS = "groupMembers";
        public static final String ATTR_GROUP_NAME = "groupName";
        public static final String ATTR_GROUP_ROLE = "groupRole";
        public static final String ATTR_NEW_GROUP_EMAIL = "newGroupEmail";

        @SuppressWarnings("unused")
        private static final String ATTR_NEW_GROUP_ID = "newGroupId";
    }
}
