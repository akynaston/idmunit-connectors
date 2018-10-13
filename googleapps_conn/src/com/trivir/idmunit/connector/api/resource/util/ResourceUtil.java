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

package com.trivir.idmunit.connector.api.resource.util;

public final class ResourceUtil {

    public static boolean areEqual(String s1, String s2, boolean ignoreCase, boolean trim) {
        //are both null or the same String reference?
        if (s1 == s2) {
            return true;
        }

        if ((s1 == null) || (s2 == null)) {
            return false;
        }

        if (trim) {
            s1 = s1.trim();
            s2 = s2.trim();
        }

        if (ignoreCase) {
            return s1.equalsIgnoreCase(s1);
        } else {
            return s1.equals(s2);
        }
    }
}
