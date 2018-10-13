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

import java.util.*;

public class JavaUtil {

    public static boolean isBlank(String s) {
        return isBlank(s, true);
    }

    public static boolean isBlank(String s, boolean whitespaceIsBlank) {
        boolean isBlank = s == null;
        if (!isBlank) {
            if (whitespaceIsBlank) {
                s = s.trim();
            }
            isBlank = s.length() < 1;
        }
        return isBlank;
    }

    public static String join(Collection<String> strings) {
        return join(strings, null);
    }

    public static String join(Collection<String> strings, String delim) {
        if (strings == null) {
            return "";
        }

        if (delim == null) {
            delim = ",";
        }

        boolean first = true;
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {

            if (first) {
                first = false;
            } else {
                builder.append(delim);
            }
            builder.append(s);
        }

        return builder.toString();
    }


    public static <T> T getFirstValue(Collection<T> c) {
        if (isNullOrEmpty(c)) {
            return null;
        }

        return c.iterator().next();
    }

    public static <T> T getFirstValue(List<T> l) {
        if (isNullOrEmpty(l)) {
            return null;
        }

        return l.get(0);
    }

    public static String mapBlank(String toMap, String mapTo) {
        return (isBlank(toMap)) ? mapTo : toMap;
    }

    public static <T> T mapNull(T toMap, T mapTo) {
        return (toMap == null) ? mapTo : toMap;
    }

    public static <T> List<T> mapNullToEmpty(List<T> t1) {
        return (t1 == null) ? Collections.<T>emptyList() : t1;
    }

    public static <T> Collection<T> mapNullToEmpty(Collection<T> t1) {
        return (t1 == null) ? Collections.<T>emptyList() : t1;
    }

    public static <T> Set<T> mapNullToEmpty(Set<T> t1) {
        return (t1 == null) ? Collections.<T>emptySet() : t1;
    }


    public static <T> Map<T, T> mapNullToEmpty(Map<T, T> t1) {
        return (t1 == null) ? Collections.<T, T>emptyMap() : t1;
    }

    public static boolean isNullOrEmpty(Collection c) {
        return (c == null) || c.isEmpty();
    }

    public static boolean isNullOrEmpty(Map m) {
        return (m == null) || m.isEmpty();
    }

    public static boolean isNullOrEmpty(Set s) {
        return (s == null) || s.isEmpty();
    }

    public static <T> Collection<T> toMutableCollection(Collection<T> c) {
        Collection<T> n = c;

        if (c != null) {
            n = new ArrayList(c);
        }

        return n;
    }

    public static Object checkNotNull(String paramName, Object paramValue) {
        if (paramValue == null) {
            String message;
            if (isBlank(paramName)) {
                message = "Required param is null";
            } else {
                message = String.format("Required param '%s' is null", paramName);
            }
            throw new IllegalArgumentException(message);
        }

        return paramValue;
    }

    public static Collection checkNotNullOrEmpty(String paramName, Collection paramValue) {
        if (isNullOrEmpty(paramValue)) {
            String message;
            if (isBlank(paramName)) {
                message = "Required param is null";
            } else {
                message = String.format("Required param '%s' is null or empty", paramName);
            }
            throw new IllegalArgumentException(message);
        }

        return paramValue;
    }

    public static Map checkNotNullOrEmpty(String paramName, Map paramValue) {
        if (isNullOrEmpty(paramValue)) {
            String message;
            if (isBlank(paramName)) {
                message = "Required param is null";
            } else {
                message = String.format("Required param '%s' is null or empty", paramName);
            }
            throw new IllegalArgumentException(message);
        }

        return paramValue;
    }

    public static String checkNotBlank(String paramName, String paramValue) {
        if (isBlank(paramValue)) {
            String message;
            if (isBlank(paramName)) {
                message = "Required param is blank";
            } else {
                message = String.format("Required param '%s' is blank", paramName);
            }
            throw new IllegalArgumentException(message);
        }

        return paramValue;
    }
}
