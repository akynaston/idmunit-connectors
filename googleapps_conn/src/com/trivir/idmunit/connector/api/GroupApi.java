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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trivir.idmunit.connector.api.resource.Group;
import com.trivir.idmunit.connector.api.util.RestUtil;
import com.trivir.idmunit.connector.rest.RestClient;
import com.trivir.idmunit.connector.util.JsonUtil;
import org.idmunit.IdMUnitException;

import static com.trivir.idmunit.connector.util.JavaUtil.checkNotBlank;
import static com.trivir.idmunit.connector.util.JavaUtil.checkNotNull;
import static java.net.HttpURLConnection.*;

//TODO: Remove traces of IdmUnit (e.g, IDMUnitExceptions)
public final class GroupApi {

    //Gson is thread-safe
    private static final Gson GSON = new Gson();

    public static void deleteGroup(RestClient rest, String groupEmail) throws IdMUnitException {
        checkNotNull("rest", rest);

        //final String groupEmail = ConnectorUtil.getSingleValue(data, GroupApi.Schema.ATTR_GROUP_EMAIL);

        if (groupEmail == null || "".equals(groupEmail)) {
            throw new IdMUnitException("groupEmail must be specified");
        }

        RestClient.Response response = rest.executeDelete(GroupApi.Path.PATH_ROOT + "/" + groupEmail);
        if (response.getStatusCode() != HTTP_OK && response.getStatusCode() != HTTP_NO_CONTENT) {
            if (response.getMessageBody() == null || response.getMessageBody().length() == 0) {
                throw new IdMUnitException(String.format("Error %d deleting group '%s': '%s'",
                    response.getStatusCode(), groupEmail, response.getReasonPhrase()));
            } else {
                String msg = JsonUtil.parseError(response.getMessageBody());
                if (!"Resource Not Found: groupKey".equals(msg)) {
                    throw new IdMUnitException(String.format("Error deleting group '%s': '%s'", groupEmail, msg));
                }
            }
        }

    }

    public static Group getGroup(RestClient rest, String groupEmail) throws IdMUnitException {
        checkNotNull("rest", rest);
        checkNotBlank("groupEmail", groupEmail);

        Group group;
        String path = Path.PATH_ROOT + "/" + groupEmail;
        RestClient.Response response = rest.executeGet(path);
        if (response.getStatusCode() < HTTP_BAD_REQUEST) {
            JsonParser jp = new JsonParser();
            JsonElement el = jp.parse(response.getMessageBody());
            JsonObject jobject = el.getAsJsonObject();
            group = GSON.fromJson(jobject, Group.class);
        } else {

            // retry
            response = RestUtil.retryGet(rest, path);
            if (response.getStatusCode() == HTTP_OK) {
                JsonParser jp = new JsonParser();
                JsonElement el = jp.parse(response.getMessageBody());
                JsonObject jobject = el.getAsJsonObject();
                group = GSON.fromJson(jobject, Group.class);
            } else {
                //group = null;
                String msg = JsonUtil.parseError(response.getMessageBody());

                if ("Resource Not Found: groupKey".equals(msg)) {
                    throw new IdMUnitException(String.format("Error group '%s' does not exist", groupEmail));
                } else {
                    throw new IdMUnitException(String.format("Error retrieving group '%s'", groupEmail));
                }
            }
        }

        return group;
    }

    public static final class Path {

        public static final String PATH_ROOT = "/admin/directory/v1/groups";
    }

}
