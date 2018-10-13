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

import org.idmunit.IdMUnitException;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements an IdMUnit connector for DB2 (JDBC) running on an ISeries AS400
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connection
 */
public class ISeries extends JDBC {
    private static final String M_AS400_CLASS = "com.ibm.as400.access.AS400JDBCDriver";

    public void setup(Map<String, String> config) throws IdMUnitException {
        Map<String, String> newConfig = new HashMap<String, String>(config);
        newConfig.put(JDBC_DRIVER, M_AS400_CLASS);
        super.setup(config);
    }
}
