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

import oracle.jdbc.pool.OracleDataSource;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.BasicConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;

/**
 * Implements an IdMUnit connector for Oracle
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connection
 */
public class Oracle extends JDBC {
    private static Logger log = LoggerFactory.getLogger(Oracle.class);

    public void setup(Map<String, String> config) throws IdMUnitException {
        String user = config.get(BasicConnector.CONFIG_USER);
        String password = config.get(BasicConnector.CONFIG_PASSWORD);
        String server = config.get(BasicConnector.CONFIG_SERVER);

        setConnection(getConnection(server, user, password));
    }

    private java.sql.Connection getConnection(String serverUrl, String user, String password) throws IdMUnitException {
        java.sql.Connection connection = null;
        try {
            OracleDataSource oracleDataSource = new OracleDataSource();
            oracleDataSource.setUser(user);
            oracleDataSource.setPassword(password);
            oracleDataSource.setURL(serverUrl);
            log.debug(" Connected to " + serverUrl + " Database as " + user);
            connection = oracleDataSource.getConnection();
            connection.setAutoCommit(true);

        } catch (SQLException ex) {
            log.info("Error in Connecting to the Database " + '\n' + ex.toString());
            throw new IdMUnitException("Error in Connecting to the Database " + '\n' + ex.toString());
        }
        return connection;
    }
}
