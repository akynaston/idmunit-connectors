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

import com.trivir.idmunit.connector.ConfigTests;
import com.trivir.idmunit.connector.GoogleAppsConnector;
import com.trivir.idmunit.connector.api.AliasApi;
import com.trivir.idmunit.connector.api.SendAsApi;
import com.trivir.idmunit.connector.api.UserApi;
import com.trivir.idmunit.connector.api.resource.Alias;
import com.trivir.idmunit.connector.api.resource.SendAs;
import com.trivir.idmunit.connector.api.resource.User;
import org.idmunit.IdMUnitException;
import org.junit.Test;

import java.util.List;

import static com.trivir.idmunit.connector.ConfigTests.TEST_SERVICE_EMAIL;
import static com.trivir.idmunit.connector.ConfigTests.TEST_USERS;
import static com.trivir.idmunit.connector.GoogleAppsConnector.ADMIN_EMAIL;
import static com.trivir.idmunit.connector.api.UserApi.listUsersInDomain;
import static com.trivir.idmunit.connector.util.TestUtil.insertTestUsers;
import static com.trivir.idmunit.connector.util.TestUtil.newTestConnection;

public class ManageTestUsers {

    @Test
    public void resetTestUsers() throws IdMUnitException {
        deleteTestUsers();
        createTestUsers();
    }

    @Test
    public void listAllUsersInDomain() throws IdMUnitException {
        GoogleAppsConnector conn = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        List<User> users = listUsersInDomain(conn.getRestClient(), ConfigTests.TEST_DOMAIN);
        for (User user : users) {
            System.out.println("User: " + user.getPrimaryEmail());
        }
    }

    @Test
    public void deleteAllUsersInDomain() throws IdMUnitException {
        GoogleAppsConnector conn = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        //we may need to exclude this user: "abe@idmunit.org"
        UserApi.deleteUsersInDomain(conn.getRestClient(), ConfigTests.TEST_DOMAIN, null);
    }

    @Test
    public void deleteUsersInDomain() throws IdMUnitException {
        GoogleAppsConnector conn = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        String[] usersToDelete = new String[]{"openicf_create_user@idmunit.org", "openicf_update_user@idmunit.org"};

        for (String user : usersToDelete) {
            UserApi.deleteUser(conn.getRestClient(), user);
        }
    }

    @Test
    public void listAllSendAs() throws IdMUnitException {
        GoogleAppsConnector admin = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        List<User> users = listUsersInDomain(admin.getRestClient(), ConfigTests.TEST_DOMAIN);
        for (User user : users) {
            //TODO: can't login to gmail as admin; don't know why
            if (user.getIsAdmin()) {
                continue;
            }

            GoogleAppsConnector gmail = newTestConnection(user.getPrimaryEmail(), SendAsApi.SCOPES);
            List<SendAs> sendAses = SendAsApi.listSendAs(gmail.getRestClient(), user.getPrimaryEmail());
            System.out.println(user.getPrimaryEmail() + ": " + sendAses.size());
            for (SendAs sendAs : sendAses) {
                System.out.println(sendAs);
            }
        }
    }

    //@Test
    //NOTE: should fail
    public void listAllSendAsAsServiceAccount() throws IdMUnitException {

        String impersonate = TEST_SERVICE_EMAIL;
        //String impersonate = TEST_USERS[0];
        GoogleAppsConnector conn = newTestConnection(impersonate, SendAsApi.SCOPES);

        String user = TEST_USERS[0];
        List<SendAs> sendAses = SendAsApi.listSendAs(conn.getRestClient(), user);
        System.out.println(user + ": " + sendAses.size());
        for (SendAs sendAs : sendAses) {
            System.out.println(sendAs);
        }

    }

    @Test
    public void deleteAllSendAs() throws IdMUnitException {
        GoogleAppsConnector admin = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        List<User> users = listUsersInDomain(admin.getRestClient(), ConfigTests.TEST_DOMAIN);
        for (User user : users) {
            if (user.getIsAdmin()) {
                continue;
            }

            GoogleAppsConnector gmail = newTestConnection(user.getPrimaryEmail(), SendAsApi.SCOPES);

            SendAsApi.deleteAllSendAs(gmail.getRestClient(), user.getPrimaryEmail());
        }
    }

    @Test
    public void listAllAliases() throws IdMUnitException {

        GoogleAppsConnector conn = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        for (String user : TEST_USERS) {

            List<Alias> aliases = AliasApi.listAliases(conn.getRestClient(), user);
            System.out.println("Alias for user " + user + ":");
            for (Alias alias : aliases) {
                System.out.println(alias);
                System.out.println();
            }
        }
    }

    @Test
    public void createTestUsers() throws IdMUnitException {
        GoogleAppsConnector rest = TestUtil.newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        insertTestUsers(rest.getRestClient());
    }

    @Test
    public void deleteTestUsers() throws IdMUnitException {
        GoogleAppsConnector rest = TestUtil.newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        TestUtil.deleteTestUsers(rest.getRestClient());
    }
}
