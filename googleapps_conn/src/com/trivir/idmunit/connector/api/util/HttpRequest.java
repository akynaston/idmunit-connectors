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

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;

//TODO: move to where RestClient resides
//TODO: add support for headers

@Getter
@Setter
public final class HttpRequest {

    private HttpVerb httpVerb;
    private String path;
    private Map<String, String> queryParams;
    private String body;

    public HttpRequest() {
        this.queryParams = Collections.emptyMap();
    }

    public void setQueryParams(Map<String, String> params) {
        if (params != null) {
            this.queryParams = params;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("httpVerb: ").append(httpVerb).append("\n");
        sb.append("path: ").append(path).append("\n");
        sb.append("body: ").append(body).append("\n");
        sb.append("query params: ").append(queryParams.toString());
        return sb.toString();
    }

    public enum HttpVerb {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE");
        //PATCH("PATCH");

        private final String verb;

        HttpVerb(String s) {
            verb = s;
        }

        public String toString() {
            return this.verb;
        }
    }

    public static final class Factory {

        public static HttpRequest newRequest(
            HttpVerb verb,
            String path,
            Map<String, String> queryParams,
            String body) {

            HttpRequest request = new HttpRequest();

            request.setHttpVerb(verb);
            request.setPath(path);
            request.setQueryParams(queryParams);
            request.setBody(body);

            return request;
        }

        public static HttpRequest newGetRequest(
            String path,
            Map<String, String> queryParams) {

            HttpRequest request = new HttpRequest();

            request.setHttpVerb(HttpVerb.GET);
            request.setPath(path);
            request.setQueryParams(queryParams);

            return request;
        }

        public static HttpRequest newGetRequest(String path) {
            return newGetRequest(path, null);
        }

        public static HttpRequest newPostRequest(
            String path,
            String body) {

            return newPostRequest(path, null, body);
        }

        public static HttpRequest newPostRequest(
            String path,
            Map<String, String> queryParams,
            String body) {

            HttpRequest request = new HttpRequest();

            request.setHttpVerb(HttpVerb.POST);
            request.setPath(path);
            request.setQueryParams(queryParams);
            request.setBody(body);

            return request;
        }

/*        public static HttpRequest newPatchRequest(
            String path,
            String body) {

            return newPatchRequest(path, null, body);
        }

          public static HttpRequest newPatchRequest(
            String path,
            Map<String, String> queryParams,
            String body) {

            HttpRequest request = new HttpRequest();

            request.setHttpVerb(HttpVerb.PATCH);
            request.setPath(path);
            request.setQueryParams(queryParams);
            request.setBody(body);

            return request;
        }
*/

        public static HttpRequest newPutRequest(
            String path,
            Map<String, String> queryParams,
            String body) {

            HttpRequest request = new HttpRequest();

            request.setHttpVerb(HttpVerb.PUT);
            request.setPath(path);
            request.setQueryParams(queryParams);
            request.setBody(body);

            return request;
        }

        public static HttpRequest newPutRequest(
            String path,
            String body) {
            return newPutRequest(path, null, body);
        }

        public static HttpRequest newDeleteRequest(
            String path,
            Map<String, String> queryParams) {

            HttpRequest request = new HttpRequest();

            request.setHttpVerb(HttpVerb.DELETE);
            request.setPath(path);
            request.setQueryParams(queryParams);

            return request;
        }

        public static HttpRequest newDeleteRequest(String path) {
            return newDeleteRequest(path, null);
        }

    }
}

