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
import com.trivir.idmunit.connector.api.SendAsApi;
import com.trivir.idmunit.connector.api.resource.SendAs;
import com.trivir.idmunit.connector.api.resource.SmtpMsa;
import com.trivir.idmunit.connector.rest.RestClient;
import com.trivir.idmunit.connector.util.JWTUtil;
import org.idmunit.IdMUnitException;
import org.junit.*;

import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static com.trivir.idmunit.connector.ConfigTests.*;
import static com.trivir.idmunit.connector.GoogleAppsConnector.ADMIN_EMAIL;
import static com.trivir.idmunit.connector.GoogleAppsConnector.SYNTHETIC_ATTR_OBJECT_CLASS;
import static com.trivir.idmunit.connector.api.SendAsApi.*;
import static com.trivir.idmunit.connector.util.EntityConverter.sendAsToMap;
import static com.trivir.idmunit.connector.util.TestUtil.*;
import static org.junit.Assert.*;

//NOTE: the SendAs email address must already exist in Gmail. It does not have to be an Alias, however. I used Alias
//  for convenience to quickly create an email address that would allow SendAs operations to succeed.

public class TestClassSendAs {

    private static final int TEST_NUM_ALIASES = 2;
    private static final String[] TEST_ALIASES = new String[TEST_NUM_ALIASES];

    private static final String EMAIL_TEMPLATE = "uncc_sendas_%s.idmunit@" + TEST_DOMAIN;

    static {
        if (TEST_NUM_ALIASES < 2) {
            throw new IllegalStateException("TEST_NUM_ALIASES must be >= 2. Current value: " + TEST_NUM_ALIASES);
        }
    }

    private static void createTestAliases(RestClient rest, String userId) throws IdMUnitException {
        //add test aliases
        for (int a = 0; a < TEST_NUM_ALIASES; a++) {
            String email = newUniqueEmailAddress(EMAIL_TEMPLATE);
            TEST_ALIASES[a] = email;
            AliasApi.insertAlias(rest, userId, email);
        }
    }

    @BeforeClass
    public static final void setUpOnce() throws Exception {
        System.out.println("Setting-up tests...");
        GoogleAppsConnector admin = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);
        System.out.println("Setting-up test users...");
        resetTestUsers(admin.getRestClient());
        System.out.println("Creating test aliases...");
        //create aliases ahead-of-time due to delay between creation and recognition by Gmail SendAs API
        createTestAliases(admin.getRestClient(), TEST_USERS[0]);
        System.out.println(String.format(
                "Waiting %d seconds for test Aliases to become visible to the SendAs API...",
                TEST_WAIT_SECONDS_INIT_SEND_AS));
        waitTimeSeconds(TEST_WAIT_SECONDS_INIT_SEND_AS);
        System.out.println("Setup complete");

/*        GoogleAppsConnector gmail = newTestConnection(TEST_USERS[0], SendAsApi.SCOPES);
        boolean notCreated = true;
        long start = System.currentTimeMillis();
        long end = start;
        do {
            waitTimeSeconds(10);

            try {
                SendAs sendAs = SendAs.Factory.newSendAs(TEST_USERS[0], TEST_ALIASES[0]);
                SendAsApi.createSendAs(gmail.getRestClient(), sendAs);
                SendAsApi.deleteSendAs(gmail.getRestClient(), sendAs);
                waitTimeSeconds(TEST_WAIT_SECONDS_DELETE);
                notCreated = false;
                end = System.currentTimeMillis();
            } catch (IdMUnitException e) {
                if(e.getMessage().contains("500")) {
                    //not yet created
                } else {
                    throw e;
                }
            }
        } while(notCreated);
        System.out.println("Delay: " + ((end - start)/60));*/
    }

    @AfterClass
    public static final void tearDownOnce() throws Exception {
        GoogleAppsConnector conn = newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);
        deleteTestUsers(conn.getRestClient());
    }

    @Before
    public void setUp() { }

    @After
    public void tearDown() { }

    @Test
    public void testInsertBasicSendAs() throws IdMUnitException {
        final String userId = TEST_USERS[0];

        GoogleAppsConnector gmail = newTestConnection(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendas & verify
        SendAs sendAs = SendAs.Factory.newSendAs(userId, email);
        Map<String, Collection<String>> map = sendAsToMap(sendAs);
        gmail.opCreateObject(map);

        try {
            List<String> sendAsEmails = listSendAsEmailOnly(gmail.getRestClient(), userId);
            assertTrue(sendAsEmails.contains(email));
        } finally {
            deleteObjectSuppressed(gmail, map);
        }
    }

    @Test
    public void testInsertSendAs() throws IdMUnitException {
        final String userId = TEST_USERS[0];

        GoogleAppsConnector gmail = newTestConnection(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendas & verify
        SendAs newSendAs = SendAs.Factory.newSendAs(
            userId,
            "display",
            email,
            email,
            "signature",
            "accepted",
            false,
            false,
            true,
            null);
        Map<String, Collection<String>> map = sendAsToMap(newSendAs);
        gmail.opCreateObject(map);

        try {
            SendAs returnedSendAs = getSendAs(gmail.getRestClient(), userId, email);
            assertNotNull(returnedSendAs);
            assertTrue(newSendAs.equals(returnedSendAs));
        } finally {
            deleteObjectSuppressed(gmail, map);
        }
    }

    @Test
    public void testDeleteSendAs() throws IdMUnitException {
        final String userId = TEST_USERS[0];

        GoogleAppsConnector gmail = newTestConnection(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        //add sendas & verify
        SendAs sendAs = SendAs.Factory.newSendAs(userId, email);
        Map<String, Collection<String>> sendAsMap = sendAsToMap(sendAs);
        gmail.opCreateObject(sendAsMap);

        try {
            assertTrue(listSendAsEmailOnly(gmail.getRestClient(), userId).contains(email));

            //delete alias
            gmail.opDeleteObject(sendAsMap);

            //NOTE: it can take some time for a delete to take effect
            System.out.println(String.format(
                    "Waiting %d seconds for the delete to take effect...",
                    ConfigTests.TEST_WAIT_SECONDS_DELETE));
            waitTimeSeconds(ConfigTests.TEST_WAIT_SECONDS_DELETE);

            //verify
            assertFalse(hasSendAs(gmail.getRestClient(), userId, email));
        } finally {
            deleteObjectSuppressed(gmail, sendAsMap);
        }
    }

    @Test
    public void testValidateBasicSendAs() throws IdMUnitException {
        final String userId = TEST_USERS[0];

        GoogleAppsConnector gmail = newTestConnection(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        SendAs sendAs = SendAs.Factory.newSendAs(userId, email);
        Map<String, Collection<String>> sendAsMap = sendAsToMap(sendAs);
        gmail.opCreateObject(sendAsMap);

        try {
            PrivateKey key = null;
            try {
                key = JWTUtil.pemStringToPrivateKey(ConfigTests.TEST_PRIVATE_KEY);
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }

            boolean hasSendAs = hasSendAs(ConfigTests.TEST_SERVICE_EMAIL, key, userId, sendAs);
            assertTrue(hasSendAs);

            gmail.opValidateObject(sendAsMap);
        } finally {
            //cleanup
            deleteObjectSuppressed(gmail, sendAsMap);
        }
    }

    @Test
    public void testValidateSendAsEmpty() throws IdMUnitException {
        final String userId = TEST_USERS[0];

        GoogleAppsConnector gmail = newTestConnection(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        SendAs sendAs = SendAs.Factory.newSendAs(
            userId,
            null,
            email,
            null,
            null,
            null,
            false,
            false,
            true,
            null);
        Map<String, Collection<String>> sendAsMap = sendAsToMap(sendAs);
        gmail.opCreateObject(sendAsMap);

        try {
            assertTrue(hasSendAs(gmail.getRestClient(), userId, email));

            Map<String, Collection<String>> sendAsValidateMap = new HashMap<>();
            sendAsValidateMap.put(SYNTHETIC_ATTR_OBJECT_CLASS, Collections.singletonList(SendAs.Schema.CLASS_NAME));
            sendAsValidateMap.put(SendAs.Schema.ATTR_USERID, Collections.singletonList(userId));
            sendAsValidateMap.put(SendAs.Schema.ATTR_SEND_AS_EMAIL, Collections.singletonList(email));
            sendAsValidateMap.put(SendAs.Schema.ATTR_DISPLAY_NAME, Collections.singletonList("[EMPTY]"));
            sendAsValidateMap.put(SendAs.Schema.ATTR_REPLY_TO_ADDRESS, Collections.singletonList("[EMPTY]"));
            sendAsValidateMap.put(SendAs.Schema.ATTR_SIGNATURE, Collections.singletonList("[EMPTY]"));
            sendAsValidateMap.put(SendAs.Schema.ATTR_VERIFICATION_STATUS, Collections.singletonList("accepted"));
            sendAsValidateMap.put(SmtpMsa.Schema.ATTR_HOST, Collections.singletonList("[EMPTY]"));
            sendAsValidateMap.put(SmtpMsa.Schema.ATTR_PORT, Collections.singletonList("[EMPTY]"));
            sendAsValidateMap.put(SmtpMsa.Schema.ATTR_PASSWORD, Collections.singletonList("[EMPTY]"));
            sendAsValidateMap.put(SmtpMsa.Schema.ATTR_SECURITY_MODE, Collections.singletonList("[EMPTY]"));
            sendAsValidateMap.put(SmtpMsa.Schema.ATTR_USERNAME, Collections.singletonList("[EMPTY]"));

            gmail.opValidateObject(sendAsValidateMap);
        } finally {
            //cleanup
            deleteObjectSuppressed(gmail, sendAsMap);
        }
    }

    @Test
    public void testValidatesSendAsDifferent() throws IdMUnitException {
        final String userId = TEST_USERS[0];

        GoogleAppsConnector gmail = newTestConnection(userId, SendAsApi.SCOPES);

        String email = TEST_ALIASES[0];

        SendAs sendAs = SendAs.Factory.newSendAs(
            userId,
            "display",
            email,
            email,
            "signature",
            "accepted",
            false,
            false,
            true,
            null);
        Map<String, Collection<String>> sendAsMap = sendAsToMap(sendAs);
        gmail.opCreateObject(sendAsMap);

        try {
            assertTrue(hasSendAs(gmail.getRestClient(), userId, email));

            sendAs.setDisplayName("different display");
            sendAs.setSignature("different signature");
            sendAs.setTreatAsAlias(false);
            sendAs.setReplyToAddress("different-reply-to@idmunit.org");

            gmail.opValidateObject(sendAsToMap(sendAs));
        } catch (IdMUnitException e) {
            String msg = e.getMessage().toLowerCase();
            if (!(msg.contains("validation failed") &&
                msg.contains(SendAs.Schema.ATTR_DISPLAY_NAME.toLowerCase()) &&
                msg.contains(SendAs.Schema.ATTR_SIGNATURE.toLowerCase()) &&
                msg.contains(SendAs.Schema.ATTR_REPLY_TO_ADDRESS.toLowerCase()) &&
                msg.contains(SendAs.Schema.ATTR_TREAT_AS_ALIAS.toLowerCase()) &&
                msg.contains("[4] error(s) found"))) {
                fail();
            }
        } finally {
            //cleanup
            deleteObjectSuppressed(gmail, sendAsMap);
        }
    }

}
