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

import com.trivir.idmunit.connector.api.AliasApi;
import com.trivir.idmunit.connector.api.resource.Alias;
import com.trivir.idmunit.connector.util.TestUtil;
import org.idmunit.IdMUnitException;
import org.junit.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.trivir.idmunit.connector.ConfigTests.TEST_DOMAIN;
import static com.trivir.idmunit.connector.GoogleAppsConnector.ADMIN_EMAIL;
import static com.trivir.idmunit.connector.api.AliasApi.hasAlias;
import static com.trivir.idmunit.connector.api.AliasApi.listAliasesEmailOnly;
import static com.trivir.idmunit.connector.util.EntityConverter.aliasToMap;
import static com.trivir.idmunit.connector.util.TestUtil.*;
import static org.junit.Assert.*;

public class TestClassAlias {

    private static final String EMAIL_TEMPLATE = "uncc_alias_%s.idmunit@" + TEST_DOMAIN;

    private GoogleAppsConnector admin;

    @BeforeClass
    public static final void setUpOnce() throws Exception {
        System.out.println("Setting-up tests...");
        GoogleAppsConnector conn = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);
        System.out.println("Setting-up test users...");
        resetTestUsers(conn.getRestClient());
/*        System.out.println(String.format(
                "Waiting for %d seconds for test users to settle...",
                ConfigTests.TEST_WAIT_SECONDS_INIT));
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_INIT);*/
        System.out.println("Setup complete");
    }

    @AfterClass
    public static final void tearDownOnce() throws Exception {
        GoogleAppsConnector conn = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);
        deleteTestUsers(conn.getRestClient());
    }

    @Before
    public void setUp() throws IdMUnitException {
        admin = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);
    }

    @After
    public void tearDown() {
        admin = null;
    }

    @Test
    public void testInsertAlias() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        for (int a = 0; a < ConfigTests.TEST_NUM_ITERATIONS; a++) {
            String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

            Alias alias = Alias.Factory.newAlias(userKey, aliasEmail);
            Map<String, Collection<String>> map = aliasToMap(alias);

            //create alias
            admin.opCreateObject(map);
            List<String> aliases = listAliasesEmailOnly(admin.getRestClient(), userKey);
            assertTrue(aliases.contains(aliasEmail));

            deleteObjectSuppressed(admin, map);
        }
    }

    @Test
    public void testValidateBasicAlias() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

        //create alias
        Alias alias = Alias.Factory.newAlias(userKey, aliasEmail);
        Map<String, Collection<String>> map = aliasToMap(alias);
        admin.opCreateObject(map);
        assertTrue(hasAlias(admin.getRestClient(), userKey, aliasEmail));
        admin.opValidateObject(map);

        //deleteTestUsers
        deleteObjectSuppressed(admin, map);
    }

    @Test
    public void testValidateAlias() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

        //create alias
        Alias alias = Alias.Factory.newAlias(userKey, aliasEmail, userKey);
        Map<String, Collection<String>> map = aliasToMap(alias);
        admin.opCreateObject(map);
        assertTrue(hasAlias(admin.getRestClient(), userKey, aliasEmail));
        admin.opValidateObject(map);

        //deleteTestUsers
        deleteObjectSuppressed(admin, map);
    }

    @Test
    public void testValidateAliasDifferent() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

        //create alias
        Alias alias = Alias.Factory.newAlias(userKey, aliasEmail, userKey);
        Map<String, Collection<String>> map = aliasToMap(alias);
        admin.opCreateObject(map);
        assertTrue(hasAlias(admin.getRestClient(), userKey, aliasEmail));

        alias.setPrimaryEmail("different-primary@idmunit.org");
        try {
            admin.opValidateObject(aliasToMap(alias));
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            if (!(msg.contains("validation failed") &&
                msg.contains("primaryemail") &&
                msg.contains("[1] error(s) found"))) {
                fail();
            }
        } finally {
            //deleteTestUsers
            deleteObjectSuppressed(admin, map);
        }
    }

    @Test
    public void testDeleteAlias() throws IdMUnitException {
        final String useKey = ConfigTests.TEST_USERS[0];

        String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

        Alias alias = Alias.Factory.newAlias(useKey, aliasEmail);
        Map<String, Collection<String>> map = aliasToMap(alias);

        //create alias
        admin.opCreateObject(map);
        List<String> aliases = listAliasesEmailOnly(admin.getRestClient(), useKey);
        assertTrue(aliases.contains(aliasEmail));

        //delete alias
        admin.opDeleteObject(map);

        //NOTE: it can take some time for a delete to take effect
        System.out.println(String.format(
                "Waiting %d seconds for the delete to take effect...",
                ConfigTests.TEST_WAIT_SECONDS_DELETE));
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_DELETE);

        //verify
        aliases = listAliasesEmailOnly(admin.getRestClient(), useKey);
        assertFalse(aliases.contains(aliasEmail));
    }

}
