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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.trivir.idmunit.connector.api.resource.Alias;
import com.trivir.idmunit.connector.api.resource.AliasArray;
import com.trivir.idmunit.connector.api.util.ApiInternalException;
import com.trivir.idmunit.connector.api.util.HttpRequest;
import com.trivir.idmunit.connector.rest.RestClient;
import org.idmunit.IdMUnitException;

import java.util.*;

import static com.trivir.idmunit.connector.api.util.RestUtil.*;
import static com.trivir.idmunit.connector.util.JavaUtil.*;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;

//TODO: Remove traces of IdmUnit (e.g, IDMUnitExceptions)
//TODO: Add more intelligent 400 error handling (i.e, distinguish between alias not being available yet vs. bad email address)
public final class AliasApi {

    public static final Collection<String> SCOPES;
    //Gson is thread-safe
    private static final Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Collection<String> aliasScopes = Arrays.asList(
                "https://www.googleapis.com/auth/admin.directory.user",
                "https://www.googleapis.com/auth/admin.directory.user.readonly",
                "https://www.googleapis.com/auth/admin.directory.user.alias",
                "https://www.googleapis.com/auth/admin.directory.user.alias.readonly"
            );
        aliasScopes = Collections.unmodifiableCollection(aliasScopes);

        GSON = gson;
        SCOPES = aliasScopes;
    }

    private static List<Alias> normalizeAlias(List<Alias> aliases) {
        if (isNullOrEmpty(aliases)) {
            return aliases;
        }

        for (Alias alias : aliases) {
            alias.normalize();
        }

        return aliases;
    }

    public static Alias insertAlias(RestClient rest, String userKey, String aliasEmail) throws IdMUnitException {
        return insertAlias(rest, Alias.Factory.newAlias(userKey, aliasEmail));
    }

    public static Alias insertAlias(RestClient rest, Alias alias) throws IdMUnitException {
        final Set<Integer> returnCodes = HTTP_CODES_RETURN_ALWAYS;

        checkNotNull("rest", rest);
        checkNotNull("alias", alias);

        alias.normalize();
        String userKey = alias.getUserKey();
        String aliasEmail = alias.getAlias();
        if (isBlank(userKey)) {
            throw new IdMUnitException("Error: userKey is empty and should be populated");
        } else if (isBlank(aliasEmail)) {
            throw new IdMUnitException("Error: alias is empty and should be populated");
        }

        JsonParser jp = new JsonParser();
        String body = GSON.toJson(jp.parse(GSON.toJson(alias)));
        String path = String.format(Path.PATH_INSERT, alias.getUserKey());
        HttpRequest httpRequest = HttpRequest.Factory.newPostRequest(path, body);

        try {
            RestClient.Response response = executeWithRetry(rest, httpRequest, returnCodes);

            if (response.getStatusCode() >= HTTP_MULT_CHOICE) {
                throw new ApiInternalException(String.format("Unable to insert alias %s for user %s. Status Code: %d. Message: %s", aliasEmail, userKey, response.getStatusCode(), response.getMessageBody()));
            } else {
                return newAliasFromJson(GSON, jp, response.getMessageBody());
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to insert alias %s for user %s", aliasEmail, userKey), e);
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }
    }

    public static boolean deleteAlias(RestClient rest, String userKey, String aliasEmail) throws IdMUnitException {
        final Set<Integer> returnCodes = new ImmutableSet.Builder<Integer>()
            .addAll(HTTP_CODES_RETURN_ALWAYS)
            .addAll(HTTP_CODES_RETURN_ON_DELETE_SUCCESS)
            .build();

        checkNotNull("rest", rest);

        if (isBlank(userKey)) {
            throw new IdMUnitException("Error: userKey is empty and should be populated");
        } else if (isBlank(aliasEmail)) {
            throw new IdMUnitException("Error: alias is empty and should be populated");
        }

        String path = String.format(Path.PATH_DELETE, userKey, aliasEmail);
        HttpRequest request = HttpRequest.Factory.newDeleteRequest(path, null);
        boolean deleted;

        try {
            RestClient.Response response = executeWithRetry(rest, request, returnCodes);

            int code = response.getStatusCode();
            deleted = code < HTTP_MULT_CHOICE;
            if (!deleted && !HTTP_CODES_RETURN_ON_DELETE_SUCCESS.contains(code)) {
                String msg = getMessage(response);
                throw new IdMUnitException(String.format("Unable to delete alias %s for user %s. Status Code: %d. Message: %s", aliasEmail, userKey, code, msg));
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to delete alias %s for user %s", aliasEmail, userKey), e);
        }

        return deleted;
    }

    public static boolean deleteAlias(RestClient rest, Alias alias) throws IdMUnitException {
        checkNotNull("alias", alias);
        alias.normalize();
        return deleteAlias(rest, alias.getUserKey(), alias.getAlias());
    }

    public static boolean deleteAllAliases(RestClient rest, String userKey) throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotNull("userKey", userKey);

        List<String> aliases = listAliasesEmailOnly(rest, userKey);

        boolean someExisted = false;
        boolean allDeleted = true;
        for (String aliasEmail : aliases) {
            someExisted = true;
            allDeleted = allDeleted && deleteAlias(rest, userKey, aliasEmail);
        }

        return someExisted && allDeleted;
    }

    public static Alias getAlias(RestClient rest, String userKey, String aliasEmail) throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotBlank("aliasEmail", aliasEmail);

        Alias match = null;
        List<Alias> aliases = listAliases(rest, userKey);
        for (Alias alias : aliases) {
            if (alias != null) {
                if (aliasEmail.equals(alias.getAlias())) {
                    alias.normalize();
                    match = alias;
                    break;
                }
            }
        }

        return match;
    }

    public static String getAliasEmailOnly(RestClient rest, String userKey, String aliasEmail) throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotBlank("aliasEmail", aliasEmail);

        String match = "";
        List<String> emails = listAliasesEmailOnly(rest, userKey);
        for (String email : emails) {
            if (aliasEmail.equals(email)) {
                match = email;
                break;
            }
        }

        return match;
    }

    public static String getAliasEmailOnly(RestClient rest, Alias alias) throws IdMUnitException {
        checkNotNull("alias", alias);
        alias.normalize();
        return getAliasEmailOnly(rest, alias.getUserKey(), alias.getAlias());
    }

    public static boolean hasAlias(RestClient rest, Alias alias) throws IdMUnitException {
        return !"".equals(getAliasEmailOnly(rest, alias));
    }

    public static boolean hasAlias(RestClient rest, String userKey, String aliasEmail) throws IdMUnitException {
        return !"".equals(getAliasEmailOnly(rest, userKey, aliasEmail));
    }

    public static boolean hasAliases(RestClient rest, String userKey, Collection<String> aliasEmails) throws IdMUnitException {
        checkNotNullOrEmpty("aliasEmails", aliasEmails);
        return listAliasesEmailOnly(rest, userKey).containsAll(aliasEmails);
    }

    public static Alias getAlias(RestClient rest, Alias alias) throws IdMUnitException {
        checkNotNull("alias", alias);
        alias.normalize();
        return getAlias(rest, alias.getUserKey(), alias.getAlias());
    }

    public static List<Alias> listAliases(RestClient rest, String userKey) throws IdMUnitException {
        final Set<Integer> returnCodes = new ImmutableSet.Builder<Integer>()
            .addAll(HTTP_CODES_RETURN_ALWAYS)
            .addAll(HTTP_CODES_RETURN_ON_NOT_FOUND)
            .build();

        checkNotNull("rest", rest);
        checkNotBlank("userKey", userKey);

        AliasArray aliasList = null;
        String path = String.format(Path.PATH_LIST, userKey);
        HttpRequest request = HttpRequest.Factory.newGetRequest(path);

        try {
            RestClient.Response response = executeWithRetry(rest, request, returnCodes);

            int code = response.getStatusCode();
            if (code < HTTP_MULT_CHOICE) {
                aliasList = newListAliasFromJson(GSON, new JsonParser(), response.getMessageBody());
            } else if (!HTTP_CODES_RETURN_ON_NOT_FOUND.contains(code)) {
                String msg = getMessage(response);
                throw new ApiInternalException(String.format("Unable to list alias emails for user %s. Status Code: %d. Message: %s", userKey, code, msg));
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to list alias emails for user %s", userKey), e);
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }

        return mapNullToEmpty((aliasList == null) ? null : normalizeAlias(aliasList.aliases));
    }

    public static List<String> listAliasesEmailOnly(RestClient rest, String userKey) throws IdMUnitException {
        List<Alias> aliasObjects = mapNullToEmpty(listAliases(rest, userKey));
        List<String> aliasEmails = new ArrayList<String>(aliasObjects.size());
        String aliasEmail;
        for (Alias a : aliasObjects) {
            aliasEmail = a.getAlias();
            if (!isBlank(aliasEmail)) {
                aliasEmails.add(aliasEmail);
            }
        }

        return aliasEmails;
    }

    public static Alias newAliasFromJson(Gson gson, JsonParser parser, String json) {
        Alias alias = (Alias)newObjectFromJson(gson, parser, json, Alias.class);
        alias.normalize();
        return alias;
    }

    public static AliasArray newListAliasFromJson(Gson gson, JsonParser parser, String json) {
        return (AliasArray)newObjectFromJson(gson, parser, json, AliasArray.class);
    }

    public static final class Path {

        public static final String PATH_ROOT = "/admin/directory/v1/users";

        //use String.format() to replace path variables
        public static final String PATH_DELETE = PATH_ROOT + "/%s/aliases/%s"; //userKey
        public static final String PATH_LIST = PATH_ROOT + "/%s/aliases"; // userKey, alias
        public static final String PATH_INSERT = PATH_ROOT + "/%s/aliases"; //userKey
    }

}
