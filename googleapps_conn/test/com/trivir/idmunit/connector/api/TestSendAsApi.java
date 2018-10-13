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
import com.trivir.idmunit.connector.api.resource.SendAs;
import com.trivir.idmunit.connector.rest.RestClient;
import org.idmunit.IdMUnitException;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.trivir.idmunit.connector.ConfigTests.TEST_DOMAIN;
import static com.trivir.idmunit.connector.GoogleAppsConnector.ADMIN_EMAIL;
import static com.trivir.idmunit.connector.api.SendAsApi.*;
import static com.trivir.idmunit.connector.api.resource.SendAs.Factory.newSendAs;
import static com.trivir.idmunit.connector.util.TestUtil.*;
import static org.junit.Assert.*;

//NOTE: the SendAs email address must already exist in Gmail. It does not have to be an Alias, however. I used Alias
//  for convenience to quickly create an email address that would allow SendAs operations to succeed.

//TODO: create all Aliases in advance to minimize the delays between tests?

public class TestSendAsApi {

    private static final int TEST_NUM_ALIASES = 2;
    private static final String[] TEST_ALIASES = new String[TEST_NUM_ALIASES];
    private static final String EMAIL_TEMPLATE = "uncc_sendas_%s.idmunit@" + TEST_DOMAIN;

    static {
        if (TEST_NUM_ALIASES < 2) {
            throw new IllegalStateException("TEST_NUM_ALIASES must be >= 2. Current value: " + TEST_NUM_ALIASES);
        }
    }

    private static void createTestAliases(RestClient rest) throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        //add test aliases
        for (int a = 0; a < TEST_NUM_ALIASES; a++) {
            String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
            TEST_ALIASES[a] = email;
            AliasApi.insertAlias(rest, userId, email);
        }
    }

    @BeforeClass
    public static void setUpOnce() throws Exception {
        System.out.println("Setting-up tests...");
        RestClient rest = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES).getRestClient();
        System.out.println("Setting-up test users...");
        resetTestUsers(rest);
        System.out.println("Creating test aliases...");
        //create aliases ahead-of-time due to delay between creation and recognition by Gmail SendAs API
        createTestAliases(rest);
        System.out.println(String.format(
                "Waiting %d seconds for test Aliases to become visible to the SendAs API...",
                ConfigTests.TEST_WAIT_SECONDS_INIT_SEND_AS));
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_INIT_SEND_AS);
        System.out.println("Setup complete");
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        RestClient rest = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES).getRestClient();
        deleteTestUsers(rest);
    }

    @Test
    public void testListSendAs() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        SendAs sendAs = newSendAs(userId, email);
        createSendAs(gmail, sendAs);

        //verify sendAs
        List<SendAs> sendAses = listSendAs(gmail, userId);
        boolean found = false;
        for (SendAs as : sendAses) {
            if (email.contains(as.getSendAsEmail())) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        //cleanup
        deleteAllSendAs(gmail, userId);
    }

    @Test
    public void testHasSendAses() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        SendAs sendAs = newSendAs(userId, email);
        createSendAs(gmail, sendAs);

        assertTrue(hasSendAs(gmail, userId, email));

        //cleanup
        deleteAllSendAs(gmail, userId);
    }

    @Test
    public void testListSendAsEmailOnly() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        SendAs sendAs = newSendAs(userId, email);
        createSendAs(gmail, sendAs);

        List<String> sendAsEmails = listSendAsEmailOnly(gmail, userId);
        assertTrue(sendAsEmails.contains(email));

        //cleanup
        deleteAllSendAs(gmail, userId);
    }

    @Test
    public void testCreateBasicSendAs() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendas & verify
        SendAs newSendAs = newSendAs(userId, email);
        SendAs returnedSendAs = createSendAs(gmail, newSendAs);
        assertTrue(newSendAs.equals(returnedSendAs));

        //cleanup
        deleteSendAs(gmail, newSendAs);
    }

    @Test
    public void testCreateSendAsInvalidEmail() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String sendAsEmail = "notanemailaddress";
        SendAs sendAs = newSendAs(userId, sendAsEmail);

        try {

            //create sendas
            createSendAs(gmail, sendAs);
            fail();
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("400"));
            assertTrue(msg.contains("invalidargument"));
            assertTrue(msg.contains("invalid sendasemail"));
        }
    }

    @Test
    public void testCreateSendAsUserDoesNotExist() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String sendAsEmail = newUniqueEmailAddress(EMAIL_TEMPLATE);
        SendAs sendAs = newSendAs(ConfigTests.TEST_USER_DOES_NOT_EXIST, sendAsEmail);

        try {

            //create sendas
            createSendAs(gmail, sendAs);
            fail();
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("403"));
            assertTrue(msg.contains("delegation denied"));
        }
    }

    @Test
    public void testCreateSendAsAliasDoesNotExist() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String sendAsEmail = newUniqueEmailAddress(EMAIL_TEMPLATE);

        try {

            //add sendas
            SendAs newSendAs = newSendAs(userId, sendAsEmail);
            createSendAs(gmail, newSendAs);
            fail();
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("invalidargument"));
            assertTrue(msg.contains("sendasemail is not a valid user or group"));
        }
    }

    @Test
    public void testCreateSendAs() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendas & verify
        SendAs newSendAs = newSendAs(
            userId,
            "display",
            email,
            email,
            "signature",
            "accepted",
            true,
            false,
            true,
            null);

        SendAs returnedSendAs = createSendAs(gmail, newSendAs);
        assertTrue(newSendAs.equals(returnedSendAs));

        //cleanup
        deleteSendAs(gmail, newSendAs);
    }

    @Test
    //signature, treatAsAlias, displayName, & replyToAddress are updateatable
    public void testUpdateSendAsOtherUpdateableAttributes() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String sendAsEmail = TEST_ALIASES[0];
        String replyToEmail = TEST_ALIASES[1];

        //add sendAs
        SendAs newSendAs = new SendAs();
        newSendAs.setUserId(userId);
        newSendAs.setSendAsEmail(sendAsEmail);
        newSendAs.setReplyToAddress(sendAsEmail);
        createSendAs(gmail, newSendAs);

        newSendAs.setSignature("signature");
        newSendAs.setTreatAsAlias(true);
        newSendAs.setDisplayName("display");
        newSendAs.setReplyToAddress(replyToEmail);

        SendAs updatedSendAs = updateSendAs(gmail, newSendAs);
        newSendAs.setReplyToAddress(replyToEmail);
        assertTrue(newSendAs.equals(updatedSendAs));

        //cleanup
        deleteSendAs(gmail, newSendAs);
    }

    @Test
    //sendAsEmail cannot be updated
    public void testUpdateSendAsEmail() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        SendAs newSendAs = new SendAs();
        newSendAs.setUserId(userId);
        newSendAs.setSendAsEmail(email);
        SendAs createdSendAs = createSendAs(gmail, newSendAs);

        createdSendAs.setSendAsEmail("other_sendas@idmunit.org");
        createdSendAs.setUserId(userId);

        try {
            updateSendAs(gmail, createdSendAs);
            fail();
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("404"));
            assertTrue(msg.contains("not found"));
            //pass
        }

        //cleanup
        deleteSendAs(gmail, newSendAs);
    }

    @Test
    public void testUpdateIsDefault() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        List<SendAs> sendAses = new ArrayList();

        for (int a = 0; a < TEST_NUM_ALIASES; a++) {

            //add sendAs
            SendAs newSendAs = new SendAs();
            newSendAs.setUserId(userId);
            boolean isDefault = (a == 0) ? true : false;
            newSendAs.setIsDefault(isDefault);

            newSendAs.setSendAsEmail(TEST_ALIASES[a]);
            createSendAs(gmail, newSendAs);

            sendAses.add(newSendAs);
        }

        assertTrue(getSendAs(gmail, sendAses.get(0)).getIsDefault());
        assertFalse(getSendAs(gmail, sendAses.get(TEST_NUM_ALIASES - 1)).getIsDefault());

        SendAs newDefault = sendAses.get(TEST_NUM_ALIASES - 1);
        newDefault.setIsDefault(true);
        SendAs updated = updateSendAs(gmail, newDefault);
        assertTrue(updated.getIsDefault());

        SendAs oldDefault = getSendAs(gmail, sendAses.get(0));
        assertFalse(oldDefault.getIsDefault());

        //cleanup
        for (int a = 0; a < TEST_NUM_ALIASES; a++) {
            deleteSendAs(gmail, sendAses.get(a));
        }
    }

    @Test
    //isPrimary cannot be set at creation time
    public void testCreateIsPrimary() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        SendAs newSendAs = new SendAs();
        newSendAs.setUserId(userId);
        newSendAs.setIsPrimary(true);
        newSendAs.setSendAsEmail(email);
        SendAs created = createSendAs(gmail, newSendAs);
        assertFalse(created.getIsPrimary());

        //cleanup
        deleteSendAs(gmail, newSendAs);
    }

    @Test
    //isPrimary is read-only
    public void testUpdateIsPrimary() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        SendAs newSendAs = new SendAs();
        newSendAs.setUserId(userId);
        newSendAs.setIsPrimary(true);
        newSendAs.setSendAsEmail(email);
        SendAs created = createSendAs(gmail, newSendAs);
        assertFalse(created.getIsPrimary());

        created.setUserId(userId);
        created.setIsPrimary(true);
        SendAs updated = updateSendAs(gmail, created);
        assertFalse(updated.getIsPrimary());

        //cleanup
        deleteSendAs(gmail, newSendAs);
    }


    @Test
    public void testGetSendAs() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        createSendAs(gmail, userId, email);

        //verify sendAs
        assertEquals(email, getSendAsEmailOnly(gmail, userId, email));
        assertEquals(email, getSendAsEmailOnly(gmail, newSendAs(userId, email)));
        SendAs sendAs = getSendAs(gmail, userId, email);

        assertNotNull(sendAs);
        assertEquals(email, sendAs.getSendAsEmail());
        sendAs = getSendAs(gmail, newSendAs(userId, email));
        assertNotNull(sendAs);
        assertEquals(email, sendAs.getSendAsEmail());

        //cleanup
        deleteSendAs(gmail, userId, email);
    }

    @Test
    public void testHasSendAs() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendAs
        createSendAs(gmail, userId, email);

        //verify sendAs
        assertTrue(hasSendAs(gmail, userId, email));
        assertTrue(hasSendAs(gmail, newSendAs(userId, email)));

        //cleanup
        deleteSendAs(gmail, userId, email);
    }

    @Test
    public void testGetSendAsDoesNotExist() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
        assertNull(getSendAs(gmail, userId, email));
        assertEquals(getSendAsEmailOnly(gmail, userId, email), "");

        SendAs sendAs = newSendAs(userId, email);
        assertNull(getSendAs(gmail, sendAs));
        assertEquals(getSendAsEmailOnly(gmail, sendAs), "");
    }

    @Test
    public void testHasSendAsDoesNotExist() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
        assertFalse(hasSendAs(gmail, userId, email));
        assertFalse(hasSendAs(gmail, newSendAs(userId, email)));
    }

    @Test
    public void testDeleteSendAsDoesNotExist() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
        assertFalse(deleteSendAs(gmail, userId, email));
        assertFalse(deleteSendAs(gmail, newSendAs(userId, email)));
    }

    @Test
    public void testDeleteSendAs() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        createSendAs(gmail, userId, email);
        List<String> sendAses = listSendAsEmailOnly(gmail, userId);
        assertTrue(sendAses.contains(email));

        assertTrue(deleteSendAs(gmail, userId, email));

        //NOTE: it can take some time for a delete to take effect
        System.out.println(String.format(
                "Waiting %d seconds for the delete to take effect...",
                ConfigTests.TEST_WAIT_SECONDS_DELETE));
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_DELETE);

        //verify
        sendAses = listSendAsEmailOnly(gmail, userId);
        assertFalse(sendAses.contains(email));
    }

    @Test
    public void testDeleteAllSendAsDoesNotExist() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        deleteAllSendAs(gmail, userId);
        assertFalse(deleteAllSendAs(gmail, userId));
    }

    @Test
    public void testDeleteAllSendAs() throws IdMUnitException {
        final String userId = ConfigTests.TEST_USERS[0];

        RestClient gmail = newRestClient(userId, SendAsApi.SCOPES);

        for (int a = 0; a < TEST_NUM_ALIASES; a++) {

            //add sendas
            createSendAs(gmail, newSendAs(userId, TEST_ALIASES[a]));
        }

        //verify
        assertTrue(listSendAsEmailOnly(gmail, userId).containsAll(Arrays.asList(TEST_ALIASES)));
        assertTrue(deleteAllSendAs(gmail, userId));

        //NOTE: it can take some time for a delete to take effect
        System.out.println(String.format(
                "Waiting %d seconds for the delete to take effect...",
                ConfigTests.TEST_WAIT_SECONDS_DELETE));
        waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_DELETE);

        //verify
        List<SendAs> sendAses = listSendAs(gmail, userId);
        assertTrue(sendAses.size() == 1);
        assertTrue(sendAses.get(0).getIsPrimary() == Boolean.TRUE);
    }

}
