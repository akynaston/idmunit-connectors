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
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.BasicConnector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class JDBCTests extends TestCase {
    //Database Configuration
    public static final String TABLE_NAME = "users";
    public static final String DB_NAME = "jdbc_database";
    public static final String DB_URL = "jdbc:hsqldb:mem:" + DB_NAME;
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
    private static final String STR_SQL = "sql";
    private static final String DB_USER_NAME = "sa";
    private static final String DB_PASSWORD = "";
    private static String jdbcDriver = "jdbc-driver-class";
    //Second user last name is a NULL value hard coded
    private java.sql.Connection connection = null;
    private JDBC connector;

    private static void addKeyValuePair(Map<String, Collection<String>> data, String key, String value) {
        //Put a Key/Value pair into a Map of type <String, Collection<String>>

        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(key, values);
    } //End addKeyValuePair

    protected void setUp() throws IdMUnitException {
        connector = new JDBC();
        Map<String, String> connectionConfig = new TreeMap<String, String>();

        connectionConfig.put(BasicConnector.CONFIG_USER, DB_USER_NAME);
        connectionConfig.put(BasicConnector.CONFIG_PASSWORD, DB_PASSWORD);
        connectionConfig.put(BasicConnector.CONFIG_SERVER, DB_URL);
        connectionConfig.put(jdbcDriver, "com.trivir.idmunit.connector.JDBC");

        connector.setup(connectionConfig);

        try {
            Class.forName("org.hsqldb.jdbcDriver");
            connection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_PASSWORD);

            PreparedStatement stmtCreateTable = null;
            PreparedStatement stmtInsert = null;
            PreparedStatement stmtSetPrimaryKey = null;
            try {
                stmtCreateTable = connection.prepareStatement("CREATE TABLE " + TABLE_NAME + "(" + PRIMARY_KEY_COL + " int, " + FIRST_NAME_COL + " varchar(255)," + LAST_NAME_COL + " varchar(255))");
                stmtCreateTable.execute();

                stmtSetPrimaryKey = connection.prepareStatement("ALTER TABLE " + TABLE_NAME + " ADD PRIMARY KEY (" + PRIMARY_KEY_COL + ")");
                stmtSetPrimaryKey.execute();

                stmtInsert = connection.prepareStatement("INSERT INTO " + TABLE_NAME + " VALUES ('" + TEST_USER1_PK + "','" + TEST_USER1_FIRST_NAME + "','" + TEST_USER1_LAST_NAME + "')");
                stmtInsert.executeUpdate();

                stmtInsert = connection.prepareStatement("INSERT INTO " + TABLE_NAME + " VALUES ('" + TEST_USER2_PK + "', '" + TEST_USER2_FIRST_NAME + "', NULL)");
                stmtInsert.executeUpdate();
            } finally {
                if (stmtCreateTable != null) {
                    stmtCreateTable.close();
                }
                if (stmtInsert != null) {
                    stmtInsert.close();
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Error loading database driver. Make sure required jars are correctly installed.");
        } catch (SQLException e) {
            System.out.println("Error creating connection to database. Cache not loaded '" + e + "'");
        }
    } //End setUp

    protected void tearDown() {
        if (connection != null) {
            dropDbSchema(connection);
        }
    } //End tearDown

    private void dropDbSchema(Connection tableConnection) {
        try {
            tableConnection.prepareStatement("drop table " + TABLE_NAME).execute();
        } catch (SQLException e) {
            System.out.println("Exception '" + e + "'");
        }
    } //End dropDbSchema

    public void testInvalidConnectionParameter() throws IdMUnitException {

        JDBC dbConnector = new JDBC();

        Map<String, String> connectionConfig = new TreeMap<String, String>();

        connectionConfig.put(BasicConnector.CONFIG_USER, DB_USER_NAME);
        connectionConfig.put(BasicConnector.CONFIG_PASSWORD, DB_PASSWORD);
        connectionConfig.put(BasicConnector.CONFIG_SERVER, "jdbc:hsqldb:");
        connectionConfig.put(jdbcDriver, "com.trivir.idmunit.connector.JDBC");


        try {
            dbConnector.setup(connectionConfig);
            fail();
        } catch (IdMUnitException e) {
            IdMUnitException compareIdMUnitException = new IdMUnitException();
            assertTrue(e.getClass() == compareIdMUnitException.getClass());
        }


    }

    public void testOpValidateSuccess() throws IdMUnitException {
        //Validate an record that is in the database with all the matching attributes, run opValidate.

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + TEST_USER1_PK);
        addKeyValuePair(testData, PRIMARY_KEY_COL, TEST_USER1_PK);
        addKeyValuePair(testData, FIRST_NAME_COL, TEST_USER1_FIRST_NAME);
        addKeyValuePair(testData, LAST_NAME_COL, TEST_USER1_LAST_NAME);

        connector.opValidate(testData);
    } //End testOpValidateSuccess

    public void testOpValidateIdNotFound() throws IdMUnitException {
        //Validate with a bad SELECT, expect failure, shouldn't be able to pull a record from the database.
        final String idNotThere = "10";

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + idNotThere);

        try {
            connector.opValidate(testData);
            fail();
        } catch (IdMUnitException e) {
            IdMUnitFailureException compareIdMUnitFailureException = new IdMUnitFailureException();
            assertTrue(e.getClass() != compareIdMUnitFailureException.getClass());
        }
    } //End testOpValidateIdNotFound

    public void testOpValidateFailed() throws IdMUnitException {
        //Validate with an incorrect attribute, expect IdMUnitFailure

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + TEST_USER1_PK);
        addKeyValuePair(testData, PRIMARY_KEY_COL, TEST_USER1_PK);
        addKeyValuePair(testData, FIRST_NAME_COL, TEST_USER1_FIRST_NAME);
        addKeyValuePair(testData, LAST_NAME_COL, "WrongName");

        try {
            connector.opValidate(testData);
            fail("Should have thrown an IdMUnitFailureException.");
        } catch (IdMUnitException e) {
            IdMUnitFailureException compareIdMUnitFailureException = new IdMUnitFailureException();
            assertTrue(e.getClass() == compareIdMUnitFailureException.getClass());
        }
    } //End testOpValidateFailed

    public void testOpValidateNullFieldValue() throws IdMUnitException {
        //Validate with a null value in a record, expect Success.
        //This test was written because of a NullPointerException that we had during development

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + TEST_USER2_PK);
        addKeyValuePair(testData, PRIMARY_KEY_COL, TEST_USER2_PK);
        addKeyValuePair(testData, FIRST_NAME_COL, TEST_USER2_FIRST_NAME);

        connector.opValidate(testData);
    } //End testOpValidateNullFieldValue

    public void testOpExecSQLInsert() throws IdMUnitException {
        //Insert a record into the database, validate the user is there.

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "INSERT INTO " + TABLE_NAME + " (" + PRIMARY_KEY_COL + ", " + FIRST_NAME_COL + ", " + LAST_NAME_COL + ") VALUES (2,'John','Doe')");
        connector.opExecSQL(testData);

        testData.clear();
        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = 2");
        addKeyValuePair(testData, "ID", "2");
        addKeyValuePair(testData, "FirstName", "John");
        addKeyValuePair(testData, "LastName", "Doe");

        connector.opValidate(testData);

    } //End testOpExecSQLInsert()

    public void testOpValidateEmptyResultSet() throws IdMUnitException {

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = 2");
        addKeyValuePair(testData, "ID", "2");
        addKeyValuePair(testData, "FirstName", "John");
        addKeyValuePair(testData, "LastName", "Doe");

        try {
            connector.opValidate(testData);
            fail("Should have thrown an IdMUnitFailureException.");
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().contains("ResultSet is empty"));
        }

    }


    public void testOpExecSQLInsertConflict() throws IdMUnitException {
        //Insert a conflicting PRIMARY KEY, expect failure.

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "INSERT INTO " + TABLE_NAME + " (" + PRIMARY_KEY_COL + ", " + FIRST_NAME_COL + ", " + LAST_NAME_COL + ") VALUES (1,'John','Doe')");

        try {
            connector.opExecSQL(testData);
            fail("Should have thrown an SQLException.");

        } catch (IdMUnitException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof SQLException);
        }
    } //End testOpExecSQLInsertConflict()


    public void testOpExecSQLDeleteObjectSuccess() throws IdMUnitException {
        //Delete a user, try to retrieve the user from the database, expect failure on retrieval, object shouldn't exist.

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "DELETE FROM " + TABLE_NAME + " WHERE ID=2");
        connector.opExecSQL(testData);

        testData.clear();
        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = 2");

        try {
            connector.opValidate(testData);
            fail("Should have thrown an SQLException.");
        } catch (IdMUnitException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause().getClass() == SQLException.class);
        }
    } //End testOpExecSQLDeleteObject()

    public void testOpExecSQLDeleteObjectDoesntExist() throws IdMUnitException {
        //Try to delete a user that isn't there, don't expect failure.

        final String idNotThere = "9";
        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "DELETE FROM " + TABLE_NAME + " WHERE ID = " + idNotThere);

        connector.opExecSQL(testData);
    } //End testOpExecSQLDeleteObjectDoesntExist()

    public void testOpExecSQLUpdateAttribute() throws IdMUnitException {
        //Update an attribute and validate the change

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "UPDATE " + TABLE_NAME + " SET " + FIRST_NAME_COL + "='John', " + LAST_NAME_COL + "='Doe'" + " WHERE ID='" + TEST_USER1_PK + "'");
        connector.opExecSQL(testData);

        testData.clear();
        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + TEST_USER1_PK);
        addKeyValuePair(testData, PRIMARY_KEY_COL, "1");
        addKeyValuePair(testData, FIRST_NAME_COL, "John");
        addKeyValuePair(testData, LAST_NAME_COL, "Doe");

        connector.opValidate(testData);
    } //End testOpExecSQLUpdateAttribute()

    public void testOpExecSQLUpdateAttributeNull() throws IdMUnitException {
        //Update an attribute to null and validate the change

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "UPDATE " + TABLE_NAME + " SET " + FIRST_NAME_COL + "=null WHERE ID='" + TEST_USER1_PK + "'");
        connector.opExecSQL(testData);

        testData.clear();
        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + TEST_USER1_PK);
        addKeyValuePair(testData, PRIMARY_KEY_COL, "1");
        addKeyValuePair(testData, FIRST_NAME_COL, JDBC.NULL);
        addKeyValuePair(testData, LAST_NAME_COL, TEST_USER1_LAST_NAME);

        connector.opValidate(testData);
    } //End testOpExecSQLUpdateAttributeNull()

    public void testOpExecSQLUpdateAttributeWhitespace() throws IdMUnitException {
        //Update an attribute to whitespace and validate the change

        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "UPDATE " + TABLE_NAME + " SET " + FIRST_NAME_COL + "=' ' WHERE ID='" + TEST_USER1_PK + "'");
        connector.opExecSQL(testData);

        testData.clear();
        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + TEST_USER1_PK);
        addKeyValuePair(testData, PRIMARY_KEY_COL, "1");
        addKeyValuePair(testData, FIRST_NAME_COL, " ");
        addKeyValuePair(testData, LAST_NAME_COL, TEST_USER1_LAST_NAME);

        connector.opValidate(testData);
    } //End testOpExecSQLUpdateAttributeWhitespace()

    public void testOpExecSQLUpdatePrimaryKey() throws IdMUnitException {
        //Update the Primary Key attribute and validate the change
        final String newPrimaryKey = "3";
        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "UPDATE " + TABLE_NAME + " SET ID='" + newPrimaryKey + "'" + " WHERE ID='" + TEST_USER1_PK + "'");
        connector.opExecSQL(testData);

        testData.clear();
        addKeyValuePair(testData, STR_SQL, "SELECT * FROM " + TABLE_NAME + " WHERE ID = " + newPrimaryKey);
        addKeyValuePair(testData, PRIMARY_KEY_COL, newPrimaryKey);
        addKeyValuePair(testData, FIRST_NAME_COL, TEST_USER1_FIRST_NAME);
        addKeyValuePair(testData, LAST_NAME_COL, TEST_USER1_LAST_NAME);

        connector.opValidate(testData);
    } //End testOpExecSQLUpdatePrimaryKey()

    public void testOpExecSQLUpdatePrimaryKeyConflict() throws IdMUnitException {
        //Update the Primary Key attribute, expect failure
        final String newPrimaryKey = "2";
        Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>();

        addKeyValuePair(testData, STR_SQL, "INSERT INTO " + TABLE_NAME + " (" + PRIMARY_KEY_COL + ", " + FIRST_NAME_COL + ", " + LAST_NAME_COL + ") VALUES (" + newPrimaryKey + ",'John','Doe')");
        connector.opExecSQL(testData);

        testData.clear();
        addKeyValuePair(testData, STR_SQL, "UPDATE " + TABLE_NAME + " SET ID='" + newPrimaryKey + "'" + " WHERE ID='" + TEST_USER1_PK + "'");

        try {
            connector.opExecSQL(testData);
            fail("PRIMARY KEY should be conflicted");
        } catch (IdMUnitException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof SQLException);
        }
    } //End testOpExecSQLUpdatePrimaryKeyConflict()
} //End of JDBCTests class
