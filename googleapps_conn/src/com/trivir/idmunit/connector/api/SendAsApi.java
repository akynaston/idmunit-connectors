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
import com.trivir.idmunit.connector.api.resource.SendAs;
import com.trivir.idmunit.connector.api.resource.SendAsArray;
import com.trivir.idmunit.connector.api.util.ApiInternalException;
import com.trivir.idmunit.connector.api.util.HttpRequest;
import com.trivir.idmunit.connector.api.util.RestUtil;
import com.trivir.idmunit.connector.rest.RestClient;
import com.trivir.idmunit.connector.util.JavaUtil;
import org.idmunit.IdMUnitException;

import java.security.PrivateKey;
import java.util.*;

import static com.trivir.idmunit.connector.api.util.RestUtil.*;
import static com.trivir.idmunit.connector.util.JavaUtil.*;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;

//TODO: Remove traces of IdmUnit (e.g, IDMUnitExceptions)
//TODO: Add more intelligent 400 error handling (i.e, distinguish between alias not being available yet vs. bad email address)
public final class SendAsApi {

    public static final Collection<String> SCOPES;

    //Gson is thread-safe
    private static final Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Collection<String> sendAsScopes = Arrays.asList(
            "https://www.googleapis.com/auth/gmail.modify",
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/gmail.settings.basic",
            "https://www.googleapis.com/auth/gmail.settings.sharing");
        sendAsScopes = Collections.unmodifiableCollection(sendAsScopes);

        GSON = gson;
        SCOPES = sendAsScopes;
    }

    private static List<SendAs> normalizeSendAs(List<SendAs> sendAses) {
        if (isNullOrEmpty(sendAses)) {
            return sendAses;
        }

        for (SendAs sendAs : sendAses) {
            sendAs.normalize();
        }

        return sendAses;
    }

    public static SendAs createSendAs(RestClient rest, String userId, String sendAsEmail) throws IdMUnitException {
        return createSendAs(rest, SendAs.Factory.newSendAs(userId, sendAsEmail));
    }

    public static SendAs createSendAs(RestClient rest, SendAs sendAs) throws IdMUnitException {
        final Set<Integer> returnCodes = HTTP_CODES_RETURN_ALWAYS;

        checkNotNull("rest", rest);
        checkNotNull("sendAs", sendAs);

        sendAs.normalize();
        String userId = sendAs.getUserId();
        String sendAsEmail = sendAs.getSendAsEmail();
        if (isBlank(userId)) {
            throw new IdMUnitException("Error: userId is empty and should be populated");
        }
        if (isBlank(sendAsEmail)) {
            throw new IdMUnitException("Error: sendAsEmail is empty and should be populated");
        }

        JsonParser jp = new JsonParser();
        String body = GSON.toJson(jp.parse(GSON.toJson(sendAs)));
        String path = String.format(Path.PATH_CREATE, userId);
        HttpRequest httpRequest = HttpRequest.Factory.newPostRequest(path, body);

        try {
            //NOTE: I've seen cases where 3 retries isn't sufficient enough wait time
            RestClient.Response response = executeWithRetry(4, 1, rest, httpRequest, returnCodes);

            if (response.getStatusCode() >= HTTP_MULT_CHOICE) {
                throw new ApiInternalException(String.format("Unable to create sendAs email %s for user %s. Status Code: %d. Message: %s", sendAsEmail, userId, response.getStatusCode(), response.getMessageBody()));
            } else {
                return newSendAsFromJson(GSON, jp, response.getMessageBody());
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to create sendAs email %s for user %s", sendAsEmail, userId), e);
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }
    }

    public static SendAs updateSendAs(RestClient rest, SendAs sendAs) throws IdMUnitException {
        final Set<Integer> returnCodes = HTTP_CODES_RETURN_ALWAYS;

        checkNotNull("rest", rest);
        checkNotNull("sendAs", sendAs);

        sendAs.normalize();
        String userId = sendAs.getUserId();
        String sendAsEmail = sendAs.getSendAsEmail();
        if (isBlank(userId)) {
            throw new IdMUnitException("Error: userId is empty and should be populated");
        }
        if (isBlank(sendAsEmail)) {
            throw new IdMUnitException("Error: sendAsEmail is empty and should be populated");
        }

        JsonParser jp = new JsonParser();
        String body = GSON.toJson(jp.parse(GSON.toJson(sendAs)));
        String path = String.format(Path.PATH_UPDATE, userId, sendAsEmail);
        HttpRequest httpRequest = HttpRequest.Factory.newPutRequest(path, body);

        try {
            RestClient.Response response = executeWithRetry(rest, httpRequest, returnCodes);

            if (response.getStatusCode() >= HTTP_MULT_CHOICE) {
                throw new ApiInternalException(String.format("Unable to update sendAs email %s for user %s. Status Code: %d. Message: %s", sendAsEmail, userId, response.getStatusCode(), response.getMessageBody()));
            } else {
                return newSendAsFromJson(GSON, jp, response.getMessageBody());
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to update sendAs email %s for user %s", sendAsEmail, userId), e);
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }
    }

    public static boolean deleteSendAs(RestClient rest, String userId, String sendAsEmail) throws IdMUnitException {
        final Set<Integer> returnCodes = new ImmutableSet.Builder<Integer>()
            .addAll(HTTP_CODES_RETURN_ALWAYS)
            .addAll(HTTP_CODES_RETURN_ON_DELETE_SUCCESS)
            .build();

        checkNotNull("rest", rest);

        if (isBlank(userId)) {
            throw new IdMUnitException("Error: userId is empty and should be populated");
        }
        if (isBlank(sendAsEmail)) {
            throw new IdMUnitException("Error: sendAsEmail is empty and should be populated");
        }

        String path = String.format(Path.PATH_DELETE, userId, sendAsEmail);
        HttpRequest request = HttpRequest.Factory.newDeleteRequest(path, null);
        boolean deleted;

        try {
            RestClient.Response response = executeWithRetry(rest, request, returnCodes);

            int code = response.getStatusCode();
            deleted = code < HTTP_MULT_CHOICE;
            if (!deleted && !HTTP_CODES_RETURN_ON_DELETE_SUCCESS.contains(code)) {
                String msg = getMessage(response);
                throw new ApiInternalException(String.format("Unable to delete sendAs email %s for user %s. Status Code: %d. Message: %s", sendAsEmail, userId, code, msg));
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to delete sendAs email %s for user %s", sendAsEmail, userId));
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }

        return deleted;
    }

    public static boolean deleteAllSendAs(RestClient rest, String userId) throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotNull("userId", userId);

        List<SendAs> sendAses = listSendAs(rest, userId);

        boolean someExisted = false;
        boolean allDeleted = true;
        for (SendAs sendAs : sendAses) {
            if (!((sendAs == null) || (sendAs.getIsPrimary() == Boolean.TRUE) || isBlank(sendAs.getSendAsEmail()))) {
                someExisted = true;
                allDeleted = allDeleted && deleteSendAs(rest, userId, sendAs.getSendAsEmail());
            }
        }

        return someExisted && allDeleted;
    }

    public static boolean deleteSendAs(RestClient rest, SendAs sendAs) throws IdMUnitException {
        checkNotNull("sendAs", sendAs);

        sendAs.normalize();
        return deleteSendAs(rest, sendAs.getUserId(), sendAs.getSendAsEmail());
    }

    public static List<SendAs> listSendAs(RestClient rest, String userId) throws IdMUnitException {
        final Set<Integer> returnCodes = new ImmutableSet.Builder<Integer>()
            .addAll(HTTP_CODES_RETURN_ALWAYS)
            .addAll(HTTP_CODES_RETURN_ON_NOT_FOUND)
            .build(); //TODO: move after checks

        checkNotNull("rest", rest);
        checkNotBlank("userId", userId);

        SendAsArray sendAsList = null;
        String path = String.format(Path.PATH_LIST, userId);
        HttpRequest request = HttpRequest.Factory.newGetRequest(path);

        try {
            RestClient.Response response = executeWithRetry(rest, request, returnCodes);

            int code = response.getStatusCode();
            if (code < HTTP_MULT_CHOICE) {
                sendAsList = newListSendAsFromJson(GSON, new JsonParser(), response.getMessageBody());
            } else if (!HTTP_CODES_RETURN_ON_NOT_FOUND.contains(code)) {
                String msg = getMessage(response);
                throw new ApiInternalException(String.format("Unable to list sendAs emails for user %s. Status Code: %d. Message: %s", userId, code, msg));
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to list sendAs emails for user %s", userId), e);
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }

        return mapNullToEmpty((sendAsList == null) ? null : normalizeSendAs(sendAsList.sendAs));
    }

    public static List<String> listSendAsEmailOnly(RestClient rest, String userId) throws IdMUnitException {
        List<SendAs> sendAsObjects = mapNullToEmpty(listSendAs(rest, userId));
        List<String> sendAsEmails = new ArrayList<String>(sendAsObjects.size());
        String sendAsEmail;
        for (SendAs s : sendAsObjects) {
            sendAsEmail = s.getSendAsEmail();
            if (!isBlank(sendAsEmail)) {
                sendAsEmails.add(sendAsEmail);
            }
        }

        return sendAsEmails;
    }

    public static SendAs getSendAs(String serviceEmail, PrivateKey key, String superUserEmail, SendAs sendAs) throws IdMUnitException {
        checkNotNull("sendAs", sendAs);

        RestClient rest = RestUtil.newRestClient(serviceEmail, key, superUserEmail, JavaUtil.join(SCOPES, ","));

        sendAs.normalize();
        return getSendAs(rest, sendAs.getUserId(), sendAs.getSendAsEmail());
    }

    public static SendAs getSendAs(RestClient rest, SendAs sendAs) throws IdMUnitException {
        checkNotNull("sendAs", sendAs);

        sendAs.normalize();
        return getSendAs(rest, sendAs.getUserId(), sendAs.getSendAsEmail());
    }

    public static String getSendAsEmailOnly(String serviceEmail, PrivateKey key, String superUserEmail, SendAs sendAs) throws IdMUnitException {
        checkNotNull("sendAs", sendAs);

        sendAs.normalize();
        RestClient rest = RestUtil.newRestClient(serviceEmail, key, superUserEmail, JavaUtil.join(SCOPES, ","));

        return getSendAsEmailOnly(rest, sendAs.getUserId(), sendAs.getSendAsEmail());
    }

    public static String getSendAsEmailOnly(RestClient rest, SendAs sendAs) throws IdMUnitException {
        checkNotNull("sendAs", sendAs);

        sendAs.normalize();
        return getSendAsEmailOnly(rest, sendAs.getUserId(), sendAs.getSendAsEmail());
    }

    public static boolean hasSendAs(String serviceEmail, PrivateKey key, String superUserEmail, SendAs sendAs) throws IdMUnitException {
        return !"".equals(getSendAsEmailOnly(serviceEmail, key, superUserEmail, sendAs));
    }

    public static boolean hasSendAs(RestClient rest, SendAs sendAs) throws IdMUnitException {
        return !"".equals(getSendAsEmailOnly(rest, sendAs));
    }

    public static boolean hasSendAs(RestClient rest, String userId, String sendAsEmail) throws IdMUnitException {
        return !"".equals(getSendAsEmailOnly(rest, userId, sendAsEmail));
    }

    public static boolean hasSendAses(RestClient rest, String userId, List<String> sendAsEmails) throws IdMUnitException {
        checkNotNullOrEmpty("sendAsEmails", sendAsEmails);
        return listSendAsEmailOnly(rest, userId).containsAll(sendAsEmails);
    }

    public static SendAs getSendAs(RestClient rest, String userId, String sendAsEmail) throws IdMUnitException {
        final Set<Integer> returnCodes = new ImmutableSet.Builder<Integer>()
            .addAll(HTTP_CODES_RETURN_ALWAYS)
            .addAll(HTTP_CODES_RETURN_ON_NOT_FOUND)
            .build();

        checkNotNull("rest", rest);
        checkNotBlank("userId", userId);
        checkNotBlank("sendAsEmail", sendAsEmail);

        SendAs sendAs = null;
        String path = String.format(Path.PATH_GET, userId, sendAsEmail);
        HttpRequest request = HttpRequest.Factory.newGetRequest(path);

        try {
            RestClient.Response response = executeWithRetry(rest, request, returnCodes);

            int code = response.getStatusCode();
            if (code < HTTP_MULT_CHOICE) {
                sendAs = newSendAsFromJson(GSON, new JsonParser(), response.getMessageBody());
            } else if (!HTTP_CODES_RETURN_ON_NOT_FOUND.contains(code)) {
                String msg = getMessage(response);
                throw new ApiInternalException(String.format("Unable to get sendAs email %s for user %s. Status Code: %d. Message: %s", sendAsEmail, userId, code, msg));
            }
        } catch (IdMUnitException e) {
            throw new IdMUnitException(String.format("Unable to get sendAs email %s for user %s", sendAsEmail, userId), e);
        } catch (ApiInternalException e) {
            throw new IdMUnitException(e.getMessage());
        }

        return sendAs;
    }

    public static String getSendAsEmailOnly(RestClient rest, String userId, String sendAsEmail) throws IdMUnitException {
        SendAs sendAs = getSendAs(rest, userId, sendAsEmail);
        if (sendAs != null) {
            return mapBlank(sendAs.getSendAsEmail(), "");
        } else {
            return "";
        }
    }

    public static SendAs newSendAsFromJson(Gson gson, JsonParser parser, String json) {
        SendAs sendAs = (SendAs)newObjectFromJson(gson, parser, json, SendAs.class);
        sendAs.normalize();
        return sendAs;
    }

    public static SendAsArray newListSendAsFromJson(Gson gson, JsonParser parser, String json) {
        return (SendAsArray)newObjectFromJson(gson, parser, json, SendAsArray.class);
    }

    public static final class Path {

        public static final String PATH_ROOT = "/gmail/v1/users";

        //use String.format() to replace path variables
        public static final String PATH_DELETE = PATH_ROOT + "/%s/settings/sendAs/%s"; //userId, sendAsEmail
        public static final String PATH_GET = PATH_ROOT + "/%s/settings/sendAs/%s"; //userId, sendAsEmail
        public static final String PATH_LIST = PATH_ROOT + "/%s/settings/sendAs"; //userId
        public static final String PATH_CREATE = PATH_ROOT + "/%s/settings/sendAs"; //userId
        public static final String PATH_UPDATE = PATH_ROOT + "/%s/settings/sendAs/%s"; //userId, sendAsEmail

        //unused
        //public static final String PATH_PATCH = PATH_ROOT + "%s/settings/sendAs/%s"; //userId, sendAsEmail
    }

}
