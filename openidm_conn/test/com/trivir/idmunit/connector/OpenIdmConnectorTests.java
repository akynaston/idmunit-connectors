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

import com.google.gson.*;
import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.*;

public class OpenIdmConnectorTests extends TestCase {
    private static final String SERVER = "server";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private static final String TEST_SERVER = "10.10.30.249";
    //private static final String TEST_SERVER= "172.17.2.30";

    private static final Map<String, String> CONFIG;

    private OpenIdmConnector connector;

    static {
        CONFIG = new HashMap<String, String>();
        CONFIG.put(SERVER, TEST_SERVER);
        CONFIG.put(USER, "openidm-admin");
        CONFIG.put(PASSWORD, "openidm-admin");
    }

    private static void deleteManagedUser(RestClient rest, String userId) throws UnsupportedEncodingException, IdMUnitException {
        final String queryFilter = URLEncoder.encode("firstId eq \"" + userId + "\" or secondId eq \"" + userId + "\"", "UTF-8");
        final JsonParser parser = new JsonParser();

        RestClient.Response response = rest.executeGet("/repo/link?_queryFilter=" + queryFilter);
        String json = response.messageBody;
        JsonObject object = parser.parse(json).getAsJsonObject();
        JsonArray array = object.getAsJsonArray("result");
        Iterator<JsonElement> iter = array.iterator();
        while (iter.hasNext()) {
            object = iter.next().getAsJsonObject();
            String id = object.getAsJsonPrimitive("_id").getAsString();
            String rev = object.getAsJsonPrimitive("_rev").getAsString();
            rest.executeDelete("/repo/link/" + id, rev);
        }

        try {
            rest.executeDelete("/managed/user/" + userId);
        } catch (RestError e) {
            //ignore
        }
    }

    public void setUp() throws Exception {
        connector = new OpenIdmConnector();
        connector.setup(CONFIG);
    }

    public void tearDown() throws Exception {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        final String userId = "tuser2_id";

        deleteManagedUser(rest, userId);

        connector.tearDown();
    }

    public void setUpTestConnector() {

    }

    public void testUserCRUD() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("userName", singleValue("tuser2"));
        attrs.put("givenName", singleValue("Test"));
        attrs.put("sn", singleValue("User"));
        attrs.put("mail", singleValue("tuser@example.com"));
        attrs.put("telephoneNumber", singleValue("555-555-1212"));
        attrs.put("password", singleValue("T3stPassw0rd"));
        attrs.put("description", singleValue("My first user"));
        attrs.put("_id", singleValue("tuser2_id"));
        attrs.put("objectType", singleValue("user"));
        attrs.put("birthDate", singleValue("1900/01/01"));

        Map<String, Collection<String>> validAttrs = new HashMap<String, Collection<String>>();
        validAttrs.put("userName", singleValue("tuser2"));
        validAttrs.put("givenName", singleValue("Test"));
        validAttrs.put("sn", singleValue("User"));
        validAttrs.put("mail", singleValue("tuser@example.com"));
        validAttrs.put("telephoneNumber", singleValue("555-555-1212"));
        validAttrs.put("description", singleValue("My first user"));
        validAttrs.put("_id", singleValue("tuser2_id"));
        validAttrs.put("objectType", singleValue("user"));

        connector.opAddObject(Collections.unmodifiableMap(attrs));

        // remove password from the expected attributes since validating passwords isn't supported yet
        // TODO support validating passwords
        attrs.remove("password");
        connector.opValidateObject(Collections.unmodifiableMap(validAttrs));

        Map<String, Collection<String>> userInfo = new HashMap<String, Collection<String>>();
        userInfo.put("userName", singleValue("tuser2"));
        userInfo.put("objectType", singleValue("user"));

        connector.opDeleteObject(Collections.unmodifiableMap(userInfo));
    }

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    private static Collection<String> multiValue(String... values) {
        return Arrays.asList(values);
    }

     /*
    {
        "userName":"tuser2",
        "givenName":"Test",
        "sn":"User",
        "mail":"tuser@example.com",
        "telephoneNumber":"555-555-1212",
        "password":"T3stPassw0rd",
        "description":"My user to validate",
        "_id":"tuser2_id",

    }
     */

    public void testAddComplexAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);

        Map<String, Collection<String>> createAttrs = new HashMap<String, Collection<String>>();
        createAttrs.put("userName", singleValue("bwayne"));
        createAttrs.put("givenName", singleValue("Bruce"));
        createAttrs.put("sn", singleValue("Wayne"));
        createAttrs.put("mail", singleValue("BadTest@yahooligans.com"));
        createAttrs.put("telephoneNumber", singleValue("555-555-1212"));
        createAttrs.put("description", singleValue("My user to validate"));
        createAttrs.put("_id", singleValue("bwayneid"));
        createAttrs.put("objectType", singleValue("user"));
        createAttrs.put("testAttr[]", multiValue("one", "two", "three", "four"));

        connector.opAddObject(createAttrs);
        try {

            RestClient.Response response = rest.executeGet("/managed/user/bwayneid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"BadTest@yahooligans.com\"", user.get("mail").toString());
            assertEquals("[\"one\",\"two\",\"three\",\"four\"]", user.get("testAttr").toString());
        } catch (IdMUnitException e) {
            e.printStackTrace();

        } finally {
            rest.executeDelete("/managed/user/bwayneid");
        }
    }

    public void testDeleteSimple() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);

        Map<String, Collection<String>> createAttrs = new HashMap<String, Collection<String>>();
        createAttrs.put("userName", singleValue("bwayne"));
        createAttrs.put("givenName", singleValue("Bruce"));
        createAttrs.put("sn", singleValue("Wayne"));
        createAttrs.put("mail", singleValue("BadTest@yahooligans.com"));
        createAttrs.put("_id", singleValue("bwayneid"));
        createAttrs.put("objectType", singleValue("user"));

        connector.opAddObject(createAttrs);
        try {

            RestClient.Response response = rest.executeGet("/managed/user/bwayneid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"bwayneid\"", user.get("_id").toString());

            Map<String, Collection<String>> deleteAttrs = new HashMap<String, Collection<String>>();
            deleteAttrs.put("_id", singleValue("bwayneid"));
            deleteAttrs.put("objectType", singleValue("user"));
            connector.opDeleteObject(deleteAttrs);

            try {
                rest.executeGet("/managed/user/bwayneid?_fields=*,*_ref");
                fail("User shouldn't exist");
            } catch (RestError err) {
                assertEquals("404", err.getErrorCode());
            }
        } catch (IdMUnitException e) {
            e.printStackTrace();

        } finally {
            try {
                rest.executeDelete("/managed/user/bwayneid");
            } catch (RestError err) {
                //ignore
            }
        }
    }

    public void testAddBooleanAttr() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);

        Map<String, Collection<String>> createAttrs = new HashMap<String, Collection<String>>();
        createAttrs.put("userName", singleValue("bwayne"));
        createAttrs.put("givenName", singleValue("Bruce"));
        createAttrs.put("sn", singleValue("Wayne"));
        createAttrs.put("mail", singleValue("BadTest@yahooligans.com"));
        createAttrs.put("telephoneNumber", singleValue("555-555-1212"));
        createAttrs.put("description", singleValue("My user to validate"));
        createAttrs.put("_id", singleValue("bwayneid"));
        createAttrs.put("objectType", singleValue("user"));
        createAttrs.put("attrBoolean::boolean", singleValue("true"));

        connector.opAddObject(createAttrs);
        try {

            RestClient.Response response = rest.executeGet("/managed/user/bwayneid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"BadTest@yahooligans.com\"", user.get("mail").toString());
            assertEquals(true, user.get("attrBoolean").getAsBoolean());
        } catch (IdMUnitException e) {
            e.printStackTrace();

        } finally {
            rest.executeDelete("/managed/user/bwayneid");
        }
    }

    public void testValidateUserFailureOutput() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"userName\":\"tuser2\",\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"description\":\"My user to validate\",\n" +
                "        \"_id\":\"tuser2_id\"\n" +
                "        \n" +
                "    }";

        Map<String, Collection<String>> actualAttrs = new HashMap<String, Collection<String>>();
        actualAttrs.put("userName", singleValue("tuser2"));
        actualAttrs.put("givenName", singleValue("Test"));
        actualAttrs.put("sn", singleValue("User"));
        actualAttrs.put("mail", singleValue("BadTest@yahooligans.com"));
        actualAttrs.put("telephoneNumber", singleValue("555-555-1212"));
        actualAttrs.put("description", singleValue("My user to validate"));
        actualAttrs.put("_id", singleValue("tuser2_id"));
        actualAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);
            connector.opValidateObject(Collections.unmodifiableMap(actualAttrs));
        } catch (IdMUnitFailureException e) {
            assertEquals("'.mail' attribute mismatch: expected \"BadTest@yahooligans.com\" but was \"tuser@example.com\"", e.getMessage());
            //We should see errors for all attributes that are different between the two maps above.
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    public void testValidateRegex() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"userName\":\"tuser2\",\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"description\":\"My user to validate\",\n" +
                "        \"_id\":\"tuser2_id\"\n" +
                "        \n" +
                "    }";

        Map<String, Collection<String>> newAttrs = new HashMap<String, Collection<String>>();
        newAttrs.put("userName", singleValue("tuser2"));
        newAttrs.put("_id", singleValue("tuser2_id"));
        newAttrs.put("telephoneNumber", singleValue(".*"));
        newAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opValidateObject(Collections.unmodifiableMap(newAttrs));
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }


    public void testDeleteUserPlusLinks() throws IdMUnitException, UnsupportedEncodingException {
        final RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        final String userId = "tuser2_id";
        final JsonParser parser = new JsonParser();

        deleteManagedUser(rest, userId);

        try {
            //create user
            String createJson = "{\n" +
                    "        \"userName\":\"tuser2\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"mail\":\"tuser@example.com\",\n" +
                    "        \"telephoneNumber\":\"555-555-1212\",\n" +
                    "        \"password\":\"T3stPassw0rd\",\n" +
                    "        \"description\":\"My user to validate\",\n" +
                    "        \"_id\":\"" + userId + "\"\n" +
                    "        \n" +
                    "    }";
            rest.executePost("/managed/user?_action=create", createJson);

            //create links
            createJson = "{\n" +
                    "    \"linkType\":\"AD_managedUser\",\n" +
                    "    \"linkQualifier\":\"default\",\n" +
                    "    \"firstId\":\"" + userId + "\",\n" +
                    "    \"secondId\":\"67890\"\n" +
                    "}";
            rest.executePost("/repo/link?_action=create", createJson);
            createJson = "{\n" +
                    "    \"linkType\":\"AD_managedUser\",\n" +
                    "    \"linkQualifier\":\"default\",\n" +
                    "    \"firstId\":\"67891\",\n" +
                    "    \"secondId\":\"" + userId + "\"\n" +
                    "}";
            rest.executePost("/repo/link?_action=create", createJson);
            createJson = "{\n" +
                    "    \"linkType\":\"AD_managedUser\",\n" +
                    "    \"linkQualifier\":\"default1\",\n" +
                    "    \"firstId\":\"09876\",\n" +
                    "    \"secondId\":\"" + userId + "\"\n" +
                    "}";
            rest.executePost("/repo/link?_action=create", createJson);

            String queryFilter = URLEncoder.encode("firstId eq \"" + userId + "\" or secondId eq \"" + userId + "\"", "UTF-8");

            RestClient.Response response = rest.executeGet("/repo/link?_queryFilter=" + queryFilter);
            String json = response.messageBody;
            JsonObject object = parser.parse(json).getAsJsonObject();
            JsonArray array = object.getAsJsonArray("result");
            assertEquals(3, array.size());

            Map<String, Collection<String>> userInfo = new HashMap<String, Collection<String>>();
            userInfo.put("userName", singleValue("tuser2"));
            userInfo.put("objectType", singleValue("user"));
            connector.opDeleteObject(Collections.unmodifiableMap(userInfo));

            response = rest.executeGet("/repo/link?_queryFilter=" + queryFilter);
            json = response.messageBody;
            object = parser.parse(json).getAsJsonObject();
            array = object.getAsJsonArray("result");
            assertEquals(0, array.size());
        } finally {
            deleteManagedUser(rest, userId);
        }
    }

    public void testValidateLink() throws IdMUnitException, UnsupportedEncodingException {
        final RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        final String userId = "tuser2_id";
        final String userName = "tuser2";
        final String linkType = "AD_managedUser";

        deleteManagedUser(rest, userId);

        try {
            //create user
            String createJson = "{\n" +
                    "        \"userName\":\"" + userName + "\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"mail\":\"tuser@example.com\",\n" +
                    "        \"telephoneNumber\":\"555-555-1212\",\n" +
                    "        \"password\":\"T3stPassw0rd\",\n" +
                    "        \"description\":\"My user to validate\",\n" +
                    "        \"_id\":\"" + userId + "\"\n" +
                    "        \n" +
                    "    }";
            rest.executePost("/managed/user?_action=create", createJson);

            //create link
            createJson = "{\n" +
                    "    \"linkType\":\"" + linkType + "\",\n" +
                    "    \"linkQualifier\":\"default\",\n" +
                    "    \"firstId\":\"" + userId + "\",\n" +
                    "    \"secondId\":\"67890\"\n" +
                    "}";
            rest.executePost("/repo/link?_action=create", createJson);

            Map<String, Collection<String>> validateAttrs = new HashMap<String, Collection<String>>();
            validateAttrs.put("linkType", singleValue(linkType));
            validateAttrs.put("objectType", singleValue("user"));
            validateAttrs.put("userName", singleValue(userName));
            connector.opValidateLink(Collections.unmodifiableMap(validateAttrs));
        } finally {
            deleteManagedUser(rest, userId);
        }
    }

    public void testValidateLinkNoLink() throws IdMUnitException, UnsupportedEncodingException {
        final RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        final String userId = "tuser2_id";
        final String userName = "tuser2";
        final String linkType = "AD_managedUser";

        deleteManagedUser(rest, userId);

        try {
            //create user
            String createJson = "{\n" +
                    "        \"userName\":\"" + userName + "\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"mail\":\"tuser@example.com\",\n" +
                    "        \"telephoneNumber\":\"555-555-1212\",\n" +
                    "        \"password\":\"T3stPassw0rd\",\n" +
                    "        \"description\":\"My user to validate\",\n" +
                    "        \"_id\":\"" + userId + "\"\n" +
                    "        \n" +
                    "    }";
            rest.executePost("/managed/user?_action=create", createJson);

            Map<String, Collection<String>> validateAttrs = new HashMap<String, Collection<String>>();
            validateAttrs.put("linkType", singleValue(linkType));
            validateAttrs.put("objectType", singleValue("user"));
            validateAttrs.put("userName", singleValue(userName));
            try {
                connector.opValidateLink(Collections.unmodifiableMap(validateAttrs));
                fail("There should have been no matching links");
            } catch (IdMUnitException e) {
                assertTrue(e.getMessage().contains("No link"));
            }
        } finally {
            deleteManagedUser(rest, userId);
        }
    }

    public void testDeleteUserLeaveLinks() throws IdMUnitException, UnsupportedEncodingException {
        final RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        final String userId = "tuser2_id";
        final JsonParser parser = new JsonParser();

        deleteManagedUser(rest, userId);

        try {
            //create user
            String createJson = "{\n" +
                    "        \"userName\":\"tuser2\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"mail\":\"tuser@example.com\",\n" +
                    "        \"telephoneNumber\":\"555-555-1212\",\n" +
                    "        \"password\":\"T3stPassw0rd\",\n" +
                    "        \"description\":\"My user to validate\",\n" +
                    "        \"_id\":\"" + userId + "\"\n" +
                    "        \n" +
                    "    }";
            rest.executePost("/managed/user?_action=create", createJson);

            //create links
            createJson = "{\n" +
                    "    \"linkType\":\"AD_managedUser\",\n" +
                    "    \"linkQualifier\":\"default\",\n" +
                    "    \"firstId\":\"" + userId + "\",\n" +
                    "    \"secondId\":\"67890\"\n" +
                    "}";
            rest.executePost("/repo/link?_action=create", createJson);
            createJson = "{\n" +
                    "    \"linkType\":\"AD_managedUser\",\n" +
                    "    \"linkQualifier\":\"default\",\n" +
                    "    \"firstId\":\"67891\",\n" +
                    "    \"secondId\":\"" + userId + "\"\n" +
                    "}";
            rest.executePost("/repo/link?_action=create", createJson);
            createJson = "{\n" +
                    "    \"linkType\":\"AD_managedUser\",\n" +
                    "    \"linkQualifier\":\"default1\",\n" +
                    "    \"firstId\":\"09876\",\n" +
                    "    \"secondId\":\"" + userId + "\"\n" +
                    "}";
            rest.executePost("/repo/link?_action=create", createJson);

            String queryFilter = URLEncoder.encode("firstId eq \"" + userId + "\" or secondId eq \"" + userId + "\"", "UTF-8");

            RestClient.Response response = rest.executeGet("/repo/link?_queryFilter=" + queryFilter);
            String json = response.messageBody;
            JsonObject object = parser.parse(json).getAsJsonObject();
            JsonArray array = object.getAsJsonArray("result");
            assertEquals(3, array.size());

            Map<String, Collection<String>> userInfo = new HashMap<String, Collection<String>>();
            userInfo.put("userName", singleValue("tuser2"));
            userInfo.put("objectType", singleValue("user"));
            connector.opDeleteObjectLeaveLinks(Collections.unmodifiableMap(userInfo));

            response = rest.executeGet("/repo/link?_queryFilter=" + queryFilter);
            json = response.messageBody;
            object = parser.parse(json).getAsJsonObject();
            array = object.getAsJsonArray("result");
            assertEquals(3, array.size());
        } finally {
            deleteManagedUser(rest, userId);
        }
    }

    /*
{
    "userName":"testuser",
    "sn":"user",
    "givenName":"Test",
    "_id":"tuser2_id",
    "mail":"testuser@example.com",
    "authzRoles": [
        {"_ref": "repo/internal/role/openidm-authorized"},
        {"_ref": "repo/internal/role/openidm-admin"}
    ]
}
     */

    public void testDeleteUserIdNotFound() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        // we have not created a user so no matter what ID we provide the user should not exist.

        Map<String, Collection<String>> userInfo = new HashMap<String, Collection<String>>();
        userInfo.put("userName", singleValue("bwayne"));
        userInfo.put("_id", singleValue("bwayne_id"));
        userInfo.put("objectType", singleValue("user"));

        connector.opDeleteObject(Collections.unmodifiableMap(userInfo));
    }

    public void testDeleteUserNameNotFound() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);

        Map<String, Collection<String>> userInfo = new HashMap<String, Collection<String>>();
        userInfo.put("userName", singleValue("bwayne"));
        userInfo.put("objectType", singleValue("user"));

        try {
            connector.opValidateObject(Collections.unmodifiableMap(userInfo));
        } catch (IdMUnitException e) {
            //the message may be version-dependent
            assertTrue(e.getMessage().toLowerCase().contains("no objects returned") || e.getMessage().contains("\"resultCount\":0"));
        }

        connector.opDeleteObject(Collections.unmodifiableMap(userInfo));
    }

    public void testValidateComplexAttrSuccess() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        try {
            String managedComplexUser = "{\n" +
                    "    \"userName\":\"tuser2\",\n" +
                    "    \"sn\":\"User\",\n" +
                    "    \"givenName\":\"Test\",\n" +
                    "    \"_id\":\"tuser2_id\",\n" +
                    "    \"mail\":\"testuser@example.com\",\n" +
                    "    \"authzRoles\": [\n" +
                    "        {\"_ref\": \"repo/internal/role/openidm-authorized\"},\n" +
                    "        {\"_ref\": \"repo/internal/role/openidm-admin\"}\n" +
                    "    ]\n" +
                    "}";
            rest.executePost("/managed/user?_action=create", managedComplexUser);

            Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
            attrs.put("userName", singleValue("tuser2"));
            attrs.put("_id", singleValue("tuser2_id"));
            attrs.put("givenName", singleValue("Test"));
            attrs.put("sn", singleValue("User"));
            attrs.put("mail", singleValue("testuser@example.com"));
            attrs.put("authzRoles[]._ref", multiValue("repo/internal/role/openidm-authorized", "repo/internal/role/openidm-admin")); //this test will pass but throw message that authzRoles._ref does not match.
            attrs.put("objectType", singleValue("user"));

            try {
                connector.opValidateObject(Collections.unmodifiableMap(attrs));
            } catch (IdMUnitFailureException e) {
                throw new IdMUnitException(e.getMessage());
            }
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    public void testValidateComplexAttrFailure() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        try {
            String managedComplexUser = "{\n" +
                    "    \"userName\":\"tuser2\",\n" +
                    "    \"sn\":\"User\",\n" +
                    "    \"givenName\":\"Test\",\n" +
                    "    \"_id\":\"tuser2_id\",\n" +
                    "    \"mail\":\"testuser@example.com\",\n" +
                    "    \"authzRoles\": [\n" +
                    "        {\"_ref\": \"repo/internal/role/openidm-authorized\"},\n" +
                    "        {\"_ref\": \"repo/internal/role/openidm-admin\"}\n" +
                    "    ]\n" +
                    "}";

            rest.executePost("/managed/user?_action=create", managedComplexUser);

            Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
            attrs.put("userName", singleValue("tuser2"));
            attrs.put("givenName", singleValue("Test"));
            attrs.put("sn", singleValue("User"));
            attrs.put("mail", singleValue("testuser@example.com"));
            attrs.put("objectType", singleValue("user"));
            attrs.put("authzRoles[]._ref", multiValue("repo/internal/role/openidm-admin", "badValue")); //this test will pass but throw message that authzRoles._ref does not match.

            try {
                connector.opValidateObject(Collections.unmodifiableMap(attrs));
            } catch (IdMUnitFailureException e) {
                assertEquals("'.authzRoles' attribute mismatch: expected item {\"_ref\":\"badValue\"} was not found in [{\"_ref\":\"repo/internal/role/openidm-authorized\",\"_refProperties\":},{\"_ref\":\"repo/internal/role/openidm-admin\",\"_refProperties\":}]", e.getMessage().replaceAll("\"_refResourceCollection\":\"repo/internal/role\",\"_refResourceId\":\"openidm-\\w{1,}\\\",", "").replaceAll("\\{\"_id\":\"(.{36})\",\"_rev\":\"(\\w{1,})\"\\}", ""));
            }
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    /*
    {
        "givenName":"Test",
        "sn":"User",
        "mail":"tuser@example.com",
        "telephoneNumber":"555-555-1212",
        "password":"T3stPassw0rd",
        "description":"My user to reconcile",
        "_id":"tuser2_id",
        "userName":"tuser2",
        "mapping":"systemXmlfileAccounts_managedUser",
        "objectType":"user",
    }
     */

    public void testReconcileUserWithId() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"description\":\"My user to reconcile\",\n" +
                "        \"_id\":\"tuser2_id\",\n" +
                "        \"userName\":\"tuser2\",\n" +
                "        \"objectType\":\"user\"\n" +
                "    }";
        String objectType = "user";
        String userId = "tuser2_id";

        Map<String, Collection<String>> reconAttrs = new HashMap<String, Collection<String>>();
        reconAttrs.put("_id", singleValue(userId));
        reconAttrs.put("mapping", singleValue("systemLdapAccount_managedUser"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);
            RestClient.Response response = rest.executeGet("/managed/" + objectType + "/" + userId + "?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"Test\"", user.get("givenName").toString());
            assertEquals("\"User\"", user.get("sn").toString());
            assertEquals("\"tuser@example.com\"", user.get("mail").toString());
            assertEquals("\"My user to reconcile\"", user.get("description").toString());
            assertEquals("\"tuser2_id\"", user.get("_id").toString());
            assertEquals("\"tuser2\"", user.get("userName").toString());

            connector.opReconcile(Collections.unmodifiableMap(reconAttrs));
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    /*
    {
        "givenName":"Test",
        "sn":"User",
        "mail":"tuser@example.com",
        "telephoneNumber":"555-555-1212",
        "password":"T3stPassw0rd",
        "description":"My user to reconcile",
        "_id":"tuser2_id",
        "userName":"tuser2",
        "mapping":"systemXmlfileAccounts_managedUser",
        "objectType":"user",
    }
     */

    public void testReconcileUserWithNonIdAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"description\":\"My user to reconcile\",\n" +
                "        \"_id\":\"tuser2_id\",\n" +
                "        \"userName\":\"tuser2\",\n" +
                "        \"objectType\":\"user\"\n" +
                "    }";
        String objectType = "user";
        String userId = "tuser2_id";

        Map<String, Collection<String>> reconAttrs = new HashMap<String, Collection<String>>();
        reconAttrs.put("cn", singleValue("tuser2"));
        reconAttrs.put("mapping", singleValue("systemLdapAccount_managedUser"));
        try {
            rest.executePost("/managed/user?_action=create", createAttrs);
            RestClient.Response response = rest.executeGet("/managed/" + objectType + "/" + userId + "?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"Test\"", user.get("givenName").toString());
            assertEquals("\"User\"", user.get("sn").toString());
            assertEquals("\"tuser@example.com\"", user.get("mail").toString());
            assertEquals("\"My user to reconcile\"", user.get("description").toString());
            assertEquals("\"tuser2_id\"", user.get("_id").toString());
            assertEquals("\"tuser2\"", user.get("userName").toString());

            connector.opReconcile(Collections.unmodifiableMap(reconAttrs));
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    /*
    {
        "userName":"tuser2",
        "givenName":"Test",
        "sn":"User",
        "_id":"tuser2_id",
        "mail":"tuser@example.com",
        "testAttr":["one", "two", "three"]
    }
     */

    public void testReconcileMappedSystem() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String id = null;
        String username = "bwayneuser";
        String mapping = "systemLdapAccount_managedUser";

        String createAttrs = "{\n" +
                "        \"givenName\":\"Bruce\",\n" +
                "        \"sn\":\"Wayne\",\n" +
                "        \"mail\":\"bwayne@example.com\",\n" +
                "        \"dn\":\"cn=bwayneuser,ou=People,dc=test\",\n" +
                "        \"cn\":\"bwayneuser\"\n}";

        Map<String, Collection<String>> reconAttrs = new HashMap<String, Collection<String>>();

        reconAttrs.put("dn", singleValue("cn=bwayneuser,ou=People,dc=test"));
        reconAttrs.put("mapping", singleValue("systemLdapAccount_managedUser"));

        try {
            rest.executePost("/system/ldap/account?_action=create", createAttrs);
            String queryString = String.format("/system/ldap/account?_queryFilter=%s eq \"%s\"&_fields=_id,%2$s", "cn", "bwayneuser");
            JsonObject userFromMappedSystem = new JsonParser().parse(rest.executeGet(queryString.replaceAll(" ", "%20")).messageBody).getAsJsonObject();
            if (userFromMappedSystem.get("result").getAsJsonArray().size() == 0) {
                throw new IdMUnitException(String.format("User %s was not found in mapped system %s.", username, mapping));
            }
            id = userFromMappedSystem.get("result").getAsJsonArray().get(0).getAsJsonObject().get("_id").getAsString();
            connector.opReconcile(Collections.unmodifiableMap(reconAttrs));

        } finally {
            rest.executePost("/sync?_action=performAction&sourceId=" + id + "&mapping=systemLdapAccount_managedUser&action=DELETE");
            rest.executePost("/sync?_action=performAction&sourceId=" + id + "&mapping=systemLdapAccount_managedUser&action=UNLINK");
            rest.executeDelete("/system/ldap/account/" + id);
        }
    }

    public void testLiveSync() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("sourceSystem", singleValue("system/ldap/account"));
        connector.opLiveSync(attrs);
    }

    public void testValidateStringArrayAttr() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        try {
            String createAttrs = "{\n" +
                    "        \"userName\":\"tuser2\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"_id\":\"tuser2_id\",\n" +
                    "        \"mail\":\"testuser@example.com\",\n" +
                    "        \"testAttr\":[\"one\", \"two\", \"three\"]\n" +
                    "    }";

            rest.executePost("/managed/user?_action=create", createAttrs);

            Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
            attrs.put("userName", singleValue("tuser2"));
            attrs.put("givenName", singleValue("Test"));
            attrs.put("sn", singleValue("User"));
            attrs.put("mail", singleValue("testuser@example.com"));
            attrs.put("testAttr[]", singleValue("four"));
            attrs.put("objectType", singleValue("user"));

            try {
                connector.opValidateObject(Collections.unmodifiableMap(attrs));
            } catch (IdMUnitFailureException e) {
                assertEquals("'.testAttr' attribute mismatch: expected item \"four\" was not found in [\"one\",\"two\",\"three\"]", e.getMessage());
            }
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    public void testValidateExactMatch() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        try {
            String createAttrs = "{\n" +
                    "        \"userName\":\"tuser2\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"mail\":\"test.user@example.com\",\n" +
                    "        \"_id\":\"tuser2_id\",\n" +
                    "        \"testAttr\":[\"one\", \"two\", \"three\"]\n" +
                    "    }";

            rest.executePost("/managed/user?_action=create", createAttrs);

            Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
            attrs.put("userName", singleValue("tuser2"));
            attrs.put("givenName", singleValue("Test"));
            attrs.put("testAttr[]", multiValue("two", "one", "three"));
            attrs.put("objectType", singleValue("user"));

            connector.opValidateObjectExact(Collections.unmodifiableMap(attrs));
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    public void testValidateExactMatchFailure() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        try {
            String createAttrs = "{\n" +
                    "        \"userName\":\"tuser2\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"mail\":\"test.user@example.com\",\n" +
                    "        \"_id\":\"tuser2_id\",\n" +
                    "        \"testAttr\":[\"one\", \"two\", \"three\"]\n" +
                    "    }";

            rest.executePost("/managed/user?_action=create", createAttrs);

            Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
            attrs.put("userName", singleValue("tuser2"));
            attrs.put("givenName", singleValue("Test"));
            attrs.put("testAttr[]", multiValue("two", "one", "three", "four"));
            attrs.put("objectType", singleValue("user"));

            try {
                connector.opValidateObjectExact(Collections.unmodifiableMap(attrs));
            } catch (IdMUnitFailureException e) {
                assertEquals("'.testAttr' attribute mismatch: actual item contains 3 values when our expected item contains 4 values. \nExpected values: [\"two\",\"one\",\"three\",\"four\"] \nActual values: [\"one\",\"two\",\"three\"] \n'.testAttr' attribute mismatch: expected item \"four\" was not found in [\"one\",\"two\",\"three\"]", e.getMessage());
            }
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }



     /*
    {
        "userName":"tuser2",
        "givenName":"Test",
        "sn":"User",
        "_id":"tuser2_id",
        "mail":"tuser@example.com",
        "testAttr":["one", "two", "three"]
    }
     */

    public void testValidateNullAttrs() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        try {
            String managedComplexUser = "{\n" +
                    "        \"userName\":\"tuser2\",\n" +
                    "        \"givenName\":\"Test\",\n" +
                    "        \"sn\":\"User\",\n" +
                    "        \"_id\":\"tuser2_id\",\n" +
                    "        \"mail\":\"tuser@example.com\",\n" +
                    "        \"testAttr\":[\"one\", \"two\", \"three\"]\n" +
                    "    }";

            rest.executePost("/managed/user?_action=create", managedComplexUser);

            Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
            attrs.put("userName", singleValue("tuser2"));
            attrs.put("givenName", singleValue("Test"));
            attrs.put("sn", singleValue("User"));
            attrs.put("mail", singleValue("tuser@example.com"));
            attrs.put("manager", singleValue("[EMPTY]"));
            attrs.put("description", singleValue("[EMPTY]"));
            attrs.put("testAttr[]", multiValue("one", "two", "three"));
            attrs.put("objectType", singleValue("user"));

            connector.opValidateObject(Collections.unmodifiableMap(attrs));

        } finally {

            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    public void testValidateBooleanAttrs() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        try {
            Map<String, Collection<String>> createAttrs = new HashMap<String, Collection<String>>();
            createAttrs.put("userName", singleValue("bwayne13"));
            createAttrs.put("givenName", singleValue("Bruce"));
            createAttrs.put("sn", singleValue("Wayne"));
            createAttrs.put("mail", singleValue("BadTest@yahooligans.com"));
            createAttrs.put("telephoneNumber", singleValue("555-555-1212"));
            createAttrs.put("description", singleValue("My user to validate"));
            createAttrs.put("_id", singleValue("bwayneid"));
            createAttrs.put("objectType", singleValue("user"));
            createAttrs.put("attrBoolean::boolean", singleValue("true"));

            connector.opAddObject(createAttrs);

            Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
            attrs.put("userName", singleValue("bwayne13"));
            attrs.put("_id", singleValue("bwayneid"));
            attrs.put("givenName", singleValue("Bruce"));
            attrs.put("attrBoolean::boolean", singleValue("true"));
            attrs.put("objectType", singleValue("user"));

            connector.opValidateObject(Collections.unmodifiableMap(attrs));

        } finally {

            rest.executeDelete("/managed/user/bwayneid");
        }
    }

    public void testValidateUserDoesNotExist() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("userName", singleValue("tuser2"));
        attrs.put("objectType", singleValue("user"));

        connector.opValidateObjectDoesNotExist(Collections.unmodifiableMap(attrs));
    }

     /*
    {
        "userName":"tuser2",
        "givenName":"Test",
        "sn":"User",
        "_id":"tuser2_id",
        "mail":"tuser@example.com",

    }
     */

    public void testValidateUserDoesNotExistFailure() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"userName\":\"tuser2\",\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"_id\":\"tuser2_id\",\n" +
                "        \"mail\":\"tuser@example.com\"\n" +
                "        \n" +
                "    }";

        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("givenName", singleValue("Test"));
        attrs.put("sn", singleValue("User"));
        attrs.put("mail", singleValue("tuser@example.com"));
        attrs.put("_id", singleValue("tuser2_id"));
        attrs.put("userName", singleValue("tuser2"));
        attrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opValidateObjectDoesNotExist(Collections.unmodifiableMap(attrs));
        } catch (IdMUnitFailureException e) {
            assertEquals("There is a user that exists with this username", e.getMessage());
        } finally {
            rest.executeDelete("/managed/user/tuser2_id");
        }
    }

    /*
    {
        "givenName":"Test",
        "sn":"User",
        "mail":"tuser@example.com",
        "telephoneNumber":"555-555-1212",
        "password":"T3stPassw0rd",
        "_id":"tuserid",
        "userName":"tuser13",
    }
     */

    public void testAddSingleAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"_id\":\"tuserid\",\n" +
                "        \"userName\":\"tuser13\"\n" +
                "    }";

        Map<String, Collection<String>> addAttrs = new HashMap<String, Collection<String>>();
        addAttrs.put("_id", singleValue("tuserid"));
        addAttrs.put("description", singleValue("My user to patch"));
        addAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opAddAttribute(addAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/tuserid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"My user to patch\"", user.get("description").toString());

        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testAddMultipleAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"_id\":\"tuserid\",\n" +
                "        \"userName\":\"tuser13\"\n" +
                "    }";

        Map<String, Collection<String>> addAttrs = new HashMap<String, Collection<String>>();
        addAttrs.put("_id", singleValue("tuserid"));
        addAttrs.put("description", singleValue("My user to patch"));
        addAttrs.put("telephoneNumber", singleValue("555-555-1212"));
        addAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opAddAttribute(addAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/tuserid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"My user to patch\"", user.get("description").toString());
            assertEquals("\"555-555-1212\"", user.get("telephoneNumber").toString());


        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testModifySingleAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"description\":\"My user to patch\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"_id\":\"tuserid\",\n" +
                "        \"userName\":\"tuser13\"\n" +
                "    }";

        Map<String, Collection<String>> modAttrs = new HashMap<String, Collection<String>>();
        modAttrs.put("_id", singleValue("tuserid"));
        modAttrs.put("description", singleValue("My modified description"));
        modAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opReplaceAttribute(modAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/tuserid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"My modified description\"", user.get("description").toString());

        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testModifyMultipleAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"description\":\"My user to patch\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"_id\":\"tuserid\",\n" +
                "        \"userName\":\"tuser13\"\n" +
                "    }";

        Map<String, Collection<String>> modAttrs = new HashMap<String, Collection<String>>();
        modAttrs.put("_id", singleValue("tuserid"));
        modAttrs.put("description", singleValue("My modified description"));
        modAttrs.put("telephoneNumber", singleValue("111-222-1313"));
        modAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opReplaceAttribute(modAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/tuserid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"My modified description\"", user.get("description").toString());
            assertEquals("\"111-222-1313\"", user.get("telephoneNumber").toString());

        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testModifyComplexAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
         /*
            {
            "userName":"tuser13",
            "sn":"User",
            "givenName":"Test",
            "_id":"tuserid",
            "mail":"testuser@example.com",
            "testAttr":"["one", "two", "three"]"
            }
             */
        String managedComplexUser = "{\n" +
                "            \"userName\":\"tuser13\",\n" +
                "            \"sn\":\"User\",\n" +
                "            \"givenName\":\"Test\",\n" +
                "            \"_id\":\"tuserid\",\n" +
                "            \"mail\":\"testuser@example.com\",\n" +
                "            \"testAttr\":[\"one\", \"two\", \"three\"]\n" +
                "            }";

        Map<String, Collection<String>> patchAttrs = new HashMap<String, Collection<String>>();
        patchAttrs.put("_id", singleValue("tuserid"));
        patchAttrs.put("objectType", singleValue("user"));
        patchAttrs.put("testAttr[]", multiValue("four", "five", "six"));

        try {
            rest.executePost("/managed/user?_action=create", managedComplexUser);

            connector.opReplaceAttribute(patchAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/tuserid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"testuser@example.com\"", user.get("mail").toString());
            assertEquals("[\"four\",\"five\",\"six\"]", user.get("testAttr").toString());

        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testModifyBooleanAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        Map<String, Collection<String>> createAttrs = new HashMap<String, Collection<String>>();
        createAttrs.put("userName", singleValue("bwayne13"));
        createAttrs.put("givenName", singleValue("Bruce"));
        createAttrs.put("sn", singleValue("Wayne"));
        createAttrs.put("mail", singleValue("BadTest@yahooligans.com"));
        createAttrs.put("telephoneNumber", singleValue("555-555-1212"));
        createAttrs.put("description", singleValue("My user to validate"));
        createAttrs.put("_id", singleValue("bwayneid"));
        createAttrs.put("objectType", singleValue("user"));
        createAttrs.put("attrBoolean::boolean", singleValue("true"));


        Map<String, Collection<String>> modAttrs = new HashMap<String, Collection<String>>();
        modAttrs.put("_id", singleValue("bwayneid"));
        modAttrs.put("description", singleValue("My modified description"));
        modAttrs.put("objectType", singleValue("user"));
        modAttrs.put("attrBoolean::boolean", singleValue("false"));


        try {
            connector.opAddObject(createAttrs);

            connector.opReplaceAttribute(modAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/bwayneid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"My modified description\"", user.get("description").toString());
            assertEquals(false, user.get("attrBoolean").getAsBoolean());

        } finally {
            rest.executeDelete("/managed/user/bwayneid");
        }
    }

    public void testValidateStringAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"description\":\"My user to patch\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"_id\":\"tuserid\",\n" +
                "        \"userName\":\"tuser13\",\n" +
                "        \"objectType\":\"user\"\n" +
                "        }";

        rest.executePost("/managed/user?_action=create", createAttrs);

        Map<String, Collection<String>> validateAttrs = new HashMap<>();
        validateAttrs.put("_id", singleValue("tuserid"));
        validateAttrs.put("objectType", singleValue("user"));
        validateAttrs.put("description::string", singleValue("A String ValueShouldFail"));

        try {
            connector.opValidateObject(validateAttrs);
            fail("Test Should have failed! Provided validation value was incorrect!");
        } catch (IdMUnitFailureException e) {
            assertEquals("'.description' attribute mismatch: expected \"A String ValueShouldFail\" but was \"My user to patch\"", e.getMessage());
        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testRemoveSingleAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        /*
        {
        "givenName":"Test",
        "sn":"User",
        "mail":"tuser@example.com",
        "telephoneNumber":"555-555-1212",
        "description":"My user to patch",
        "password":"T3stPassw0rd",
        "_id":"tuserid",
        "userName":"tuser13"
        }
         */
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"description\":\"My user to patch\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"_id\":\"tuserid\",\n" +
                "        \"userName\":\"tuser13\"\n" +
                "        }";

        Map<String, Collection<String>> removeAttrs = new HashMap<String, Collection<String>>();
        removeAttrs.put("_id", singleValue("tuserid"));
        removeAttrs.put("description", singleValue("My modified description"));
        removeAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opRemoveAttribute(removeAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/tuserid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("\"My modified description\"", user.get("description").toString());

        } catch (NullPointerException e) {
            System.out.println("We expect this exception since our description has been removed");

        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testRemoveMultipleAttribute() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "openidm-admin", "openidm-admin", false);
        String createAttrs = "{\n" +
                "        \"givenName\":\"Test\",\n" +
                "        \"sn\":\"User\",\n" +
                "        \"mail\":\"tuser@example.com\",\n" +
                "        \"telephoneNumber\":\"555-555-1212\",\n" +
                "        \"description\":\"My user to patch\",\n" +
                "        \"password\":\"T3stPassw0rd\",\n" +
                "        \"_id\":\"tuserid\",\n" +
                "        \"userName\":\"tuser13\"\n" +
                "        }";

        Map<String, Collection<String>> removeAttrs = new HashMap<String, Collection<String>>();
        removeAttrs.put("_id", singleValue("tuserid"));
        removeAttrs.put("description", singleValue("My modified description"));
        removeAttrs.put("telephoneNumber", singleValue("555-555-1212"));
        removeAttrs.put("objectType", singleValue("user"));

        try {
            rest.executePost("/managed/user?_action=create", createAttrs);

            connector.opRemoveAttribute(removeAttrs);

            RestClient.Response response = rest.executeGet("/managed/user/tuserid?_fields=*,*_ref");
            JsonObject user = new JsonParser().parse(response.messageBody).getAsJsonObject();
            assertEquals("", user.get("description").toString());
            assertEquals("", user.get("telephoneNumber").toString());

        } catch (NullPointerException e) {
            System.out.println("We expect this exception since our description has been removed");
        } finally {
            rest.executeDelete("/managed/user/tuserid");
        }
    }

    public void testBadAuth() throws IdMUnitException {
        RestClient rest = RestClient.init(TEST_SERVER, "8080", "badusername", "badpassword", false);
        try {
            // attempt any rest connection:
            RestClient.Response response = rest.executeGet("");
            fail("Should have failed authentication!");
        } catch (IdMUnitException e) {
            assertEquals("Unauthorized(401): Access Denied", e.getMessage());
        }
    }

    public void testValidatePasswordInternalUserSucceeds() throws IdMUnitException {
        Map<String, Collection<String>> validatePassword = new HashMap<String, Collection<String>>();
        validatePassword.put("objectType", singleValue("user"));
        validatePassword.put("userName", singleValue("openidm-admin"));
        validatePassword.put("password", singleValue("openidm-admin"));

        connector.opValidatePassword(validatePassword);
    }

    public void testValidatePasswordInternalUserFails() throws IdMUnitException {
        Map<String, Collection<String>> validatePassword = new HashMap<String, Collection<String>>();
        validatePassword.put("objectType", singleValue("user"));
        validatePassword.put("userName", singleValue("openidm-admin"));
        validatePassword.put("password", singleValue("badPassword"));

        try {
            connector.opValidatePassword(validatePassword);
            fail("Validate Password should have failed, the wrong password was specified!");
        } catch (IdMUnitFailureException e) {
            assertEquals("Validate password failed for user [openidm-admin]", e.getMessage());
        }
    }

}
