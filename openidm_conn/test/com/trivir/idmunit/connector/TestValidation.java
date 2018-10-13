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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import junit.framework.TestCase;
import org.idmunit.IdMUnitException;

import java.util.*;

public class TestValidation extends TestCase {
    /*
     * prop1 = val1
     * {"prop1": "val1"}
     */
    public void testMapSingleString() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("prop1", Collections.singletonList("val1"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":\"val1\"}", json);
    }

    /*
     * prop1[] = val1|val2
     * {"prop1": ["val1", "val2"]}
     */
    public void testMapSingleArray() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("prop1[]", Arrays.asList("val1", "val2"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":[\"val1\",\"val2\"]}", json);
    }

    /*
     * prop1.subProp1 = val1
     * {"prop1":{"subProp1":"val1"}}
     */
    public void testMapSubpropString() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("prop1.subProp1", Collections.singletonList("val1"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":{\"subProp1\":\"val1\"}}", json);
    }

    /*
     * prop1[].subProp1 = val1|val2
     * {"prop1": [{"subProp1": "val1"}, {"subProp1": "val2"}]}
     */
    public void testMapSubpropArray() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("prop1[].subProp1", Arrays.asList("val1", "val2"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":[{\"subProp1\":\"val1\"},{\"subProp1\":\"val2\"}]}", json);
    }

    /*
     * prop1[].subProp1.subSubProp1 = val1|val2
     *
     * {"prop1": [{"subProp1": {"subSubProp1": "val1"}, {"subProp1": {"subSubProp1": "val2"}}}]}
     */
    public void testMapNestedArrayOfObjects() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("prop1[].subProp1.subSubProp1", Arrays.asList("val1", "val2"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":[{\"subProp1\":{\"subSubProp1\":\"val1\"}},{\"subProp1\":{\"subSubProp1\":\"val2\"}}]}", json);
    }

    /*
     * prop1[].subProp1.subSubProp1 = val1|val2
     * prop1[].subProp2 = val3|val4
     *
     * {"prop1": [{"subProp1": {"subSubProp1": "val1"}, "subProp2": "val3"}, {"subProp1": {"subSubProp1": "val2"}, "subProp2": "val4"}]}
     */
    public void testMapNestedArrayWithObjectsAndString() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new LinkedHashMap<String, Collection<String>>();
        attrs.put("prop1[].subProp1.subSubProp1", Arrays.asList("val1", "val2"));
        attrs.put("prop1[].subProp2", Arrays.asList("val3", "val4"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":[{\"subProp1\":{\"subSubProp1\":\"val1\"},\"subProp2\":\"val3\"},{\"subProp1\":{\"subSubProp1\":\"val2\"},\"subProp2\":\"val4\"}]}", json);
    }

    public void testEmptyArray() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("prop1[]", Collections.singletonList("[EMPTY]"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":[]}", json);
        //{"prop1": []}
    }

    public void testEmptyString() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("prop1", Collections.singletonList("[EMPTY]"));

        JsonObject obj = OpenIdmConnector.mapToJsonObject(attrs);

        String json = new Gson().toJson(obj);
        assertEquals("{\"prop1\":\"\"}", json);
        //{"prop1": []}
    }

    /*
     * prop1[].subProp1 = val1|val2
     * prop2 = val5
     *
     * {"prop1": [{"subProp1": "val1"}, {"subProp1": "val2"}], "prop2": "val5"}
     */

    /*
     * prop1[].subProp1.subSubProp1 = val1|val2
     * prop2 = val5
     * prop1[].subProp2 = val3|val4
     *
     * {"prop1": [{"subProp1": {"subSubProp1": "val1"}, "subProp2": "val3"}, {"subProp1": {"subSubProp1": "val2"}, "subProp2": "val4"}], "prop2": "val5"}
     */

    /*
{
    "prop1": "val1",
    "prop2": ["val2"],
    "prop3": ["val3.1", "val3.2"],
    "prop4": {
        "prop5": "val5",
        "prop6": {
            "prop7": "val7",
            "prop8": ["val8.1", "val8.2"]
        }
    },
    "prop9" : [
        {
            "prop10": "val10"
        },
        {
            "prop11": "val11"
        }
    ]
}
     */
    public void testDiff1() {
        String expectedString = "{\n" +
                "    \"prop1\": \"val1\",\n" +
                "    \"prop2\": [\"val2\"],\n" +
                "    \"prop3\": [\"val3.1\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"val5\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val7\",\n" +
                "            \"prop8\": [\"val8.1\", \"val8.2\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val10\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val11\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        String actualString = "{\n" +
                "    \"prop1\": \"val1\",\n" +
                "    \"prop2\": [\"val2\"],\n" +
                "    \"prop3\": [\"val3.1\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"val5\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val7\",\n" +
                "            \"prop8\": [\"val8.1\", \"val8.2\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val10\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val11\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        JsonElement expected = new JsonParser().parse(expectedString);
        JsonElement actual = new JsonParser().parse(actualString);

        List<String> differences = OpenIdmConnector.jsonMatches(expected, actual);
        assertEquals(0, differences.size());
    }

    public void testDiff2() {
        String expectedString = "{\n" +
                "    \"prop1\": \"val1\",\n" +
                "    \"prop2\": [\"val2\"],\n" +
                "    \"prop3\": [\"val3.1\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"val5\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val7\",\n" +
                "            \"prop8\": [\"val8.1\", \"val8.2\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val10\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val11\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        String actualString = "{\n" +
                "    \"prop1\": \"val0\",\n" +
                "    \"prop2\": [\"val0\"],\n" +
                "    \"prop3\": [\"val0\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"val0\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val0\",\n" +
                "            \"prop8\": [\"val8.1\", \"val0\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val0\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val0\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        JsonElement expected = new JsonParser().parse(expectedString);
        JsonElement actual = new JsonParser().parse(actualString);

        List<String> differences = OpenIdmConnector.jsonMatches(expected, actual);
        for (String d : differences) {
            System.out.println(d);
        }
    }

    public void testDiffEmptyJson() {
        String expectedString = "{\n" +
                "    \"prop1\": \"val1\",\n" +
                "    \"prop2\": [\"val2\"],\n" +
                "    \"prop3\": [\"val3.1\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val7\",\n" +
                "            \"prop8\": [\"val8.1\", \"val8.2\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val10\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val11\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        String actualString = "{\n" +
                "    \"prop1\": \"val1\",\n" +
                "    \"prop2\": [\"val2\"],\n" +
                "    \"prop3\": [\"val3.1\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val7\",\n" +
                "            \"prop8\": [\"val8.1\", \"val8.2\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val10\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val11\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        JsonElement expected = new JsonParser().parse(expectedString);
        JsonElement actual = new JsonParser().parse(actualString);

        List<String> differences = OpenIdmConnector.jsonMatches(expected, actual);
        for (String d : differences) {
            System.out.println(d);
        }
        assertEquals(0, differences.size());
    }

    public void testDiffNullJson() {
        String expectedString = "{\n" +
                "    \"prop1\": \"\",\n" +
                "    \"prop2\": \"\",\n" +
                "    \"prop3\": \"\",\n" +
                "    \"prop4\": [],\n" +
                "    \"prop5\": [],\n" +
                "    \"prop6\": []\n" +
                "}\n";
        String actualString = "{\n" +
                "    \"prop1\": \"\",\n" +
                "    \"prop2\": null,\n" +
                "    \"prop4\": [],\n" +
                "    \"prop5\": null\n" +
                "}\n";
        JsonElement expected = new JsonParser().parse(expectedString);
        JsonElement actual = new JsonParser().parse(actualString);

        List<String> differences = OpenIdmConnector.jsonMatches(expected, actual);
        for (String d : differences) {
            System.out.println(d);
        }
        assertEquals(0, differences.size());
    }

    public void testEmptyJsonProp() {
        String expectedString = "{\n" +
                "    \"prop1\": \"\",\n" +
                "    \"prop2\": [\"val2\"],\n" +
                "    \"prop3\": [\"val3.1\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"val5\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val7\",\n" +
                "            \"prop8\": [\"val8.1\", \"val8.2\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val10\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val11\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        String actualString = "{\n" +
                "    \"prop2\": [\"val2\"],\n" +
                "    \"prop3\": [\"val3.1\", \"val3.2\"],\n" +
                "    \"prop4\": {\n" +
                "        \"prop5\": \"val5\",\n" +
                "        \"prop6\": {\n" +
                "            \"prop7\": \"val7\",\n" +
                "            \"prop8\": [\"val8.1\", \"val8.2\"]\n" +
                "        }\n" +
                "    },\n" +
                "    \"prop9\" : [\n" +
                "        {\n" +
                "            \"prop10\": \"val10\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"prop11\": \"val11\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        JsonElement expected = new JsonParser().parse(expectedString);
        JsonElement actual = new JsonParser().parse(actualString);

        List<String> differences = OpenIdmConnector.jsonMatches(expected, actual);
        for (String d : differences) {
            System.out.println(d);
        }
        assertEquals(0, differences.size());

    }
}
