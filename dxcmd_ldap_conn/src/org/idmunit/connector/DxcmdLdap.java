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

package org.idmunit.connector;

import com.trivir.idmunit.connector.DxcmdLdapConnector;
import org.idmunit.IdMUnitException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class DxcmdLdap extends DxcmdLdapConnector {
    public void opAddObject(Map<String, Collection<String>> data) throws IdMUnitException {
        Map<String, Collection<String>> fields = new HashMap<String, Collection<String>>();
        for (String name : data.keySet()) {
            fields.put(name.toLowerCase(), data.get(name));
        }

        String option = ConnectorUtil.getSingleValue(fields, "option");
        if (option == null) {
            throw new IdMUnitException("'option' not specified");
        }

        if ("migrateapp".equals(option)) {
            opMigrateApp(fields);
        } else if ("startjob".equals(option)) {
            opStartJob(fields);
        } else {
            throw new IdMUnitException("Unsupported option '" + option + "'");
        }
    }
}
