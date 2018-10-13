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

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trivir.idmunit.connector.api.resource.UserArray;
import com.trivir.idmunit.connector.api.resource.User;
import com.trivir.idmunit.connector.api.util.ApiInternalException;
import com.trivir.idmunit.connector.api.util.RestUtil;
import com.trivir.idmunit.connector.api.util.HttpRequest;
import com.trivir.idmunit.connector.rest.RestClient;
import com.trivir.idmunit.connector.util.JsonUtil;
import org.idmunit.IdMUnitException;

import java.util.*;

import static com.trivir.idmunit.connector.api.UserApi.Path.PATH_ROOT;
import static com.trivir.idmunit.connector.api.UserApi.Path.PATH_USER;
import static com.trivir.idmunit.connector.api.util.RestUtil.*;
import static com.trivir.idmunit.connector.util.JavaUtil.*;
import static com.trivir.idmunit.connector.util.JsonUtil.parseError;
import static java.net.HttpURLConnection.*;

//TODO: Remove traces of IdmUnit (e.g, IDMUnitExceptions)
public final class UserApi {

    //Gson is thread-safe
    private static final Gson GSON = new Gson();

    private static UserArray newListUserFromJson(Gson gson, JsonParser parser, String json) {
        return (UserArray)newObjectFromJson(gson, parser, json, UserArray.class);
    }

    private static User newUserFromJson(Gson gson, JsonParser parser, String json) {
        return (User)newObjectFromJson(gson, parser, json, User.class);
    }

    public static List<User> listUsers(RestClient rest, Map<String, String> queryParams) throws IdMUnitException {
        final String queryParamPageToken = "pageToken";
        final Set<Integer> returnCodes = new ImmutableSet.Builder<Integer>()
            .addAll(HTTP_CODES_RETURN_ALWAYS)
            .addAll(HTTP_CODES_RETURN_ON_NOT_FOUND)
            .build();

        checkNotNull("rest", rest);
        queryParams = mapNullToEmpty(queryParams);

        List<User> users = new ArrayList<User>();
        UserArray userList = null;
        String path = PATH_ROOT;
        HttpRequest request = HttpRequest.Factory.newGetRequest(path, queryParams);

        String pageToken = null;

        do {
            try {
                if (isBlank(pageToken)) {
                    queryParams.remove(queryParamPageToken);
                } else {
                    queryParams.put(queryParamPageToken, pageToken);
                }

                RestClient.Response response = executeWithRetry(rest, request, returnCodes);

                int code = response.getStatusCode();
                if (code < HTTP_MULT_CHOICE) {
                    userList = newListUserFromJson(GSON, new JsonParser(), response.getMessageBody());
                    if (userList == null) {
                        pageToken = null;
                    } else {
                        pageToken = userList.nextPageToken;
                        users.addAll(mapNullToEmpty(userList.users));
                    }
                } else if (!HTTP_CODES_RETURN_ON_NOT_FOUND.contains(code)) {
                    String msg = getMessage(response);
                    throw new ApiInternalException(String.format("Unable to list users. Status Code: %d. Message: %s", code, msg));
                }
            } catch (IdMUnitException e) {
                throw new IdMUnitException("Unable to list users", e);
            } catch (ApiInternalException e) {
                throw new IdMUnitException(e.getMessage());
            }
        } while (!isBlank(pageToken));

        return users;
    }

    public static List<User> listUsersInDomain(RestClient rest, String domain) throws IdMUnitException {
        checkNotBlank("domain", domain);

        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("domain", domain);
        return listUsers(rest, queryParams);
    }

    //delete suspended for sure
    public static void deleteUsersInDomain(RestClient rest, String domain, List<String> except) throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotBlank("domain", domain);
        except = mapNullToEmpty(except);

        List<User> users = listUsersInDomain(rest, domain);
        for (User user : users) {
            if (!user.getIsAdmin()) {
                String email = user.getPrimaryEmail();
                if (!except.contains(email)) {
                    deleteUser(rest, email);
                }
            }
        }
    }

    public static void deleteUser(RestClient rest, String username) throws IdMUnitException {
        checkNotNull("rest", rest);

        RestClient.Response response = rest.executeDelete(String.format(PATH_USER, username));
        // If the user doesn't exist then the status code will be
        // HTTP_NO_CONTENT and there will be no messageBody content.
        if (response.getStatusCode() != HTTP_OK && response.getStatusCode() != HTTP_NO_CONTENT) {
            if (response.getMessageBody() == null || response.getMessageBody().length() == 0) {
                throw new IdMUnitException(String.format("Error %d deleting user '%s': '%s'",
                    response.getStatusCode(), username, response.getReasonPhrase()));
            } else {
                String msg = JsonUtil.parseError(response.getMessageBody());
                if (!"Resource Not Found: userKey".equals(msg)) {
                    throw new IdMUnitException(String.format("Error deleting user '%s': '%s'", username, msg));
                }
            }
            String msg = (response.getMessageBody() != null && response.getMessageBody().length() > 0) ? JsonUtil.parseError(response.getMessageBody()) : "";
            if (!"Resource Not Found: userKey".equals(msg)) {
                throw new IdMUnitException(String.format("Error deleting user '%s'", username));
            }
        }
    }

    public static User getUser(RestClient rest, String username) throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotBlank("username", username);

        User user;
        String path = String.format(PATH_USER, username);
        RestClient.Response response = rest.executeGet(path);
        if (response.getStatusCode() < HTTP_BAD_REQUEST) {
            JsonParser jp = new JsonParser();
            JsonElement el = jp.parse(response.getMessageBody());
            JsonObject jobject = el.getAsJsonObject();
            user = GSON.fromJson(jobject, User.class);
        } else {

            response = RestUtil.retryGet(rest, path);

            if (response.getStatusCode() == HTTP_OK) {
                JsonParser jp = new JsonParser();
                JsonElement el = jp.parse(response.getMessageBody());
                JsonObject jobject = el.getAsJsonObject();
                user = GSON.fromJson(jobject, User.class);
            } else {
                String msg = parseError(response.getMessageBody());

                if ("Resource Not Found: userKey".equals(msg)) {
                    throw new IdMUnitException(String.format("Error user '%s' does not exist", username));
                } else {
                    throw new IdMUnitException(String.format("Error retrieving user '%s'", username));
                }
            }
        }

        return user;
    }

    //TODO: move validation code from NewUser constructor into this method
    public static User insertUser(RestClient rest, User toInsert) throws IdMUnitException {
        final Set<Integer> returnCodes = HTTP_CODES_RETURN_ALWAYS;

        checkNotNull("rest", rest);
        checkNotNull("user", toInsert);

        //allow for " " values for backward compatibility
        if (isBlank(toInsert.getPrimaryEmail(), false)) {
            throw new IdMUnitException("Error: username is empty and should be populated");
        } else if (isBlank(toInsert.getGivenName(), false)) {
            throw new IdMUnitException("Error: givenName is empty and should be populated");
        } else if (isBlank(toInsert.getFamilyName(), false)) {
            throw new IdMUnitException("Error: familyName is empty and should be populated");
        } else if (isBlank(toInsert.getPassword(), false)) {
            throw new IdMUnitException("Error: password is empty and should be populated");
        } else if (isBlank(toInsert.getOrgUnitPath(), false)) {
            toInsert.setOrgUnitPath(null);
        }

        JsonParser jp = new JsonParser();
        String body = GSON.toJson(jp.parse(GSON.toJson(toInsert)));
        String path = PATH_ROOT;
        HttpRequest httpRequest = HttpRequest.Factory.newPostRequest(path, body);

        try {
            RestClient.Response response = executeWithRetry(rest, httpRequest, returnCodes);

            if (response.getStatusCode() >= HTTP_MULT_CHOICE) {
                throw new ApiInternalException(String.format("Unable to insert user %s. Status Code: %d. Message: %s", toInsert.primaryEmail, response.getStatusCode(), response.getMessageBody()));
            } else {
                User inserted = newUserFromJson(GSON, jp, response.getMessageBody());
                return inserted;
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to insert user %s", toInsert.primaryEmail), e);
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }
    }

    public static class Factory {

        public static User newUser(String primaryEmail,
                                   String givenName,
                                   String familyName,
                                   String password) {
            User user = new User();
            user.setPrimaryEmail(primaryEmail);
            user.setGivenName(givenName);
            user.setFamilyName(familyName);
            user.setPassword(password);

            return user;
        }
    }

    public static final class Path {

        public static final String PATH_ROOT = "/admin/directory/v1/users";
        public static final String PATH_USER = PATH_ROOT + "/%s";
    }

}
