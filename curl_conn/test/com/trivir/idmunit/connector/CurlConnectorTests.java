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

import junit.framework.TestCase;
import org.idmunit.IdMUnitException;

import java.util.*;

public class CurlConnectorTests extends TestCase {
    private static final String CONFIG_TRUST_ALL_CERTS = "trust-all-certs";

    private static final Map<String, String> CONFIG;
    static {
        CONFIG = new HashMap<String, String>();
        CONFIG.put(CONFIG_TRUST_ALL_CERTS, "true");
    }

    private CurlConnector connector;


    public void setUp() throws Exception {
        connector = new CurlConnector();
        connector.setup(CONFIG);
    }

    public void tearDown() throws Exception {
        connector.tearDown();
    }

    public void testAction() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("url", singleValue("http://localhost:8080/openidm/managed/user/8f1307e8-7334-46ba-9a09-ea720e2b91e0"));
        attrs.put("method", singleValue("PUT"));
        attrs.put("headers", multiValue("X-OpenIDM-Username=openidm-admin", "X-OpenIDM-Password=openidm-admin", "content-type=application/json", "If-Match=*"));
        attrs.put("body", singleValue("{\"userName\": \"test-connector\"," +
                "\"sn\": \"test-connector\"," +
                "\"mail\": \"test-connector@example.com\"," +
                "\"givenName\": \"test-connector\"}"));

        connector.execute("Action", Collections.unmodifiableMap(attrs));
    }

    public void testActionServerError() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("url", singleValue("http://localhost:8080/openidm"));
        attrs.put("method", singleValue("POST"));

        try {
            connector.execute("Action", Collections.unmodifiableMap(attrs));
        } catch (IdMUnitException e) {
            assertEquals(e.getMessage(), "opAction was unsuccessful. Response [status: '500' reason: 'Server Error' body: '']");
        }
    }

    public void testActionBadAction() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("url", singleValue("http://localhost:8080/openidm"));
        attrs.put("method", singleValue("GET"));

        try {
            connector.execute("Action", Collections.unmodifiableMap(attrs));
        } catch (IdMUnitException e) {
            assertEquals(e.getMessage(), "method [get] is an unsupported HTTP method for 'opAction'");
        }
    }

    public void testActionMissingAttrs() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();

        try {
            connector.execute("Action", Collections.unmodifiableMap(attrs));
        } catch (IdMUnitException e) {
            assertEquals(e.getMessage(), "The following required attributes were not found: url method");
        }
    }

    public void testActionUnknownAction() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("url", singleValue("http://localhost:8080/openidm"));
        attrs.put("method", singleValue("GET"));

        try {
            connector.execute("Action", Collections.unmodifiableMap(attrs));
        } catch (IdMUnitException e) {
            assertEquals(e.getMessage(), "method [get] is an unsupported HTTP method for 'opAction'");
        }
    }

    public void testValidate() throws Exception {
/*        {
            "_id": "8f1307e8-7334-46ba-9a09-ea720e2b91e0",
                "_rev": "91",      <----- This will change
                "userName": "test-conn",
                "givenName": "test-conn",
                "sn": "test-conn",
                "mail": "test-conn@example.com",
                "effectiveRoles": [],
            "effectiveAssignments": []
        }*/
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("url", singleValue("http://localhost:8080/openidm/managed/user/8f1307e8-7334-46ba-9a09-ea720e2b91e0"));
        attrs.put("method", singleValue("GET"));
        attrs.put("headers", multiValue("X-OpenIDM-Username=openidm-admin", "X-OpenIDM-Password=openidm-admin"));
        attrs.put("responseBody", singleValue("{" +
                "\"_id\": \"8f1307e8-7334-46ba-9a09-ea720e2b91e0\"," +
                "\"_rev\": \"99\"," +
                "\"userName\": \"test-connector\"," +
                "\"sn\": \"test-connector\"," +
                "\"mail\": \"test-connector@example.com\"," +
                "\"givenName\": \"test-connector\"," +
                "\"effectiveRoles\": []," +
                "\"effectiveAssignments\": []" +
                "}"));

        connector.execute("Validate", Collections.unmodifiableMap(attrs));
    }

    public void testValidateAttrMismatch() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("url", singleValue("http://localhost:8080/openidm/managed/user/8f1307e8-7334-46ba-9a09-ea720e2b91e0"));
        attrs.put("method", singleValue("GET"));
        attrs.put("headers", multiValue("X-OpenIDM-Username=openidm-admin", "X-OpenIDM-Password=openidm-admin"));
        attrs.put("responseBody", singleValue("{" +
                "\"_id\": \"8f1307e8-7334-46ba-9a09-ea720e2b91e0\"," +
                "\"_rev\": \"92\"," +
                "\"userName\": \"test-connector\"," +
                "\"sn\": \"test-connector\"," +
                "\"mail\": \"test-connector@example.com\"," +
                "\"givenName\": \"test-connector2\"," +
                "\"effectiveRoles\": []," +
                "\"effectiveAssignments\": []" +
                "}"));

        try {
            connector.execute("Validate", Collections.unmodifiableMap(attrs));
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().startsWith("Validation failed: "));
        }
    }

    public void testValidateStatusCodeMismatch() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("url", singleValue("http://localhost:8080/openidm/managed/user/8f1307e8-7334-46ba-9a09-ea720e2b91e0"));
        attrs.put("method", singleValue("GET"));
        attrs.put("headers", multiValue("X-OpenIDM-Username=openidm-admin", "X-OpenIDM-Password=openidm-admin"));
        attrs.put("responseBody", singleValue("{" +
                "\"_id\": \"8f1307e8-7334-46ba-9a09-ea720e2b91e0\"," +
                "\"_rev\": \"93\"," +
                "\"userName\": \"test-connector\"," +
                "\"sn\": \"test-connector\"," +
                "\"mail\": \"test-connector@example.com\"," +
                "\"givenName\": \"test-connector\"," +
                "\"effectiveRoles\": []," +
                "\"effectiveAssignments\": []" +
                "}"));
        attrs.put("statusCode", singleValue("500"));

        try {
            connector.execute("Validate", Collections.unmodifiableMap(attrs));
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().startsWith("Validation failed: "));
        }
    }

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    private static Collection<String> multiValue(String... values) {
        return Arrays.asList(values);
    }
}
