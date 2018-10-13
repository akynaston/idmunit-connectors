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
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Implements an IdMUnit connector for DB2 (JDBC) running on an ISeries AS400
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connection
 */
public class JDBC extends AbstractConnector {
    protected static final String JDBC_DRIVER = "jdbc-driver-class";
    private static final String STR_SQL = "sql";
    private static Logger log = LoggerFactory.getLogger(JDBC.class);
    private String jdbcDriver = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
    private Connection connection;

    public void setup(Map<String, String> config) throws IdMUnitException {
        String user = config.get(BasicConnector.CONFIG_USER);
        if (user == null || user.length() == 0) {
            throw new IdMUnitException(String.format("Required configuration parameter '%s' is missing", BasicConnector.CONFIG_USER));
        }
        String password = config.get(BasicConnector.CONFIG_PASSWORD);
        if (password == null) {
            throw new IdMUnitException(String.format("Required configuration parameter '%s' is missing", BasicConnector.CONFIG_PASSWORD));
        }
        String server = config.get(BasicConnector.CONFIG_SERVER);
        if (server == null || server.length() == 0) {
            throw new IdMUnitException(String.format("Required configuration parameter '%s' is missing. The JDBC URL must be specified in the '%s' configuration.", BasicConnector.CONFIG_SERVER, BasicConnector.CONFIG_SERVER));
        }
        String driver = config.get(JDBC_DRIVER);
        if (driver == null || driver.length() == 0) {
            throw new IdMUnitException(String.format("Required configuration parameter '%s' is missing", JDBC_DRIVER));
        }

        jdbcDriver = driver;

        this.connection = getConnection(server, user, password);
    }

    public void tearDown() throws IdMUnitException {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IdMUnitException("Failed to close jdbc connection: " + e.getMessage(), e);
        }
        connection = null;
    }

    private java.sql.Connection getConnection(String serverUrl, String user, String password) throws IdMUnitException {
        try {
            // make sure driver exists
            Class.forName(jdbcDriver);
        } catch (ClassNotFoundException e) {
            throw new IdMUnitException("Missing library. Please ensure that the jar file that contains the following class exists: " + jdbcDriver);
        }
        java.sql.Connection sqlConnection = null;
        try {
            sqlConnection = DriverManager.getConnection(serverUrl, user, password);
            log.debug(" Connected to " + serverUrl + " Database as " + user);
            // Sets the auto-commit property for the connection to be false.
            sqlConnection.setAutoCommit(true);
        } catch (SQLException e) {
            log.info("Error connecting to the Database " + '\n' + e.toString());
            throw new IdMUnitException("Error connecting to the Database " + '\n' + e.toString(), e);
        }

        return sqlConnection;
    }

    private TreeMap<String, String> resultSetToTreeMap(ResultSet currentResultSet) throws IdMUnitException {
        //Oracle and HSQL database servers return the column names in UPPER CASE. MySQL server returns the column names in their actual case. A case insensitive map is used to avoid problems with the case on the column names.
        TreeMap<String, String> caseInsensitiveAttrsMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        try {
            currentResultSet.next(); //only compare the first row for now
            ResultSetMetaData metaData = currentResultSet.getMetaData();
            for (int ctr = 1; ctr < metaData.getColumnCount() + 1; ++ctr) {
                String colName = metaData.getColumnName(ctr);
                log.info("Column Name: " + colName);
                String attrVal = currentResultSet.getString(ctr);
                if (attrVal != null && attrVal.trim().length() > 0) {
                    caseInsensitiveAttrsMap.put(colName, attrVal.trim());
                    log.info("Column Val: " + attrVal.trim());
                }
            }
        } catch (SQLException e) {
            //Translate the error message to English for the case where the returned row was empty (query had no results)
            //String errorMessage = (e.getMessage().indexOf("Cursor position not valid")!=-1)?"Error: no results found for the SQL query provided" : "Failed toUpper JDBC attrs: " + e.getMessage();
            String errorMessage = "Error: no results found for the SQL query provided. " + e.getMessage();
            throw new IdMUnitException(errorMessage, e);
        }
        return caseInsensitiveAttrsMap;
    }

    public void opValidateObject(Map<String, Collection<String>> data) throws IdMUnitException {
        log.warn("The operation ValidateObject is deprecated. Please use the Validate operation instead.");
        opValidate(data);
    }

    public void opValidate(Map<String, Collection<String>> data) throws IdMUnitException {
        Statement stmt = null;
        ArrayList<String> errorsFound = new ArrayList<String>();
        try {
            String sql = ConnectorUtil.getSingleValue(data, STR_SQL);
            if (sql == null) {
                throw new IdMUnitException("The '" + STR_SQL + "' attribute is required for this operation.");
            }

            log.debug("SQL Statement: " + sql);

            ResultSet resultSet = null;
            stmt = connection.createStatement();
            resultSet = stmt.executeQuery(sql);

            TreeMap<String, String> caseInsensitiveAttrsMap = resultSetToTreeMap(resultSet);

            for (String colName : data.keySet()) {
                if (!(colName.equalsIgnoreCase(STR_SQL))) {
                    String expectedVal = ConnectorUtil.getSingleValue(data, colName);
                    String actualVal = caseInsensitiveAttrsMap.get(colName);
                    if (actualVal != null) {
                        log.info(".....validating attribute: [" + colName + "] EXPECTED: [" + expectedVal + "] ACTUAL: [" + actualVal.toString() + "]");
                        if (!actualVal.matches(expectedVal)) {
                            errorsFound.add("Validation failed: Attribute [" + colName + "] not equal.  Expected dest value: [" + expectedVal + "] Actual dest value(s): [" + actualVal.toString() + "]");
                            continue;
                        }
                        log.info("...SUCCESS");
                    } else {
                        errorsFound.add("Validation failed: Attribute [" + colName + "] not equal.  Expected dest value: [" + expectedVal + "] but the attribute value did not exist in the application.");
                    }
                }
            }
        } catch (SQLException e) {
            throw new IdMUnitException("Validation exception: " + e.getMessage(), e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException ex) {
                throw new IdMUnitException("Failed to close prepared statement: " + ex.getMessage(), ex);
            }

        }
        if (errorsFound.size() > 0) {
            StringBuffer failMessages = new StringBuffer("");
            for (Iterator<String> itErrors = errorsFound.iterator(); itErrors.hasNext(); ) {
                failMessages.append(itErrors.next());
                failMessages.append("\r\n");
            }
            throw new IdMUnitFailureException(failMessages.toString() + "\r\n[" + errorsFound.size() + "] errors found.");
        }
    }

    public void opExecSQL(Map<String, Collection<String>> data) throws IdMUnitException {
        Statement stmt = null;
        try {
            String sql = ConnectorUtil.getSingleValue(data, STR_SQL);
            if (sql == null || sql.length() == 0) {
                throw new IdMUnitException("The '" + STR_SQL + "' attribute is required for this operation.");
            }
            log.info("...apply SQL statement: " + sql);
            stmt = connection.createStatement();

            stmt.executeUpdate(sql);
            stmt.close();
            log.info("..successful.");
        } catch (SQLException e) {
            throw new IdMUnitException("SQL Execution exception: " + e.getMessage(), e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ex) {
                throw new IdMUnitException("Failed to close prepared statement: " + ex.getMessage(), ex);
            }
        }
    }

    protected Connection getConnection() {
        return connection;
    }

    protected void setConnection(Connection connection) {
        this.connection = connection;
    }
}
