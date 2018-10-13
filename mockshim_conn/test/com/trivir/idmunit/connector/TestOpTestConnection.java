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
import com.trivir.idmunit.connector.mock.data.SqlUtil;
import junit.framework.TestCase;
import org.hsqldb.Server;
import org.idmunit.IdMUnitException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.idmunit.connector.BasicConnector.*;

public class TestOpTestConnection extends TestCase {

    private MockShim connector;
    private DataStore dataStore;

    public void setUp() throws IdMUnitException {
        final String dbHost = "172.17.2.130";
        final int dbPort = HsqlImpl.getPort();
        final String dbName = HsqlImpl.getDbName();

        connector = new MockShim();

        Map<String, String> connectionConfig = new TreeMap<String, String>();

        connectionConfig.put(CONFIG_SERVER, HsqlImpl.getUrl(dbHost, dbPort, dbName));
        connectionConfig.put(CONFIG_USER, HsqlImpl.getUsername());
        connectionConfig.put(CONFIG_PASSWORD, HsqlImpl.getPassword());

        connector.setup(connectionConfig);
        dataStore = connector.getDataStore();
    }

    public void tearDown() {
        if (dataStore != null) {
            try {
                dataStore.deleteAllRecords();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void testConnectionDefaultConfig() throws IdMUnitException {
        connector.opTestConnection(new HashMap<String, Collection<String>>());
    }

    public void testConnectionCustomConfig() {
        //start local database and try to connect to it

        final String dbHost = "localhost";
        final int dbPort = 0;
        final String dbName = "other";
        final String username = "sa";  //default
        final String password = "";  //default

        Server hsql = HsqlImpl.newServer(null, ".", dbName, 0);
        String url = HsqlImpl.getUrl(dbHost, dbPort, dbName);

        try {
            hsql.start();

            Connection conn = null;
            Statement st = null;

            try {
                conn = HsqlImpl.newConnection(url, username, password);
                st = conn.createStatement();
                HsqlImpl.createSchema(st);
            } catch (SQLException e) {
                fail();
            } finally {
                SqlUtil.close(st);
                SqlUtil.close(conn);
            }

            Map<String, Collection<String>> connectionConfig = new TreeMap<String, Collection<String>>();
            connectionConfig.put(CONFIG_SERVER, Arrays.asList(url));
            connectionConfig.put(CONFIG_USER, Arrays.asList(username));
            connectionConfig.put(CONFIG_PASSWORD, Arrays.asList(password));

            connector.opTestConnection(connectionConfig);
        } catch (IdMUnitException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            hsql.stop();
        }
    }
}
