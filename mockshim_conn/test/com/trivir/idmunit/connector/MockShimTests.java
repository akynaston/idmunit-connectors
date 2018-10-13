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

import com.trivir.idmunit.connector.mock.data.DataStore;
import com.trivir.idmunit.connector.mock.data.HsqlImpl;
import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.BasicConnector;

import java.sql.SQLException;
import java.util.*;

import static com.trivir.idmunit.connector.mock.data.DataStore.*;

public class MockShimTests extends TestCase {

    //Fields
    public static final String PRIMARY_KEY_COL = "ID";
    public static final String FIRST_NAME_COL = "FirstName";
    public static final String LAST_NAME_COL = "LastName";

    //Data
    public static final String TEST_USER1_PK = "1";
    public static final String TEST_USER1_FIRST_NAME = "Carl";
    public static final String TEST_USER1_LAST_NAME = "Kynasten";

    public static final String TEST_USER2_PK = "99";
    public static final String TEST_USER2_FIRST_NAME = "Andrew";
    //Second user last name is a NULL value hard coded

    private MockShim connector;
    private DataStore dataStore;

    protected void setUp() throws IdMUnitException {

        final String dbHost = "172.17.2.130";
        final int dbPort = HsqlImpl.getPort();
        final String dbName = HsqlImpl.getDbName();

        connector = new MockShim();

        Map<String, String> connectionConfig = new TreeMap<String, String>();

        connectionConfig.put(BasicConnector.CONFIG_SERVER, HsqlImpl.getUrl(dbHost, dbPort, dbName));
        connectionConfig.put(BasicConnector.CONFIG_USER, HsqlImpl.getUsername());
        connectionConfig.put(BasicConnector.CONFIG_PASSWORD, HsqlImpl.getPassword());

        connector.setup(connectionConfig);
        dataStore = connector.getDataStore();

        try {
            dataStore.deleteAllRecords();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    } //End setUp

    protected void tearDown() {
        if (dataStore != null) {
            try {
                dataStore.deleteAllRecords();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void testOpAddObject() throws IdMUnitException {

        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();

        attrs.put("ASSOC_ID", Arrays.asList("761634"));
        attrs.put("DEPTID", Arrays.asList("100"));
        attrs.put("DEPTNAME", Arrays.asList("Finance"));
        attrs.put("COST_CENTER", Arrays.asList("D100"));
        attrs.put("COST_CENTER_DESCR", Arrays.asList("Information Technology"));
        attrs.put("JOBTITLE", Arrays.asList("Sr. Lead Systems Engineer"));
        attrs.put("LOCATION", Arrays.asList("PA002"));
        attrs.put("COMPANY", Arrays.asList("BAE Systems, Inc"));
        attrs.put("BUSINESS_UNIT", Arrays.asList("Intelligence & Security"));
        attrs.put("SUPERVISOR_ID", Arrays.asList("765617"));
        attrs.put("COUNTRY", Arrays.asList("United States"));
        attrs.put("ADDRESS1", Arrays.asList("1100 Bairs Rd"));
        attrs.put("CITY", Arrays.asList("York"));
        attrs.put("STATE", Arrays.asList("PA"));
        attrs.put("POSTAL", Arrays.asList("17408"));
        attrs.put("FIRST_NAME", Arrays.asList("Deb"));
        attrs.put("MIDDLE_NAME", Arrays.asList("A"));
        attrs.put("LAST_NAME", Arrays.asList("Gantz"));
        attrs.put("MAIL_DROP", Arrays.asList("03-134B"));
        attrs.put("NAME", Arrays.asList("Deb A Gantz"));
        attrs.put("EMPLOYEE_TYPE", Arrays.asList("Employee"));

        attrs.put(HsqlImpl.FIELD_ASSOCIATION, Arrays.asList("761634"));
        attrs.put(HsqlImpl.FIELD_OBJECTCLASS, Arrays.asList("BAE_CI_SSO_IDSCHEMA"));
        attrs.put(HsqlImpl.FIELD_SRCDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));
        attrs.put(HsqlImpl.FIELD_DESTDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));

        connector.opAddObject(attrs);

        attrs = new HashMap<String, Collection<String>>();

        attrs.put(HsqlImpl.FIELD_ASSOCIATION, Arrays.asList("761634"));

        connector.opDeleteObject(attrs);

        attrs = new HashMap<String, Collection<String>>();

        attrs.put("ASSOC_ID", Arrays.asList("761634"));
        attrs.put("DEPTID", Arrays.asList("100"));
        attrs.put("DEPTNAME", Arrays.asList("Finance"));
        attrs.put("COST_CENTER", Arrays.asList("D100"));
        attrs.put("COST_CENTER_DESCR", Arrays.asList("Information Technology"));
        attrs.put("JOBTITLE", Arrays.asList("Sr. Lead Systems Engineer"));
        attrs.put("LOCATION", Arrays.asList("PA002"));
        attrs.put("COMPANY", Arrays.asList("BAE Systems, Inc"));
        attrs.put("BUSINESS_UNIT", Arrays.asList("Intelligence & Security"));
        attrs.put("SUPERVISOR_ID", Arrays.asList("765617"));
        attrs.put("COUNTRY", Arrays.asList("United States"));
        attrs.put("ADDRESS1", Arrays.asList("1100 Bairs Rd"));
        attrs.put("CITY", Arrays.asList("York"));
        attrs.put("STATE", Arrays.asList("PA"));
        attrs.put("POSTAL", Arrays.asList("17408"));
        attrs.put("FIRST_NAME", Arrays.asList("Deb"));
        attrs.put("MIDDLE_NAME", Arrays.asList("A"));
        attrs.put("LAST_NAME", Arrays.asList("Gantz"));
        attrs.put("MAIL_DROP", Arrays.asList("03-134B"));
        attrs.put("NAME", Arrays.asList("Deb A Gantz"));
        attrs.put("EMPLOYEE_TYPE", Arrays.asList("Employee"));

        attrs.put(HsqlImpl.FIELD_ASSOCIATION, Arrays.asList("761634"));
        attrs.put(HsqlImpl.FIELD_OBJECTCLASS, Arrays.asList("BAE_CI_SSO_IDSCHEMA"));
        attrs.put(HsqlImpl.FIELD_SRCDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));
        attrs.put(HsqlImpl.FIELD_DESTDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));

        connector.opAddObject(attrs);

    }

    public void testOpValidateObject() throws IdMUnitException {

        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();

        attrs.put("ASSOC_ID", Arrays.asList("761634"));
        attrs.put("DEPTID", Arrays.asList("100"));
        attrs.put("DEPTNAME", Arrays.asList("Finance"));
        attrs.put("COST_CENTER", Arrays.asList("D100"));
        attrs.put("COST_CENTER_DESCR", Arrays.asList("Information Technology"));
        attrs.put("JOBTITLE", Arrays.asList("Sr. Lead Systems Engineer"));
        attrs.put("LOCATION", Arrays.asList("PA002"));
        attrs.put("COMPANY", Arrays.asList("BAE Systems, Inc"));
        attrs.put("BUSINESS_UNIT", Arrays.asList("Intelligence & Security"));
        attrs.put("SUPERVISOR_ID", Arrays.asList("765617"));
        attrs.put("COUNTRY", Arrays.asList("United States"));
        attrs.put("ADDRESS1", Arrays.asList("1100 Bairs Rd"));
        attrs.put("CITY", Arrays.asList("York"));
        attrs.put("STATE", Arrays.asList("PA"));
        attrs.put("POSTAL", Arrays.asList("17408"));
        attrs.put("FIRST_NAME", Arrays.asList("Deb"));
        attrs.put("MIDDLE_NAME", Arrays.asList("A"));
        attrs.put("LAST_NAME", Arrays.asList("Gantz"));
        attrs.put("MAIL_DROP", Arrays.asList("03-134B"));
        attrs.put("NAME", Arrays.asList("Deb A Gantz"));
        attrs.put("EMPLOYEE_TYPE", Arrays.asList("Employee"));

        attrs.put(HsqlImpl.FIELD_ASSOCIATION, Arrays.asList("761634"));
        attrs.put(HsqlImpl.FIELD_OBJECTCLASS, Arrays.asList("BAE_CI_SSO_IDSCHEMA"));
        attrs.put(HsqlImpl.FIELD_SRCDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));
        attrs.put(HsqlImpl.FIELD_DESTDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));

        connector.opAddObject(attrs);

        connector.opValidateObject(attrs);

    }

    public void testOpDeleteObject() throws IdMUnitException {

        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();

        attrs.put(HsqlImpl.FIELD_ASSOCIATION, Arrays.asList("761634"));
//    attrs.put(HsqlImpl.FIELD_OBJECTCLASS, Arrays.asList("BAE_CI_SSO_IDSCHEMA"));
//    attrs.put(HsqlImpl.FIELD_SRCDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));
//    attrs.put(HsqlImpl.FIELD_DESTDN, Arrays.asList("\\TRIVIR\\VAULT\\TestUser1"));

        connector.opDeleteObject(attrs);
    }
} //End of JDBCTests class
