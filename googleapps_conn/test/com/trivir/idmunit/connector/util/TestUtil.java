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
import com.trivir.idmunit.connector.api.UserApi;
import com.trivir.idmunit.connector.api.resource.User;
import com.trivir.idmunit.connector.rest.RestClient;
import org.idmunit.IdMUnitException;

import java.util.*;

import static com.trivir.idmunit.connector.GoogleAppsConnector.*;
import static com.trivir.idmunit.connector.api.UserApi.deleteUser;
import static com.trivir.idmunit.connector.api.UserApi.insertUser;
import static com.trivir.idmunit.connector.util.JavaUtil.checkNotNull;

public class TestUtil {

    public static String newUniqueEmailAddress(String template) {
        return String.format(template, System.nanoTime());
    }

    public static List<String> newUniqueEmailAddresses(String template, int num) {
        List<String> aliases = new ArrayList<String>(num);
        for (int a = 0; a < num; a++) {
            aliases.add(newUniqueEmailAddress(template));
        }

        return aliases;
    }

    public static void waitTimeSeconds(int multiplier) {
        if (multiplier < 1) {
            multiplier = 1;
        }
        try {
            Thread.sleep(1000 * multiplier);                 //1000 milliseconds is one second.
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    //TODO: move credentials to a body file and read them in
    public static GoogleAppsConnector newTestConnection(String superUserEmail, Collection<String> scopes) throws IdMUnitException {
        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, superUserEmail);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, ConfigTests.TEST_SERVICE_EMAIL);
        config.put(CONFIG_PRIVATE_KEY, ConfigTests.TEST_PRIVATE_KEY);
        config.put(CONFIG_SCOPES, JavaUtil.join(scopes, ","));
        conn.setup(config);
        return conn;

    }

    public static RestClient newRestClient(String superUserEmail, Collection<String> scopes) throws IdMUnitException {
        return newTestConnection(superUserEmail, scopes).getRestClient();
    }

    //return == exception thrown?
    public static boolean deleteObjectSuppressed(GoogleAppsConnector conn, Map<String, Collection<String>> map) {
        boolean thrown;
        try {
            conn.opDeleteObject(map);
            thrown = true;
        } catch (IdMUnitException e) {
            thrown = false;
        }

        return thrown;
    }

    public static void resetTestUsers(RestClient rest) throws IdMUnitException {
        deleteTestUsers(rest);
        insertTestUsers(rest);
    }

    public static void deleteTestUsers(RestClient rest) {
        checkNotNull("rest", rest);

        for (int u = 0; u < ConfigTests.TEST_USERS.length; u++) {
            try {
                deleteUser(rest, ConfigTests.TEST_USERS[u]);
            } catch (IdMUnitException e) {
                //ignore
            }
        }
    }

    public static final void insertTestUsers(RestClient rest) throws IdMUnitException {
        checkNotNull("rest", rest);

        for (int u = 0; u < ConfigTests.TEST_USERS.length; u++) {
            User user = UserApi.Factory.newUser(ConfigTests.TEST_USERS[u],
                "uncc" + u,
                "idmunit",
                "Data1234");
            user.setOrgUnitPath("/UNCC");

            insertUser(rest, user);
        }
    }
}
