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

/**************************************************************************
 *  IdMUnit - Automated Testing Framework for Identity Management Solutions
 *
 * Purpose of this file: This is a test runner that provides an interface for
 * the selection and execution of a spreadsheet and the sheets therein.
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 *
 *******************************************************************************/
package org.idmunit;

import junit.framework.Test;
import junit.framework.TestCase;
import org.idmunit.parser.ExcelParser;

public class SCPSampleTests extends TestCase {
    //Used for operational purposes - ensure that the classname used is the same as the name of this class
    public static Test suite() {
        return ExcelParser.createSuite(SCPSampleTests.class);
    }

    /* TO BE MODIFIED BY IDMUNIT USER */
    public void testUser() {
    }
}
