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

package com.trivir.idmunit.connector.api.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trivir.idmunit.connector.rest.RestClient;
import org.idmunit.IdMUnitException;

import java.security.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.trivir.idmunit.connector.GoogleAppsConnector.ADMIN_EMAIL;
import static com.trivir.idmunit.connector.api.UserApi.Path.PATH_USER;
import static com.trivir.idmunit.connector.rest.RestClient.parseErrorDescription;
import static com.trivir.idmunit.connector.util.JavaUtil.checkNotNull;
import static com.trivir.idmunit.connector.util.JavaUtil.isBlank;
import static java.net.HttpURLConnection.*;

//TODO: integrate excute and retry code into RestClient
//TODO: update RestClient to take a function signature for initialization and results handling; the results handler should
// replace the HTTP_CODES construct used here
public class RestUtil {

    public static final Set<Integer> HTTP_CODES_RETURN_ON_NOT_FOUND;
    public static final Set<Integer> HTTP_CODES_RETURN_ON_DELETE_SUCCESS;
    public static final Set<Integer> HTTP_CODES_RETURN_ALWAYS;

    private static final String[] SCOPES_DEFAULT = new String[]{
        "https://www.googleapis.com/auth/admin.directory.user",
        "https://www.googleapis.com/auth/admin.directory.group",
        "https://www.googleapis.com/auth/admin.directory.group.member",
        "https://www.googleapis.com/auth/admin.directory.orgunit",
        "https://www.googleapis.com/auth/admin.directory.user",
        "https://www.googleapis.com/auth/admin.directory.user.alias",
        "https://www.googleapis.com/auth/admin.directory.user.security",
        "https://www.googleapis.com/auth/admin.directory.userschema",
        "https://www.googleapis.com/auth/apps.licensing",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile",
    };

    private static final String REST_SERVICE_URL = "https://www.googleapis.com";

    private static final int RETRY_MAX = 10;
    private static final int WAIT_BETWEEN_MAX = 10;

    static {
        Set<Integer> codesNotFound = new HashSet<Integer>();
        codesNotFound.add(HTTP_NOT_FOUND);
        codesNotFound.add(HTTP_NO_CONTENT);

        Set<Integer> codesDeleteSuccess = new HashSet<Integer>();
        codesDeleteSuccess.add(HTTP_NOT_FOUND);
        codesDeleteSuccess.add(HTTP_BAD_REQUEST);

        Set<Integer> codesReturn = new HashSet<Integer>();
        codesReturn.add(HTTP_FORBIDDEN);

        HTTP_CODES_RETURN_ON_NOT_FOUND = Collections.unmodifiableSet(codesNotFound);
        HTTP_CODES_RETURN_ON_DELETE_SUCCESS = Collections.unmodifiableSet(codesDeleteSuccess);
        HTTP_CODES_RETURN_ALWAYS = Collections.unmodifiableSet(codesReturn);
    }

    public static RestClient newRestClient(String serviceEmail, PrivateKey key, String superUserEmail, String scopeStr) throws IdMUnitException {
        String[] scopes;
        if (isBlank(scopeStr)) {
            scopes = SCOPES_DEFAULT;
        } else {
            scopeStr = scopeStr.replaceAll(" ", "");
            scopes = scopeStr.split(",");
        }

        try {
            RestClient restClient = RestClient.init(REST_SERVICE_URL, serviceEmail, key, superUserEmail, scopes);

            if (ADMIN_EMAIL.equals(superUserEmail)) {
                checkSdkEnabled(restClient);
            }
            return restClient;
        } catch (InvalidKeyException e) {
            throw new IdMUnitException("Error retrieving authentication token", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IdMUnitException("Error retrieving authentication token", e);
        } catch (SignatureException e) {
            throw new IdMUnitException("Error retrieving authentication token", e);
        } catch (IdMUnitException e) {
            throw new IdMUnitException("Error retrieving authentication token", e);
        }
    }

    private static void checkSdkEnabled(RestClient restClient) throws IdMUnitException {
        // Make a test call to check to make sure the Admin SDK API is enabled
        RestClient.Response response = restClient.executeGet(String.format(PATH_USER, ADMIN_EMAIL));
        if (response.getStatusCode() != HTTP_OK) {
            if (response.getStatusCode() == HTTP_FORBIDDEN) {
                String errorDescription = parseErrorDescription(response.getMessageBody());
                if ("Access Not Configured. The API (Admin Directory API) is not enabled for your project. Please use the Google Developers Console to update your configuration.".equals(errorDescription)) {
                    throw new IdMUnitException(errorDescription);
                }
            }
            throw new IdMUnitException("Error making a test call with new connection: " + response.getReasonPhrase() + " (" + response.getStatusCode() + ") : " + response.getMessageBody());
        }
    }

    public static Object newObjectFromJson(Gson gson, JsonParser parser, String json, Class c) {
        JsonElement el = parser.parse(json);
        JsonObject jobject = el.getAsJsonObject();
        return gson.fromJson(jobject, c);
    }

    public static String getMessage(RestClient.Response response) {
        String msg = "";

        if (response == null) {
            return msg;
        }

        msg = response.getMessageBody();
        if (isBlank(msg)) {
            msg = response.getReasonPhrase();
        }
        if (isBlank(msg)) {
            msg = response.toString();
        }

        return msg;
    }

    public static RestClient.Response execute(
        RestClient rest,
        HttpRequest request,
        Collection<Integer> returnCodes)
        throws IdMUnitException {
        return executeWithRetry(0, 0, rest, request, returnCodes);
    }

    public static RestClient.Response executeWithRetry(
        RestClient rest,
        HttpRequest request,
        Collection<Integer> returnCodes)
        throws IdMUnitException {
        return executeWithRetry(3, 1, rest, request, returnCodes);
    }

    public static RestClient.Response executeWithRetry(
        int retryCount,
        int waitBetweenSeconds,
        RestClient rest,
        HttpRequest request,
        Collection<Integer> returnCodes)
        throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotNull("request", request);

        if ((retryCount < 0) || (retryCount > RETRY_MAX)) {
            throw new IllegalArgumentException("Param 'retryCount' must be between 0 and " + RETRY_MAX);
        }

        if ((waitBetweenSeconds < 0) || (waitBetweenSeconds > WAIT_BETWEEN_MAX)) {
            throw new IllegalArgumentException("Param 'waitBetweenSeconds' must be between 0 and " + WAIT_BETWEEN_MAX);
        }

        if (returnCodes == null) {
            returnCodes = Collections.emptySet();
        }

        RestClient.Response response = null;

        //TODO: update RestClient to support queryParams for all requests
        for (int r = 0; r <= retryCount; r++) {
            switch (request.getHttpVerb()) {
                case GET:
                    response = rest.executeGet(request.getPath(), request.getQueryParams());
                    break;
                case POST:
                    response = rest.executePost(request.getPath(), request.getBody());
                    break;
                case PUT:
                    response = rest.executePut(request.getPath(), request.getBody());
                    break;
/*                case PATCH:
                    response = rest.executePatch(request.getPath(), request.getBody());
                    break;*/
                case DELETE:
                    response = rest.executeDelete(request.getPath());
                    break;
                default:
                    throw new IllegalStateException(String.format("Broken Switch statement. Unhandled HttpVerb '%s'", request.getHttpVerb().toString()));
            }

            int code = response.getStatusCode();
            //if code is in the 200s or in successCodes
            if ((code < HTTP_MULT_CHOICE) || returnCodes.contains(code)) {
                break;
            } else {
                System.out.println(String.format("Request:\n%s", request.toString()));
                System.out.println(String.format("Response:\n%s", response.toString()));
                if (r < retryCount) {
                    System.out.println(String.format("Retry attempt %d...", r + 1));
                    waitTimeSeconds(waitBetweenSeconds);
                }
            }
        }

        return response;
    }

    public static void waitTimeSeconds(int multiplier) {
        if (multiplier < 1) {
            return;
        }
        try {
            //1000 milliseconds is one second
            System.out.println(String.format("Waiting for %d seconds...", multiplier));
            Thread.sleep(1000 * multiplier);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    //TODO: remove when UserApi and GroupApi are flushed-out
    public static RestClient.Response retryGet(RestClient rest, String path) throws IdMUnitException {
        waitTimeSeconds(3);
        RestClient.Response response = rest.executeGet(path);
        return response;
    }
}
