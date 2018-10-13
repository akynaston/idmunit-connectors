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

package com.trivir.idmunit.connector.mock.data;

import org.hsqldb.Server;

import java.sql.*;

import static com.trivir.idmunit.connector.mock.util.JavaUtil.isBlank;

public class HsqlImpl {

    public static final String JDBC_DRIVER = "org.hsqldb.jdbcDriver";

    public static final String TABLE_VERSION = normalizeCase("VERSION");
    public static final String TABLE_EVENTS = normalizeCase("EVENTS");
    public static final String TABLE_DATA = normalizeCase("DATA");

    public static final String FIELD_ID = normalizeCase("ID");
    public static final String FIELD_DATA = normalizeCase("DATA");
    public static final String FIELD_ASSOCIATION = normalizeCase("ASSOCIATION");
    public static final String FIELD_OBJECTCLASS = normalizeCase("OBJECTCLASS");
    public static final String FIELD_EVENTTYPE = normalizeCase("EVENTTYPE");
    public static final String FIELD_SRCDN = normalizeCase("SRCDN");
    public static final String FIELD_DESTDN = normalizeCase("DESTDN");
    public static final String FIELD_CHANNEL = normalizeCase("CHANNEL");
    public static final String FIELD_ATTRIBUTES = normalizeCase("ATTRIBUTES");

    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 0;

    private static final String USERNAME_DEFAULT = "sa";
    private static final String PASSWORD_DEFAULT = "";
    private static final String SQL_DROP_TABLE = normalizeCase("drop table %s");
    private static final String JDBC_URL_PREFIX = "jdbc:hsqldb:hsql://";
    private static final String DB_NAME = normalizeCase("mockCache");
    private static final int DB_PORT = 4455;

    //TODO: replace with DatabaseMetadata lookup
    public static boolean tableExists(Connection conn, String tableName) {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement("select count(*) from " + tableName);
            ps.execute();
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            SqlUtil.close(ps);
        }
    }

    public static String normalizeCase(String s) {
        if (s == null) {
            return null;
        } else {
            return s.toUpperCase();
        }
    }

    public static Connection newConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static void loadJdbcDriver() throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    public static String getUsername() {
        return USERNAME_DEFAULT;
    }

    public static String getPassword() {
        return PASSWORD_DEFAULT;
    }

    public static void dropSchema(Statement st) throws SQLException {
        Connection conn = st.getConnection();

        String[] tableNames = new String[]{TABLE_VERSION, TABLE_EVENTS, TABLE_DATA};
        for (String tableName : tableNames) {
            if (tableExists(conn, TABLE_VERSION)) {
                st.execute(String.format(SQL_DROP_TABLE, tableName));
            }
        }
    }

    public static void createSchema(Statement st) throws SQLException, IllegalStateException {
        Connection conn = st.getConnection();

        if (!tableExists(conn, TABLE_VERSION)) {
            createVersionTable(conn);
        }

        if (tableExists(conn, TABLE_VERSION)) {
            validateSchemaVersion(conn, MAJOR_VERSION, MINOR_VERSION);
        }

        if (!tableExists(conn, TABLE_EVENTS)) {
            createEventTable(conn);
        }

        if (!tableExists(conn, TABLE_DATA)) {
            createDataTable(conn);
        }

    }

    private static void validateSchemaVersion(Connection conn, int expectedMajor, int expectedMinor) throws IllegalStateException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement("select ver_major, ver_minor from " + HsqlImpl.TABLE_VERSION);
            rs = ps.executeQuery();

            // We only care about the first entry. There should only be one.
            rs.next();
            int actualMajor = rs.getInt("ver_major");
            int actualMinor = rs.getInt("ver_minor");

            if (actualMajor != expectedMajor || actualMinor != expectedMinor) {
                throw new IllegalStateException(String.format("Unexpected schema version major(%d) minor(%d). Error reading cache.", actualMajor, actualMinor));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error retrieving schema version. Error reading cache ", e);
        } finally {
            SqlUtil.close(rs);
            SqlUtil.close(ps);
        }
    }

    private static boolean createVersionTable(Connection conn) {
        Statement st = null;
        PreparedStatement ps = null;

        try {
            st = conn.createStatement();

            String sql = "create table " + HsqlImpl.TABLE_VERSION +
                    "(ver_major integer," +
                    "ver_minor integer," +
                    "constraint uq_version unique(ver_major,ver_minor))";
            st.execute(sql);

            ps = conn.prepareStatement("insert into " + HsqlImpl.TABLE_VERSION + "(VER_MAJOR, VER_MINOR) values(?,?)");
            ps.setInt(1, MAJOR_VERSION);
            ps.setInt(2, MINOR_VERSION);
            ps.executeUpdate();

            return true;

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } finally {
            SqlUtil.close(st);
            SqlUtil.close(ps);
        }
    }

    private static boolean createEventTable(Connection conn) {
        Statement st = null;

        try {
            st = conn.createStatement();

            String sql = "create table " + HsqlImpl.TABLE_EVENTS +
                    "(id INTEGER IDENTITY," +
                    "association varchar(64)," +
                    "objectClass varchar(64)," +
                    "channel varchar(32)," +
                    "eventType varchar(16)," +
                    "state varchar(32))";

            st.execute(sql);

            return true;

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } finally {
            SqlUtil.close(st);
        }
    }

    private static boolean createDataTable(Connection conn) {
        Statement st = null;

        try {
            st = conn.createStatement();

            String sql = "create table " + HsqlImpl.TABLE_DATA +
                    "(id INTEGER IDENTITY," +
                    "association varchar(64)," +
                    "objectClass varchar(64)," +
                    "channel varchar(32), " +
                    "srcDn varchar(128)," +
                    "destDn varchar(128)," +
                    "eventType varchar(32)," +
                    "data blob," +
                    "UNIQUE(association))";

            st.execute(sql);

            return true;

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } finally {
            SqlUtil.close(st);
        }
    }

    public static String getDbName() {
        return DB_NAME;
    }

    public static int getPort() {
        return DB_PORT;
    }

    public static String getUrl(String dbHost, int dbPort, String dbName) {

        if (isBlank(dbHost)) {
            dbHost = "localhost";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(JDBC_URL_PREFIX);
        builder.append(dbHost);
        if (dbPort != 0) {
            builder.append(":").append(dbPort);
        }
        builder.append("/");
        builder.append(dbName);

        return builder.toString();
    }

    public static Server newServer(String host, String path, String name, Integer port) {

        Server server = new Server();
        server.setLogWriter(null);
        server.setSilent(true);

        //name
        if (isBlank(name)) {
            name = DB_NAME;
        }
        server.setDatabaseName(0, name);

        //host or path
        if (isBlank(host) || "localhost".equalsIgnoreCase(host)) {
            server.setDatabasePath(0, path);
        } else {
            server.setAddress(host);
        }

        //port
        if (port == null) {
            port = DB_PORT;
        }
        if (port != 0) {
            server.setPort(port);
        }

        return server;
    }

}
