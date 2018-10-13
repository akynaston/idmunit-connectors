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

import java.sql.*;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.trivir.idmunit.connector.mock.util.JavaUtil.checkNotBlank;
import static com.trivir.idmunit.connector.mock.util.JavaUtil.checkNotNull;

public class SqlUtil {

    public static final Set<Integer> TYPE_STRING;
    public static final Set<Integer> TYPE_NUMBER;
    public static final Set<Integer> TYPE_BINARY;
    public static final Set<Integer> TYPE_TIME;

    static {
        Set<Integer> set = new HashSet<Integer>();

        set.add(Types.CHAR);
        set.add(Types.LONGNVARCHAR);
        set.add(Types.LONGVARCHAR);
        set.add(Types.VARCHAR);
        set.add(Types.NVARCHAR);
        set.add(Types.CLOB);
        set.add(Types.NCLOB);

        TYPE_STRING = Collections.unmodifiableSet(set);

        set = new HashSet<Integer>();
        set.add(Types.BIGINT);
        set.add(Types.BIT);
        set.add(Types.DECIMAL);
        set.add(Types.DOUBLE);
        set.add(Types.FLOAT);
        set.add(Types.INTEGER);
        set.add(Types.NUMERIC);
        set.add(Types.REAL);
        set.add(Types.SMALLINT);
        set.add(Types.TINYINT);

        TYPE_NUMBER = Collections.unmodifiableSet(set);

        set = new HashSet<Integer>();
        set.add(Types.BINARY);
        set.add(Types.BLOB);
        set.add(Types.LONGVARBINARY);
        set.add(Types.VARBINARY);

        TYPE_BINARY = Collections.unmodifiableSet(set);

        set = new HashSet<Integer>();
        set.add(Types.DATE);
        set.add(Types.TIME);
        set.add(Types.TIMESTAMP);
        TYPE_TIME = Collections.unmodifiableSet(set);
    }

    public static void printTable(Statement stmt, String qualifiedName) throws SQLException {
        checkNotNull("stmt", stmt);
        checkNotBlank("qualifiedName", qualifiedName);

        ResultSet rs = stmt.executeQuery("SELECT * FROM " + qualifiedName);
        System.out.println("TABLE: '" + qualifiedName + "'");
        printResultSet(rs);
    }

    @SuppressWarnings("checkstyle:EmptyBlock")
    public static void printResultSet(ResultSet rs) throws SQLException {
        if (rs == null) {
            return;
        }

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) {
                    System.out.print(",  ");
                }
                boolean isText = TYPE_STRING.contains(rsmd.getColumnType(i));
                boolean isBinary = TYPE_BINARY.contains(rsmd.getColumnType(i));

                String columnValue = "";

                if (isBinary) {
                    byte[] bytes = rs.getBytes(i);
                    if (bytes == null || bytes.length == 0) {
                        columnValue = "[binary populated]";
                    } else {
                        columnValue = "[binary unpopulated]";
                    }
                } else {
                    columnValue = rs.getString(i);
                }

                if (isText) {
                    columnValue = "'" + columnValue + "'";
                } else if (!isBinary) {
                }
                System.out.print(rsmd.getColumnName(i) + ": " + columnValue);
            }
            System.out.println("");
        }
    }


    public static Connection close(Connection c) {
        if (c == null) {
            return null;
        }

        try {
            c.close();
        } catch (SQLException e) {
            //ignore
        }

        return c;
    }

    public static Statement close(Statement s) {
        if (s == null) {
            return null;
        }

        try {
            s.close();
        } catch (SQLException e) {
            //ignore
        }

        return null;
    }

    public static ResultSet close(ResultSet rs) {
        if (rs == null) {
            return null;
        }

        try {
            rs.close();
        } catch (SQLException e) {
            //ignore
        }

        return null;
    }

    public static Connection rollback(Connection c) {
        if (c == null) {
            return null;
        }

        try {
            c.rollback();
        } catch (SQLException e) {
            //ignore
        }

        return c;
    }

    public static boolean match(Statement stmt, String sql) throws SQLException {
        checkNotNull("stmt", stmt);
        checkNotBlank("sql", sql);

        ResultSet rs = null;
        boolean match;

        try {
            stmt.execute(sql);
            rs = stmt.getResultSet();
            match = rs.next();
            return match;
        } catch (SQLException e) {
            throw e;
        } finally {
            close(rs);
        }
    }

    public static int getGeneratedKey(PreparedStatement ps) throws SQLException {
        checkNotNull("ps", ps);

        ResultSet rs = null;
        try {
            rs = ps.getGeneratedKeys();
            rs.next();
            int id = rs.getInt(1);
            rs.close();
            return id;
        } finally {
            close(rs);
        }
    }

    public static long[] getUnits(long millis) {
        long[] part = new long[5];
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        part[0] = days;
        part[1] = hours % 24;
        part[2] = minutes % 60;
        part[3] = seconds % 60;
        part[4] = millis % 1000;

        return part;
    }

    //TODO: use TimeUnit constants instead of manual calculations
    public static long add(Timestamp addTo, long toAdd, TimeUnit unit) {
        long sum;
        long addToMillis = addTo.getTime();

        switch (unit) {
            case DAYS:
                sum = addToMillis + (toAdd * 24 * 60 * 60 * 1000);
                break;
            case HOURS:
                sum = addToMillis + (toAdd * 60 * 60 * 1000);
                break;
            case MINUTES:
                sum = addToMillis + (toAdd * 60 * 1000);
                break;
            case SECONDS:
                sum = addToMillis + (toAdd * 1000);
                break;
            case MILLISECONDS:
                sum = addToMillis + toAdd;
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported time unit: %s", unit.toString().toLowerCase()));
        }

        return sum;
    }

    public static long subtract(Timestamp subtractFrom, long toSubtract, TimeUnit unit) {
        long sum;
        long subtractFromMillis = subtractFrom.getTime();

        switch (unit) {
            case DAYS:
                sum = subtractFromMillis - (toSubtract * 24 * 60 * 60 * 1000);
                break;
            case HOURS:
                sum = subtractFromMillis - (toSubtract * 60 * 60 * 1000);
                break;
            case MINUTES:
                sum = subtractFromMillis - (toSubtract * 60 * 1000);
                break;
            case SECONDS:
                sum = subtractFromMillis - (toSubtract * 1000);
                break;
            case MILLISECONDS:
                sum = subtractFromMillis - toSubtract;
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported time unit: %s", unit.toString().toLowerCase()));
        }

        return sum;
    }

    public static long diff(java.sql.Timestamp currentTime, java.sql.Timestamp oldTime, TimeUnit unit) {
        long milliseconds1 = oldTime.getTime();
        long milliseconds2 = currentTime.getTime();

        long diff = milliseconds2 - milliseconds1;
        long units;

        switch (unit) {
            case DAYS:
                units = diff / (24 * 60 * 60 * 1000);
                break;
            case HOURS:
                units = diff / (60 * 60 * 1000);
                break;
            case MINUTES:
                units = diff / (60 * 1000);
                break;
            case SECONDS:
                units = diff / 1000;
                break;
            case MILLISECONDS:
                units = diff;
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported time unit: %s", unit.toString().toLowerCase()));
        }

        return units;
    }

    public static Timestamp now() {
        return new Timestamp(Calendar.getInstance().getTime().getTime());
    }

    //assumes rs is new (i.e., next() hasn't been called yet)
    public static boolean hasRows(ResultSet rs) throws SQLException {
        boolean has = false;

        if (rs == null) {
            return has;
        }

        if (rs.getType() == ResultSet.TYPE_FORWARD_ONLY) {
            throw new IllegalArgumentException("Result set must be scrollable");
        }

        boolean exception = false;
        try {
            has = rs.next();
        } catch (SQLException e) {
            exception = true;
            throw e;
        } finally {
            if (!exception) {
                //reset cursor
                rs.beforeFirst();
            }
        }

        return has;
    }

    public static int getSize(ResultSet rs) throws SQLException {
        int size = 0;

        if (rs == null) {
            return size;
        }

        if (rs.getType() == ResultSet.TYPE_FORWARD_ONLY) {
            throw new IllegalArgumentException("Result set must be scrollable");
        }

        boolean exception = false;
        int current = rs.getRow();
        try {
            rs.last();
            size = rs.getRow();
        } catch (SQLException e) {
            exception = true;
            throw e;
        } finally {
            if (!exception) {
                //reset cursor
                rs.absolute(current);
            }
        }

        return size;
    }

    public static Object getFirstValue(Statement stmt, String sql) throws SQLException {
        return getFirstValue(stmt, sql, 1);
    }

    public static Object getFirstValue(Statement stmt, String sql, int index) throws SQLException {
        Object value = null;

        ResultSet rs = null;
        try {
            stmt.execute(sql);
            rs = stmt.getResultSet();
            if (rs != null) {
                while (rs.next()) {
                    value = rs.getObject(index);
                    break;
                }
            }
        } finally {
            SqlUtil.close(rs);
        }

        return value;
    }

    public static Object getFirstValue(Statement stmt, String sql, String columnName) throws SQLException {
        Object value = null;

        ResultSet rs = null;
        try {
            stmt.execute(sql);
            rs = stmt.getResultSet();
            if (rs != null) {
                if (rs.next()) {
                    value = rs.getObject(columnName);
                }
            }
        } finally {
            SqlUtil.close(rs);
        }

        return value;
    }

    public static Object getFirstValue(ResultSet rs, String columnName) throws SQLException {
        Object value = null;

        if (rs != null) {
            if (rs.next()) {
                value = rs.getObject(columnName);
            }
        }

        return value;
    }
}
