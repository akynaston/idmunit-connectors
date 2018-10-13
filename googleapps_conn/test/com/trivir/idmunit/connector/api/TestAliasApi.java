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

package com.trivir.idmunit.connector.api;

import com.trivir.idmunit.connector.ConfigTests;
import com.trivir.idmunit.connector.api.resource.Alias;
import com.trivir.idmunit.connector.rest.RestClient;
import com.trivir.idmunit.connector.util.TestUtil;
import org.idmunit.IdMUnitException;
import org.junit.*;

import java.util.Collection;
import java.util.List;

import static com.trivir.idmunit.connector.ConfigTests.TEST_DOMAIN;
import static com.trivir.idmunit.connector.GoogleAppsConnector.ADMIN_EMAIL;
import static com.trivir.idmunit.connector.api.AliasApi.*;
import static com.trivir.idmunit.connector.util.TestUtil.*;
import static org.junit.Assert.*;

public class TestAliasApi {

    private static final String EMAIL_TEMPLATE = "uncc_alias_%s.idmunit@" + TEST_DOMAIN;
    private RestClient admin;

    @BeforeClass
    public static final void setUpOnce() throws Exception {
        System.out.println("Setting-up tests...");
        RestClient rest = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES).getRestClient();
        System.out.println("Setting-up test users...");
        resetTestUsers(rest);
        //waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_INIT);
        System.out.println("Setup complete");
    }

    @AfterClass
    public static final void tearDownOnce() throws Exception {
        RestClient rest = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES).getRestClient();
        deleteTestUsers(rest);
    }

    @Before
    public void setUp() throws IdMUnitException {
        admin = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES).getRestClient();
    }

    @After
    public void tearDown() {
        admin = null;
    }

    @Test
    public void testListAliasUserDoesNotExist() throws IdMUnitException {
        assertTrue(listAliases(admin, ConfigTests.TEST_USER_DOES_NOT_EXIST).isEmpty());
    }

    @Test
    public void testDeleteAliasUserDoesNotExist() throws IdMUnitException {
        String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
        assertFalse(deleteAlias(admin, ConfigTests.TEST_USER_DOES_NOT_EXIST, email));
        assertFalse(deleteAlias(admin, Alias.Factory.newAlias(ConfigTests.TEST_USER_DOES_NOT_EXIST, email)));
    }

    @Test
    public void testDeleteAliasDoesNotExist() throws IdMUnitException {
        final String useKey = ConfigTests.TEST_USERS[0];

        String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
        assertFalse(deleteAlias(admin, useKey, email));
        assertFalse(deleteAlias(admin, Alias.Factory.newAlias(useKey, email)));
    }

    @Test
    public void testDeleteAllAliasesUserDoesNotExist() throws IdMUnitException {
        assertFalse(deleteAllAliases(admin, ConfigTests.TEST_USER_DOES_NOT_EXIST));
    }

    @Test
    public void testListAliases() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        Collection<String> newEmails = newUniqueEmailAddresses(EMAIL_TEMPLATE, ConfigTests.TEST_NUM_ITERATIONS);
        for (String email : newEmails) {
            insertAlias(admin, Alias.Factory.newAlias(userKey, email));
        }

        List<Alias> aliases = listAliases(admin, userKey);
        boolean found = false;
        for (Alias alias : aliases) {
            if (newEmails.contains(alias.getAlias())) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        //cleanup
        deleteAllAliases(admin, userKey);
    }

    @Test
    public void testHasAliases() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        Collection<String> newEmails = newUniqueEmailAddresses(EMAIL_TEMPLATE, ConfigTests.TEST_NUM_ITERATIONS);
        for (String email : newEmails) {
            insertAlias(admin, Alias.Factory.newAlias(userKey, email));
        }

        assertTrue(hasAliases(admin, userKey, newEmails));
    }

    @Test
    public void testListAliasEmailOnly() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        Collection<String> newEmails = newUniqueEmailAddresses(EMAIL_TEMPLATE, ConfigTests.TEST_NUM_ITERATIONS);
        for (String email : newEmails) {
            insertAlias(admin, Alias.Factory.newAlias(userKey, email));
        }

        List<String> aliasEmails = listAliasesEmailOnly(admin, userKey);
        assertTrue(aliasEmails.containsAll(newEmails));

        //cleanup
        deleteAllAliases(admin, userKey);
    }

    @Test
    public void testInsertAliasUserDoesNotExist() throws IdMUnitException {
        String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

        Alias alias = Alias.Factory.newAlias(ConfigTests.TEST_USER_DOES_NOT_EXIST, aliasEmail);

        try {

            //create alias
            insertAlias(admin, alias);
            fail();
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("notfound"));
            assertTrue(msg.contains("userkey"));
        }
    }

    @Test
    public void testInsertAliasInvalidEmail() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        Alias alias = Alias.Factory.newAlias(userKey, "notanemailaddress");

        try {

            //create alias
            insertAlias(admin, alias);
            fail();
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("400"));
            assertTrue(msg.contains("invalid input"));
            assertTrue(msg.contains("alias_email"));
        }
    }

    @Test
    public void testInsertAlias() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        for (int a = 0; a < ConfigTests.TEST_NUM_ITERATIONS; a++) {
            String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

            Alias alias = Alias.Factory.newAlias(userKey, aliasEmail, userKey);

            //delete alias (if exists)
            deleteAlias(admin, userKey, aliasEmail);
            List<String> aliases = listAliasesEmailOnly(admin, userKey);
            assertFalse(aliases.contains(aliasEmail));

            //create alias
            Alias newAlias = insertAlias(admin, alias);
            assertEquals(alias, newAlias);

            //cleanup
            deleteAlias(admin, alias);
        }
    }

    @Test
    public void testInsertAliasString() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        for (int a = 0; a < ConfigTests.TEST_NUM_ITERATIONS; a++) {
            String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

            //delete alias (if exists)
            deleteAlias(admin, userKey, aliasEmail);
            List<String> aliases = listAliasesEmailOnly(admin, userKey);
            assertFalse(aliases.contains(aliasEmail));

            //create alias
            insertAlias(admin, userKey, aliasEmail);
            aliases = listAliasesEmailOnly(admin, userKey);
            assertTrue(aliases.contains(aliasEmail));

            //cleanup
            deleteAlias(admin, userKey, aliasEmail);
        }
    }

    @Test
    public void testDeleteAlias() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

        Alias alias = Alias.Factory.newAlias(userKey, aliasEmail);

        //insert alias
        insertAlias(admin, alias);
        List<String> aliases = listAliasesEmailOnly(admin, userKey);
        assertTrue(aliases.contains(aliasEmail));

        //delete alias
        deleteAlias(admin, alias);

        //NOTE: it can take some time for a delete to take effect
        System.out.println(String.format(
                "Waiting %d seconds for the delete to take effect...",
                ConfigTests.TEST_WAIT_SECONDS_DELETE));
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_DELETE);

        //verify
        aliases = listAliasesEmailOnly(admin, userKey);
        assertFalse(aliases.contains(aliasEmail));
    }

    @Test
    public void testDeleteAliasString() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        String aliasEmail = TestUtil.newUniqueEmailAddress(EMAIL_TEMPLATE);

        //insert alias
        insertAlias(admin, userKey, aliasEmail);
        List<String> aliases = listAliasesEmailOnly(admin, userKey);
        assertTrue(aliases.contains(aliasEmail));

        //delete alias
        deleteAlias(admin, userKey, aliasEmail);

        //NOTE: it can take some time for a delete to take effect
        System.out.println(String.format(
                "Waiting %d seconds for the delete to take effect...",
                ConfigTests.TEST_WAIT_SECONDS_DELETE));
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_DELETE);

        //verify
        aliases = listAliasesEmailOnly(admin, userKey);
        assertFalse(aliases.contains(aliasEmail));
    }

    @Test
    public void testDelteAllAliases() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        Collection<String> newAliases = newUniqueEmailAddresses(EMAIL_TEMPLATE, ConfigTests.TEST_NUM_ITERATIONS);
        for (String newAlias : newAliases) {
            insertAlias(admin, userKey, newAlias);
        }
        List<String> aliases = listAliasesEmailOnly(admin, userKey);
        assertTrue(aliases.containsAll(newAliases));

        deleteAllAliases(admin, userKey);

        //NOTE: it can take some time for a delete to take effect
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_DELETE);

        //verify
        aliases = listAliasesEmailOnly(admin, userKey);
        assertFalse(aliases.containsAll(newAliases));
    }

    @Test
    public void testHasAlias() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        String newEmail = newUniqueEmailAddress(EMAIL_TEMPLATE);

        //add alias &  verify
        insertAlias(admin, userKey, newEmail);
        assertTrue(AliasApi.listAliasesEmailOnly(admin, userKey).contains(newEmail));

        //verify has Alias
        assertTrue(hasAlias(admin, userKey, newEmail));
        assertTrue(hasAlias(admin, Alias.Factory.newAlias(userKey, newEmail)));

        //cleanup
        deleteAlias(admin, userKey, newEmail);
    }

    @Test
    public void testHasAliasDoesNotExist() throws IdMUnitException {
        final String userKey = ConfigTests.TEST_USERS[0];

        String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
        assertFalse(hasAlias(admin, userKey, email));
        assertFalse(hasAlias(admin, Alias.Factory.newAlias(userKey, email)));
    }

}
