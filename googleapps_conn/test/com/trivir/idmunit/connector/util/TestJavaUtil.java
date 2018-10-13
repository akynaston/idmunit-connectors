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

package com.trivir.idmunit.connector.util;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TestJavaUtil {

    @Test
    public void testIsBlank() throws Exception {
        assertTrue(JavaUtil.isBlank(null));
        assertTrue(JavaUtil.isBlank(""));
        assertTrue(JavaUtil.isBlank(" "));
        assertTrue(JavaUtil.isBlank("\t"));
        assertTrue(JavaUtil.isBlank("\n"));
        assertTrue(JavaUtil.isBlank("\r"));
        assertFalse(JavaUtil.isBlank("a"));
        //TODO: add unicode tests

        assertFalse(JavaUtil.isBlank(" ", false));
        assertTrue(JavaUtil.isBlank(" ", true));
    }

    @Test
    public void testMapNull() throws Exception {
        assertEquals("", JavaUtil.mapNull(null, ""));
        assertEquals("a", JavaUtil.mapNull(null, "a"));
        assertNull(JavaUtil.mapNull(null, null));
    }

    @Test
    public void testNullOrEmpty() throws Exception {

        //List
        assertTrue(JavaUtil.isNullOrEmpty(new LinkedList()));

        List list = new LinkedList();
        list.add("a");
        assertFalse(JavaUtil.isNullOrEmpty(list));

        //Map
        assertTrue(JavaUtil.isNullOrEmpty(new HashMap()));

        Map map = new HashMap();
        map.put("a", "");
        assertFalse(JavaUtil.isNullOrEmpty(map));

        //Set
        assertTrue(JavaUtil.isNullOrEmpty(new HashSet()));

        Set set = new HashSet();
        set.add("a");
        assertFalse(JavaUtil.isNullOrEmpty(set));
    }

}
