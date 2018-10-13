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

import junit.framework.TestCase;
import oracle.jdbc.pool.OracleDataSource;
import org.idmunit.IdMUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OracleTestsUseServiceURL extends TestCase {
    //This test class is for use with an Oracle database installed.

    private static final String TABLE_NAME = "TestTable";
    private static final String TABLE_SPACE = "TestTableSpace";
    private static final String USER_NAME = "TestUser";
    private static final String USER_PASSWORD = "trivir";
    private static final String DBF = "test-tablespace.dbf";
    private static Logger log = LoggerFactory.getLogger(OracleTestsUseServiceURL.class);
    private java.sql.Connection connection = null;

    protected void setUp() throws IdMUnitException, SQLException {
        try {
            OracleDataSource oracleDataSource = new OracleDataSource();
            oracleDataSource.setUser("system");
            oracleDataSource.setPassword("trivir");
            oracleDataSource.setURL("jdbc:oracle:thin:@//10.10.30.128:1521/xe");

            connection = oracleDataSource.getConnection();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            log.info("Error in Connecting to the Database " + '\n' + e.toString());
            throw new IdMUnitException("Error in Connecting to the Database " + '\n' + e.toString());
        }

        try {
            connection.prepareStatement("DROP TABLESPACE " + TABLE_SPACE + " INCLUDING CONTENTS AND DATAFILES").execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        try {
            connection.prepareStatement("DROP USER " + USER_NAME + " CASCADE").execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        connection.prepareStatement("CREATE TABLESPACE " + TABLE_SPACE + " DATAFILE '" + DBF + "' SIZE 100M").execute();
        connection.prepareStatement("CREATE USER " + USER_NAME + " IDENTIFIED BY " + USER_PASSWORD + " DEFAULT TABLESPACE " + TABLE_SPACE).execute();
        connection.prepareStatement("GRANT CONNECT TO " + USER_NAME).execute();
        connection.prepareStatement("ALTER USER " + USER_NAME + " QUOTA UNLIMITED ON " + TABLE_SPACE).execute();
        connection.prepareStatement("CREATE TABLE " + USER_NAME + "." + TABLE_NAME + "(ATTR1 VARCHAR2(40BYTE), ATTR2 VARCHAR2(20BYTE)) LOGGING NOCOMPRESS NOCACHE NOPARALLEL MONITORING TABLESPACE " + TABLE_SPACE).execute();
        connection.prepareStatement("GRANT SELECT, INSERT, UPDATE, DELETE ON " + USER_NAME + "." + TABLE_NAME + " TO " + USER_NAME).execute();
    } //End setUp

    protected void tearDown() {
        if (connection != null) {
            dropDbSchema(connection);
        }
    } //End tearDown

    private void dropDbSchema(Connection dbConnection) {
        try {
            dbConnection.prepareStatement("DROP TABLE " + TABLE_NAME).execute();
            dbConnection.prepareStatement("DROP USER " + USER_NAME + " CASCADE").execute();
        } catch (SQLException e) {
            log.info("Error in removing table and user from Oracle Database " + '\n' + e.toString());
        }
    } //End dropDbSchema

    public void testMSWordApostropheCharacter() throws IdMUnitException, SQLException {
        String openingWordApostrophe = "u2018";
        String closingWordApostrophe = "u2019";
        String value = "Bad Apostrophe-" + openingWordApostrophe + closingWordApostrophe + "-";
        String statement = "INSERT INTO " + USER_NAME + "." + TABLE_NAME + " (ATTR1) VALUES ('" + value + "')";
        connection.prepareStatement(statement).execute();

        ResultSet rs = connection.prepareStatement("SELECT ATTR1 FROM " + USER_NAME + "." + TABLE_NAME).executeQuery();
        rs.next();
        assertEquals(value, rs.getString("Attr1"));

    } //End testMSWordApostropheCharacter
}
