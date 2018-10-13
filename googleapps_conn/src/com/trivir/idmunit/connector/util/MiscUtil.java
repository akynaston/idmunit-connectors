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

import java.util.Collection;
import java.util.Iterator;

import static com.trivir.idmunit.connector.util.JavaUtil.*;

public class MiscUtil {


    public static boolean containsOnlyBlanks(Collection<String> c, boolean whitespaceIsBlank) {
        checkNotNull("c", c);

        if (c.isEmpty()) {
            return true;
        }

        for (String s : c) {
            if (!isBlank(s, whitespaceIsBlank)) {
                return false;
            }
        }

        return true;
    }

    public static Collection<String> removeBlanks(Collection<String> c, boolean whitespaceIsBlank) {

        if (isNullOrEmpty(c)) {
            return c;
        }

        Iterator<String> i = c.iterator();
        while (i.hasNext()) {
            if (isBlank(i.next(), whitespaceIsBlank)) {
                i.remove();
            }
        }

        return c;
    }

    public static String getTitleFromHtmlPage(String page) {
        String title = "";

        if (isBlank(page)) {
            return title;
        }

        final String titleStartTag = "<title>";
        final String titleEndTag = "</title>";

        //get title from login page
        int start = page.indexOf(titleStartTag);
        if (start < 0) {
            return title;
        }

        int end = page.indexOf(titleEndTag);
        if (end < 0) {
            return title;
        }

        if (end <= start) {
            return title;
        }

        String substring = page.substring(start + titleStartTag.length(), end);
        if (isBlank(substring)) {
            return title;
        }

        title = substring;

        return title;
    }
}
