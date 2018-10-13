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

import com.trivir.idmunit.connector.MockShim;
import com.trivir.idmunit.connector.mock.MockShimException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.*;

import static com.trivir.idmunit.connector.mock.data.HsqlImpl.*;
import static com.trivir.idmunit.connector.mock.util.JavaUtil.isBlank;
import static com.trivir.idmunit.connector.mock.util.JavaUtil.isNullOrEmpty;

public class DataStore {

    public static final String SQL_INSERT_DATA = normalizeCase("insert into " + TABLE_DATA + "(association, objectClass , channel, srcDn, destDn, eventType, data) values(?,?,?,?,?,?,?)");
    public static final String SQL_INSERT_EVENTS = normalizeCase("insert into " + TABLE_EVENTS + "(association, objectClass, channel, eventType, state) values(?,?,?,?,?)");
    public static final String SQL_SELECT_DATA = normalizeCase("select * from " + TABLE_DATA + " where association=?");
    public static final String SQL_UPDATE_DATA = normalizeCase("update " + TABLE_DATA + " set objectClass=?, channel=?, srcDn=?, destDn=?, eventType=?, data=? where association=?");
    public static final String SQL_SELECT_EVENTS_FOR_PUBLICATION = normalizeCase("select * from " + TABLE_EVENTS + " where channel=? and state=?");
    public static final String SQL_UPDATE_EVENT_STATE = normalizeCase("update " + TABLE_EVENTS + " set state=? where id=?");
    public static final String SQL_DELETE_DATA = normalizeCase("delete from " + TABLE_DATA + " where association=?");
    public static final String SQL_DELETE_FROM = normalizeCase("DELETE FROM %s");


    public static final String EVENT_TYPE_ADD = "add";
    public static final String EVENT_TYPE_MODIFY = "modify";
    public static final String EVENT_TYPE_DELETE = "delete";

    public static final String CHANNEL_PUB = "pub";
    public static final String CHANNEL_SUB = "sub";

    public static final String STATE_ACTIVE = "active";
    public static final String STATE_ORPHANED = "orphaned";
    public static final String STATE_COMPLETE = "complete";

    private static Logger log = LoggerFactory.getLogger(MockShim.class);
    private static final String BYTE_ENCODING = "UTF-8";

    private Connection connection = null;
    private PreparedStatement psInsertData;
    private PreparedStatement psInsertEvents;
    private PreparedStatement psSelectData;
    private PreparedStatement psUpdateData;
    private PreparedStatement psSelectEventsForPublication;
    private PreparedStatement psUpdateEventState;
    private PreparedStatement psDeleteData;

    public DataStore(String url, String username, String password) throws MockShimException {
        try {
            loadJdbcDriver();
            connection = newConnection(url, username, password);
            initDb();
        } catch (ClassNotFoundException e) {
            throw new MockShimException(e.getMessage());
        } catch (SQLException e) {
            throw new MockShimException(e.getMessage());
        }
    }
    public static boolean deleteRecordSuppressed(DataStore dataStore, String association) {
        return deleteRecordSuppressed(dataStore, association, false);
    }

    public static boolean deleteRecordSuppressed(DataStore dataStore, String association, boolean triggerEvent) {
        boolean deleted = false;

        if (dataStore == null) {
            return deleted;
        }

        if (isBlank(association)) {
            return deleted;
        }

        // delete an user from database
        Map<String, Object> record = new HashMap<String, Object>();
        record.put(HsqlImpl.FIELD_ASSOCIATION, association);

        try {
            dataStore.deleteRecord(record, triggerEvent);
            deleted = true;
        } catch (MockShimException e) {
            //ignore exception
        }

        return deleted;
    }

    public static boolean deleteAllRecords(DataStore dataStore) {
        boolean deleted = false;

        if (dataStore == null) {
            return deleted;
        }

        String[] tableNames = new String[]{TABLE_DATA, TABLE_EVENTS};

        Statement st = null;

        try {
            st = dataStore.connection.createStatement();

            deleted = true;
            for (String tableName : tableNames) {
                try {
                    String sql = String.format(SQL_DELETE_FROM, tableName);
                    System.out.println("SQL: " + sql);
                    st.executeUpdate(sql);
                } catch (SQLException e) {
                    deleted = false;
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SqlUtil.close(st);
        }

        return deleted;
    }

    public static void close(DataStore dataStore) {
        if (dataStore != null) {
            dataStore.close();
        }
    }

    private void initDb() throws SQLException {
        psInsertData = connection.prepareStatement(SQL_INSERT_DATA);
        psInsertEvents = connection.prepareStatement(SQL_INSERT_EVENTS);
        psSelectData = connection.prepareStatement(SQL_SELECT_DATA);
        psUpdateData = connection.prepareStatement(SQL_UPDATE_DATA);
        psSelectEventsForPublication = connection.prepareStatement(SQL_SELECT_EVENTS_FOR_PUBLICATION);
        psUpdateEventState = connection.prepareStatement(SQL_UPDATE_EVENT_STATE);
        psDeleteData = connection.prepareStatement(SQL_DELETE_DATA);
    }

    public boolean deleteRecordSuppressed(String association, boolean withEvent) {
        return deleteRecordSuppressed(this, association, withEvent);
    }

    /* The data field or blob contains the following fields
    {
        "association": "12345",
        "object-class": "BAE_CI_SSO_IDSCHEMA",
        "channel": "pub",
        "src-dn": "",
        "dest-dn": "\\TRIVIR\\VAULT\\TestUser1",
        "event-type": "add",
        "attributes": {
            "FIRST_NAME": [
                "Gordon"
            ],
            "LAST_NAME": [
                "Mathis"
            ]
        }
    }
    */

    private void insertData(String association, String objectClass, String channel, String srcDn, String destDn, String eventType, String json) throws SQLException, UnsupportedEncodingException {
        psInsertData.clearParameters();
        psInsertData.setString(1, association);
        psInsertData.setString(2, objectClass);
        psInsertData.setString(3, channel);
        psInsertData.setString(4, srcDn);
        psInsertData.setString(5, destDn);
        psInsertData.setString(6, eventType);
        psInsertData.setBytes(7, fromJsonToBytes(json));

        System.out.println("SQL: " + SQL_INSERT_DATA);
        psInsertData.executeUpdate();
        System.out.println(String.format("Inserted record with association '%s'", association));
    }

    private void updateData(String association, String objectClass, String channel, String srcDn, String destDn, String eventType, String json) throws SQLException, UnsupportedEncodingException {
        psUpdateData.clearParameters();
        psUpdateData.setString(1, objectClass);
        psUpdateData.setString(2, channel);
        psUpdateData.setString(3, srcDn);
        psUpdateData.setString(4, destDn);
        psUpdateData.setString(5, eventType);
        psUpdateData.setBytes(6, fromJsonToBytes(json));
        psUpdateData.setString(7, association);

        System.out.println("SQL: " + SQL_UPDATE_DATA);
        psUpdateData.executeUpdate();

        System.out.println(String.format("Updated record with association '%s'", association));
    }

    private void insertEvent(String association, String objectClass, String channel, String eventType, String state) throws SQLException {
        psInsertEvents.clearParameters();
        psInsertEvents.setString(1, association);
        psInsertEvents.setString(2, objectClass);
        psInsertEvents.setString(3, channel);
        psInsertEvents.setString(4, eventType);
        psInsertEvents.setString(5, state);

        System.out.println("SQL: " + SQL_INSERT_EVENTS);
        psInsertEvents.executeUpdate();

        System.out.println(String.format("Inserted event with association '%s'", association));
    }

    public boolean saveRecord(Map<String, Object> record, boolean triggerEvent) throws MockShimException {
        final String eventType = EVENT_TYPE_ADD;
        final String channel = CHANNEL_PUB;

        if (isNullOrEmpty(record)) {
            return false;
        }

        try {
            Map<String, Object> irecord = toInsensitiveRecord(record);

            String association = (String)irecord.get(FIELD_ASSOCIATION);
            String objectClass = (String)irecord.get(FIELD_OBJECTCLASS);
            String srcDn = (String)getDnFromRecord(irecord, FIELD_SRCDN);
            String destDn = (String)getDnFromRecord(irecord, FIELD_DESTDN);

            // update the data and event tables
            insertData(
                    association,
                    objectClass,
                    channel,
                    srcDn,
                    destDn,
                    eventType,
                    fromRecordToJson(record));
            if (triggerEvent) {
                insertEvent(
                        association,
                        objectClass,
                        channel,
                        eventType,
                        STATE_ACTIVE);
            }

            return true;
        } catch (JsonGenerationException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new MockShimException("Invalid record syntax: " + e.getMessage(), e);
        }
    }

    public boolean deleteAllRecords() throws SQLException {
        return deleteAllRecords(this);
    }

    public boolean deleteRecord(Map<String, Object> record, boolean triggerEvent) throws MockShimException {

        ResultSet rs = null;

        try {
            Map<String, Object> irecord = toInsensitiveRecord(record);

            String association = (String)irecord.get(FIELD_ASSOCIATION);

            psSelectData.clearParameters();
            psSelectData.setString(1, association);

            System.out.println("SQL: " + SQL_SELECT_DATA);
            rs = psSelectData.executeQuery();

            String objectClass = (String)SqlUtil.getFirstValue(rs, FIELD_OBJECTCLASS);
            if (isBlank(objectClass)) {
                return false;
            }
            rs = SqlUtil.close(rs);

            psDeleteData.clearParameters();
            psDeleteData.setString(1, association);

            System.out.println("SQL: " + SQL_DELETE_DATA);
            psDeleteData.executeUpdate();

            if (triggerEvent) {
                insertEvent((String)record.get(FIELD_ASSOCIATION), objectClass, CHANNEL_PUB, EVENT_TYPE_DELETE, STATE_ACTIVE);
            }
        } catch (SQLException e) {
            throw new MockShimException(e.getMessage());
        } catch (ClassCastException e) {
            throw new MockShimException("Invalid record syntax: " + e.getMessage(), e);
        } finally {
            SqlUtil.close(rs);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean modifyRecord(Map<String, Object> record, boolean triggerEvent) throws MockShimException {
        final String eventType = EVENT_TYPE_MODIFY;
        final String channel = CHANNEL_PUB;

        try {
            Map<String, Object> irecord = toInsensitiveRecord(record);

            String association = (String)irecord.get(FIELD_ASSOCIATION);

            if (isBlank(association)) {
                throw new MockShimException("Attribute '" + FIELD_ASSOCIATION + "' is required.");
            }

            // use the assocation to get the record from the data table
            Map<String, Object> currentRecord = getRecord(association);

            // get any changes to the table attributes so that the data remains in sync
            currentRecord.put(FIELD_OBJECTCLASS, irecord.get(FIELD_OBJECTCLASS));
            currentRecord.put(FIELD_CHANNEL, channel);
            currentRecord.put(FIELD_SRCDN, getDnFromRecord(irecord, FIELD_SRCDN));
            currentRecord.put(FIELD_DESTDN, getDnFromRecord(irecord, FIELD_DESTDN));
            currentRecord.put(FIELD_EVENTTYPE, eventType);

            //these may be case-sensitive
            Map<String, String> modifiedAttrs = (Map<String, String>)irecord.get(FIELD_ATTRIBUTES);
            Map<String, String> currentAttrs = (Map<String, String>)currentRecord.get(FIELD_ATTRIBUTES);
            currentAttrs.putAll(modifiedAttrs);
            currentRecord.put(FIELD_ATTRIBUTES, currentAttrs);

            updateData(
                    (String)currentRecord.get(FIELD_ASSOCIATION),
                    (String)currentRecord.get(FIELD_OBJECTCLASS),
                    (String)currentRecord.get(FIELD_CHANNEL),
                    (String)currentRecord.get(FIELD_SRCDN),
                    (String)currentRecord.get(FIELD_DESTDN),
                    (String)currentRecord.get(FIELD_EVENTTYPE),
                    fromRecordToJson(currentRecord));

            if (triggerEvent) {
                insertEvent(
                        association,
                        (String)currentRecord.get(FIELD_OBJECTCLASS),
                        channel,
                        eventType,
                        STATE_ACTIVE);
            }


            return true;

        } catch (SQLException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new MockShimException("Invalid record syntax: " + e.getMessage(), e);
        }
    }


    @SuppressWarnings("unchecked")
    public Map<String, Object> getRecord(String association) throws MockShimException {

        ResultSet rs = null;

        try {
            psSelectData.clearParameters();
            psSelectData.setString(1, association);

            System.out.println("SQL: " + SQL_SELECT_DATA);
            rs = psSelectData.executeQuery();

            if (rs.next()) {
                String json = fromBytesToJson(rs.getBytes(FIELD_DATA));
                if (isBlank(json)) {
                    return null; //not found
                } else {
                    return fromJsonToRecord(json);
                }
            }
        } catch (SQLException e) {
            throw new MockShimException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new MockShimException(e.getMessage());
        } catch (JsonParseException e) {
            throw new MockShimException(e.getMessage());
        } catch (JsonMappingException e) {
            throw new MockShimException(e.getMessage());
        } catch (IOException e) {
            throw new MockShimException(e.getMessage());
        } finally {
            SqlUtil.close(rs);
        }

        return null;
    }

    //TODO: use generics?
    private static Map<String, Object> toInsensitiveRecord(Map<String, Object> map) {
        if (isNullOrEmpty(map)) {
            return new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        }

        Map<String, Object> imap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        imap.putAll(map);
        return imap;
    }

    //TODO: use generics?
    private static Map<String, String> toInsensitiveEvent(Map<String, String> map) {
        if (isNullOrEmpty(map)) {
            return new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        }

        Map<String, String> imap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        imap.putAll(map);
        return imap;
    }

    private static Object getDnFromRecord(Map<String, Object> record, String fieldName) {
        return record.get(fieldName) != null ? record.get(fieldName) : "";
    }

    public static Map<String, Object> fromJsonToRecord(String json) throws IOException {
        if (isBlank(json)) {
            return new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            return toInsensitiveRecord(mapper.readValue(json, Map.class));
        }
    }

    public static String fromRecordToJson(Map<String, Object> record) throws IOException {
        if (isNullOrEmpty(record)) {
            return "";
        } else {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(record);
        }
    }

    private static String fromBytesToJson(byte[] bytes) throws UnsupportedEncodingException {
        if (bytes == null) {
            return null;
        } else {
            return new String(bytes, BYTE_ENCODING);
        }
    }

    private static byte[] fromJsonToBytes(String json) throws UnsupportedEncodingException {
        if (json == null) {
            return null;
        } else {
            return json.getBytes(BYTE_ENCODING);
        }
    }

    public boolean updateRecord(Map<String, Object> record) throws MockShimException {

        try {
            Map<String, Object> iRecord = toInsensitiveRecord(record);

            updateData(
                    (String)iRecord.get(FIELD_ASSOCIATION),
                    (String)iRecord.get(FIELD_OBJECTCLASS),
                    (String)iRecord.get(FIELD_CHANNEL),
                    (String)iRecord.get(FIELD_SRCDN),
                    (String)iRecord.get(FIELD_DESTDN),
                    (String)iRecord.get(FIELD_EVENTTYPE),
                    fromRecordToJson(record));

            return true;

        } catch (JsonGenerationException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new MockShimException(e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new MockShimException("Invalid record syntax: " + e.getMessage(), e);
        }
    }


    public List<Map<String, String>> getPublisherEvents() throws MockShimException {
        final String channel = CHANNEL_PUB;
        final String state = STATE_ACTIVE;

        List<Map<String, String>> events = new ArrayList<Map<String, String>>();
        ResultSet rs = null;

        try {
            psSelectEventsForPublication.clearParameters();
            psSelectEventsForPublication.setString(1, channel);
            psSelectEventsForPublication.setString(2, state);

            System.out.println("SQL: " + SQL_SELECT_EVENTS_FOR_PUBLICATION);
            rs = psSelectEventsForPublication.executeQuery();

            ResultSetMetaData md = rs.getMetaData();

            while (rs.next()) {
                Map<String, String> event = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
                for (EventTable i : EventTable.values()) {
                    event.put(md.getColumnName(i.ordinal() + 1), rs.getString(i.ordinal() + 1));
                }
                events.add(event);
            }
            return events;

        } catch (SQLException e) {
            throw new MockShimException(e.getMessage());
        } finally {
            SqlUtil.close(rs);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean updateEventState(Map<String, String> event) throws MockShimException {
        return updateEventState(event, STATE_COMPLETE);
    }

    public boolean updateEventState(Map<String, String> event, String state) throws MockShimException {

        if (event == null) {
            return false;
        }

        Map<String, String> ievent = toInsensitiveEvent(event);

        String sId = ievent.get(FIELD_ID);
        if (isBlank(sId)) {
            throw new MockShimException(String.format("Missing required value for field %s", FIELD_ID));
        }

        int id;
        try {
            id = Integer.parseInt(sId);
        } catch (NumberFormatException nfe) {
            throw new MockShimException(String.format("Invalid %s: %s", FIELD_ID, sId));
        }

        try {
            psUpdateEventState.clearParameters();
            psUpdateEventState.setString(1, state);
            psUpdateEventState.setInt(2, id);

            System.out.println("SQL: " + SQL_UPDATE_EVENT_STATE);
            psUpdateEventState.executeUpdate();

            System.out.println(String.format("Updated event STATE to '%s' for event with ID %s", state, sId));

            return true;
        } catch (SQLException e) {
            throw new MockShimException(e.getMessage());
        }
    }

    public void close() {
        SqlUtil.close(connection);
    }

    //TODO: move to HsqlImpl
    public enum EventTable {
        ID, CHANNEL, OBJECT_CLASS, ASSOCIATION, EVENT_TYPE
    }

    //TODO: Move to MockShim
    @SuppressWarnings("unchecked")
    public void validateRecordData(Map<String, Collection<String>> data) throws IdMUnitException {
        Statement stmt = null;
        ArrayList<String> errorsFound = new ArrayList<String>();
        try {
            String association = ConnectorUtil.getSingleValue(data, FIELD_ASSOCIATION);
            if (association == null) {
                throw new IdMUnitException("Attribute '" + FIELD_ASSOCIATION + "' must be supplied.");
            }

            data.remove(FIELD_ASSOCIATION);

            String sql = "select " + FIELD_OBJECTCLASS + ", data from " + TABLE_DATA + " where association = '" + association + "'";

            Map<String, Object> attrs = null;

            ResultSet resultSet = null;
            stmt = connection.createStatement();
            resultSet = stmt.executeQuery(sql);

            if (resultSet.next()) {
                String pObjectClass = ConnectorUtil.getSingleValue(data, FIELD_OBJECTCLASS);
                if (pObjectClass == null) {
                    throw new IdMUnitException("Attribute: [" + FIELD_OBJECTCLASS + "] is required, but was not found");
                }
                String dObjectClass = resultSet.getString(1);

                if (pObjectClass == null || dObjectClass == null) {
                    throw new IdMUnitException(FIELD_OBJECTCLASS + " must be provided for this action, and must be available from the database, please add, and re-run test.");
                }

                if (pObjectClass.equals(dObjectClass)) {
                    data.remove(FIELD_OBJECTCLASS);
                } else {
                    throw new IdMUnitException("Attribute: [" + FIELD_OBJECTCLASS + "] EXPECTED: [" + pObjectClass + "] ACTUAL: [" + dObjectClass + "]");
                }

                byte[] temp = resultSet.getBytes(2);
                String json = new String(temp);
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> record = mapper.readValue(json, Map.class);
                attrs = (Map<String, Object>)record.get(FIELD_ATTRIBUTES);
            } else {
                throw new IdMUnitException("Record '" + association + "' does not exist.");
            }

            for (String colName : data.keySet()) {
                String expectedVal = ConnectorUtil.getSingleValue(data, colName);
                Collection attrValues = (Collection<String>)attrs.get(colName);
                String actualVal = null;
                if (attrValues != null) {
                    actualVal = (String)attrValues.iterator().next();
                }
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
        } catch (SQLException sqe) {
            throw new IdMUnitException("Validation exception: " + sqe.getMessage(), sqe);
        } catch (JsonParseException e) {
            throw new IdMUnitException("Validation exception: " + e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new IdMUnitException("Validation exception: " + e.getMessage(), e);
        } catch (IOException e) {
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
}
