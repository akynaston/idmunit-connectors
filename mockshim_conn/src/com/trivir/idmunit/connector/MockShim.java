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

import com.trivir.idmunit.connector.mock.MockShimException;
import com.trivir.idmunit.connector.mock.data.DataStore;
import com.trivir.idmunit.connector.mock.data.HsqlImpl;
import com.trivir.idmunit.connector.mock.data.SqlUtil;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static com.trivir.idmunit.connector.mock.util.JavaUtil.getStackTraces;
import static com.trivir.idmunit.connector.mock.util.JavaUtil.isBlank;
import static org.idmunit.connector.BasicConnector.*;


/**
 * Implements an IdMUnit connector for the MockShim
 *
 * @author Gordon Mathis, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connection
 */
public class MockShim extends AbstractConnector {
    protected static final String MOCKSHIM_DRIVER = "mockshim-driver-class";
    protected static final String STR_SUCCESS = "...SUCCESS";
    private static final String STR_SQL = "sql";
    private static Logger log = LoggerFactory.getLogger(MockShim.class);

    private DataStore dataStore;
    private Connection connection;
    private String user;
    private String password;
    private String server;

    public void setup(Map<String, String> config) throws IdMUnitException {

        user = config.get(BasicConnector.CONFIG_USER);
        if (isBlank(user)) {
            throw new IdMUnitException("Missing the username in the JDBC url");
        }
        password = config.get(CONFIG_PASSWORD);
        //allow for empty string
        if (password == null) {
            throw new IdMUnitException("Missing the password in the JDBC url");
        }
        server = config.get(CONFIG_SERVER);
        if (isBlank(server)) {
            throw new IdMUnitException("Missing the server in the JDBC url");
        }

        try {
            dataStore = new DataStore(server, user, password);
        } catch (MockShimException e) {
            throw new IdMUnitException(String.format("Unable to initialize database: %s", e.getMessage()), e);
        }
    }

    public void tearDown() throws IdMUnitException {
        DataStore.close(dataStore);

        SqlUtil.close(connection);
    }

    /*
        private java.sql.Connection getConnection(String serverUrl, String user, String password) throws IdMUnitException {
            try{
                  // make sure driver exists
                  Class.forName(jdbcDriver);
                }catch(Exception e){
                  throw new IdMUnitException("Missing library.  Please ensure that the jar file that contains the following class exists: " + jdbcDriver);
                }
                java.sql.Connection connection = null;
                try {
                    connection = DriverManager.getConnection(serverUrl, user, password);
                    log.debug(" Connected to " + serverUrl + " Database as " + user);
                    // Sets the auto-commit property for the connection to be false.
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    log.info("Error connecting to the Database "+'\n'+e.toString());
                    throw new IdMUnitException("Error connecting to the Database "+'\n'+e.toString(), e);
                }

                return connection;
        }
    */

    private void modifyObject(Map<String, Collection<String>> data, boolean withEvent) throws IdMUnitException {
        Map<String, Collection<String>> idata = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        idata.putAll(data);

        Map<String, Object> eventAttrs = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        String association = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_ASSOCIATION);
        if (association == null) {
            throw new IdMUnitException("Attribute '" + HsqlImpl.FIELD_ASSOCIATION + "' must be supplied.");
        } else {
            eventAttrs.put(HsqlImpl.FIELD_ASSOCIATION, association);
            idata.remove(HsqlImpl.FIELD_ASSOCIATION);
        }

        String objectClass = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_OBJECTCLASS);
        if (objectClass == null) {
            throw new IdMUnitException("Attribute '" + HsqlImpl.FIELD_OBJECTCLASS + "' must be supplied.");
        } else {
            eventAttrs.put(HsqlImpl.FIELD_OBJECTCLASS, objectClass);
            idata.remove(HsqlImpl.FIELD_OBJECTCLASS);
        }

        String srcDn = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_SRCDN);
        if (srcDn != null) {
            eventAttrs.put(HsqlImpl.FIELD_SRCDN, srcDn);
            idata.remove(HsqlImpl.FIELD_SRCDN);
        }

        String destDn = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_DESTDN);
        if (destDn != null) {
            eventAttrs.put(HsqlImpl.FIELD_DESTDN, destDn);
            idata.remove(HsqlImpl.FIELD_DESTDN);
        }

        //translate attr format
        Map<String, Object> modifyAttrs = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        for (Object attr : idata.entrySet()) {
            Map.Entry entry = (Map.Entry)attr;
            String attrName = (String)entry.getKey();
            modifyAttrs.put(attrName, entry.getValue());
        }
        eventAttrs.put(HsqlImpl.FIELD_ATTRIBUTES, modifyAttrs);

        try {
            dataStore.modifyRecord(eventAttrs, withEvent);
        } catch (MockShimException e) {
            throw new IdMUnitException("Unable to modify object", e);
        }

        log.info(STR_SUCCESS);
    }

    public void opModifyObject(Map<String, Collection<String>> data) throws IdMUnitException {
        modifyObject(data, true);
    }

    public void opModifyObjectNoEvent(Map<String, Collection<String>> data) throws IdMUnitException {
        modifyObject(data, false);
    }

    public void opValidateObject(Map<String, Collection<String>> data) throws IdMUnitException {

        Map<String, Collection<String>> idata = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        idata.putAll(data);

        // We are not going to validate SrcDN and DestDN on the publisher channel.
        String srcDn = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_SRCDN);
        if (srcDn != null) {
            idata.remove(HsqlImpl.FIELD_SRCDN);
        }

        String destDn = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_DESTDN);
        if (destDn != null) {
            idata.remove(HsqlImpl.FIELD_DESTDN);
        }

        dataStore.validateRecordData(idata);
    }


    public void opExecSQL(Map<String, Collection<String>> data) throws IdMUnitException {
        Statement st = null;
        try {
            String sql = ConnectorUtil.getSingleValue(data, STR_SQL);
            if (isBlank(sql)) {
                throw new IdMUnitException("Your '" + STR_SQL + "' column is missing for this JDBC connector");
            }
            log.info("...apply SQL statement: " + sql);
            st = connection.createStatement();

            st.executeUpdate(sql);
            log.info("..successful.");
        } catch (SQLException e) {
            throw new IdMUnitException("SQL Execution exception: " + e.getMessage(), e);
        } finally {
            SqlUtil.close(st);
        }
    }

    private void addObject(Map<String, Collection<String>> data, boolean withEvent) throws IdMUnitException {
        Map<String, Collection<String>> idata = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        idata.putAll(data);

        Map<String, Object> eventAttrs = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        String association = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_ASSOCIATION);
        if (association == null) {
            throw new IdMUnitException("Attribute '" + HsqlImpl.FIELD_ASSOCIATION + "' must be supplied.");
        } else {
            eventAttrs.put(HsqlImpl.FIELD_ASSOCIATION, association);
            idata.remove(HsqlImpl.FIELD_ASSOCIATION);
        }

        String objectClass = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_OBJECTCLASS);
        if (objectClass == null) {
            throw new IdMUnitException("Attribute '" + HsqlImpl.FIELD_OBJECTCLASS + "' must be supplied.");
        } else {
            eventAttrs.put(HsqlImpl.FIELD_OBJECTCLASS, objectClass);
            idata.remove(HsqlImpl.FIELD_OBJECTCLASS);
        }

        String srcDn = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_SRCDN);
        if (srcDn != null) {
            eventAttrs.put(HsqlImpl.FIELD_SRCDN, srcDn);
            idata.remove(HsqlImpl.FIELD_SRCDN);
        }

        String destDn = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_DESTDN);
        if (destDn != null) {
            eventAttrs.put(HsqlImpl.FIELD_DESTDN, destDn);
            idata.remove(HsqlImpl.FIELD_DESTDN);
        }

        //translate attr format
        Map<String, Object> addAttrs = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        for (Object attr : idata.entrySet()) {
            Map.Entry entry = (Map.Entry)attr;
            String attrName = (String)entry.getKey();
            addAttrs.put(attrName, entry.getValue());
        }
        eventAttrs.put(HsqlImpl.FIELD_ATTRIBUTES, addAttrs);

        try {
            dataStore.saveRecord(eventAttrs, withEvent);
        } catch (MockShimException e) {
            throw new IdMUnitException("Unable to add object", e);
        }

        log.info(STR_SUCCESS);
    }

    public void opAddObjectNoEvent(Map<String, Collection<String>> data) throws IdMUnitException {
        addObject(data, false);

    }

    public void opAddObject(Map<String, Collection<String>> data) throws IdMUnitException {
        addObject(data, true);
    }

    public void opTestConnection(Map<String, Collection<String>> data) throws IdMUnitException {

        String server1 = ConnectorUtil.getSingleValue(data, CONFIG_SERVER);
        if (isBlank(server1)) {
            server1 = this.server;
        }

        String user1 = ConnectorUtil.getSingleValue(data, CONFIG_USER);
        if (isBlank(user1)) {
            user1 = this.user;
        }

        String password1 = ConnectorUtil.getSingleValue(data, CONFIG_PASSWORD);
        if (password1 == null) {
            password1 = this.password;
        }

        log.info("Connecting to database ...");
        log.info(String.format("\tServer  : %s", server));
        log.info(String.format("\tUser    : %s", user));
        log.info(String.format("\tPassword: %s", password));

        DataStore dataStore1 = null;

        try {
            dataStore1 = new DataStore(server, user, password);
        } catch (MockShimException e) {
            log.error(getStackTraces(e));
            throw new IdMUnitException(String.format("Unable to connect to database: %s", e.getMessage(), e));
        } finally {
            DataStore.close(dataStore1);
        }
    }

    //alias for backward compatibility
    public void opDelObject(Map<String, Collection<String>> data) throws IdMUnitException {
        opDeleteObject(data);
    }

    private void deleteObject(Map<String, Collection<String>> data, boolean withEvent) throws IdMUnitException {
        Map<String, Collection<String>> idata = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        idata.putAll(data);

        Map<String, Object> eventAttrs = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        String association = ConnectorUtil.getSingleValue(idata, HsqlImpl.FIELD_ASSOCIATION);
        if (association == null) {
            throw new IdMUnitException("Attribute '" + HsqlImpl.FIELD_ASSOCIATION + "' must be supplied.");
        } else {
            eventAttrs.put(HsqlImpl.FIELD_ASSOCIATION, association);
        }

        try {
            dataStore.deleteRecord(eventAttrs, withEvent);
        } catch (MockShimException e) {
            throw new IdMUnitException("Unable to delete object '" + association + "'", e);
        }

        log.info(STR_SUCCESS);
    }

    public void opDeleteObjectNoEvent(Map<String, Collection<String>> data) throws IdMUnitException {
        deleteObject(data, false);
    }

    public void opDeleteObject(Map<String, Collection<String>> data) throws IdMUnitException {
        deleteObject(data, true);
    }

    @SuppressWarnings("unchecked")
    protected void insertRecord(Map<String, Collection<String>> data) throws IdMUnitException {

        String labels = "";
        String values = "";

        for (Object object : data.entrySet()) {
            Map.Entry entry = (Map.Entry)object;
            String label = (String)entry.getKey();
            labels += labels.length() > 0 ? ", " + label : label;
            String value = ((Collection<String>)entry.getValue()).iterator().next();
            values += values.length() > 0 ? ", " + value : value;
        }

        try {
            Statement stmt = connection.createStatement();
            String sql = "insert into data (" + labels + ")  values (" + values + ")";
            stmt.execute(sql);

        } catch (SQLException e) {
            throw new IdMUnitException("Error insert record to database: " + e.getMessage());
        }
    }

    protected DataStore getDataStore() {
        return dataStore;
    }

}
