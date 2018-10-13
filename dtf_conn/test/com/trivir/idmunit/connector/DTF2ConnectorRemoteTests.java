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

import com.jcraft.jsch.*;
import junit.framework.TestCase;
import org.idmunit.IdMUnitException;

import java.io.*;
import java.util.*;

public class DTF2ConnectorRemoteTests extends TestCase {
    private static final String TEST_BASE_DIR = "/tmp/";
    private static final String TEST_DIR = TEST_BASE_DIR + "dtftests/";
    private static final String TEST_READ_DIR = TEST_DIR + "input";
    private static final String TEST_WRITE_DIR = TEST_DIR + "output";
    private static final String HOST = "10.10.30.249";
    private static final int PORT = 22;
    private static final String USER = "trivir";
    private static final String PASSWORD = "Trivir#1";

    private Map<String, String> configParams = new HashMap<String, String>();
    private DTF2Connector dtfConn;

    private Session session;
    private ChannelSftp channel;

    public DTF2ConnectorRemoteTests() {
        dtfConn = new DTF2Connector();

        // Setup default configuration for all tests:
        configParams.put(DTF2Connector.READ_PATH, TEST_READ_DIR);
        configParams.put(DTF2Connector.WRITE_PATH, TEST_WRITE_DIR);
        configParams.put(DTF2Connector.DELIM, ",");
        configParams.put(DTF2Connector.ROW_KEY, "UserId"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".csv");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, "UserId, Name, FirstName, LastName, Group, Role");
        configParams.put(DTF2Connector.SSH_HOST, HOST);
        configParams.put(DTF2Connector.SSH_PORT, Integer.toString(PORT));
        configParams.put(DTF2Connector.SSH_USER, USER);
        configParams.put(DTF2Connector.SSH_PASSWORD, PASSWORD);
    }

    private static void addRows(StringBuilder data, String prefix, int numRows) {
        for (int i = 0; i < numRows; ++i) {
            data.append(prefix + "UserId" + i + ", ");
            data.append(prefix + "Name" + i + ", ");
            data.append(prefix + "FirstName" + i + ", ");
            data.append(prefix + "LastName" + i + ", ");
            data.append(prefix + "Group" + i + ", ");
            data.append(prefix + "Role" + i + "\r\n");
        }
    }

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    public void setUp() throws Exception {
        super.setUp();
        JSch jsch = new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");

        try {
            session = jsch.getSession(USER, HOST, PORT);
            session.setPassword(PASSWORD);
            session.connect(30000);
        } catch (JSchException e) {
            throw new IdMUnitException("Failed to connect.", e);
        }

        if (!session.isConnected()) {
            throw new IdMUnitException("Session failed to connect; information may be available above.");
        }

        try {
            Channel c = session.openChannel("sftp");
            c.connect();
            channel = (ChannelSftp)c;
        } catch (JSchException e) {
            throw new IdMUnitException("Unable to open sftp channel", e);
        }

        channel.cd(TEST_BASE_DIR);
        try {
            channel.mkdir(TEST_DIR);
        } catch (SftpException e) {
            if (e.id != 4) {
                throw e;
            }
        }

        try {
            channel.mkdir(TEST_READ_DIR);
        } catch (SftpException e) {
            if (e.id != 4) {
                throw e;
            }
        }

        try {
            channel.mkdir(TEST_WRITE_DIR);
        } catch (SftpException e) {
            if (e.id != 4) {
                throw e;
            }
        }

        if (!channel.stat(TEST_READ_DIR).isDir()) {
            throw new RuntimeException(TEST_READ_DIR + " exists but is not a directory");
        }

        if (!channel.stat(TEST_WRITE_DIR).isDir()) {
            throw new RuntimeException(TEST_WRITE_DIR + " exists but is not a directory");
        }

        channel.cd(TEST_READ_DIR);
    }

    public void tearDown() throws Exception {
        super.tearDown();

        channel.cd(TEST_READ_DIR);
        Vector<?> dirEntries = channel.ls(".");
        if (dirEntries != null) {
            for (int i = 0; i < dirEntries.size(); i++) {
                Object obj = dirEntries.elementAt(i);
                if (obj instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)obj;
                    SftpATTRS attrs = entry.getAttrs();
                    if (attrs.isReg()) {
                        channel.rm(entry.getFilename());
                    }
                }
            }
        }

        channel.cd(TEST_WRITE_DIR);
        dirEntries = channel.ls(".");
        if (dirEntries != null) {
            for (int i = 0; i < dirEntries.size(); i++) {
                Object obj = dirEntries.elementAt(i);
                if (obj instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)obj;
                    SftpATTRS attrs = entry.getAttrs();
                    if (attrs.isReg()) {
                        channel.rm(entry.getFilename());
                    }
                }
            }
        }

        channel.rmdir(TEST_READ_DIR);
        channel.rmdir(TEST_WRITE_DIR);
        channel.rmdir(TEST_DIR);

        if (channel != null) {
            channel.disconnect();
        }

        if (session != null) {
            session.disconnect();
        }

        dtfConn.tearDown();
    }

    public void testValidate() throws Exception {
        final int numRows = 4;

        for (int i = 0; i < 2; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", numRows);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        StringBuilder tempData = new StringBuilder();
        addRows(tempData, "tempfile-", numRows);
        ByteArrayInputStream tempIs = new ByteArrayInputStream(tempData.toString().getBytes("UTF-8"));
        channel.put(tempIs, "tempfile.tmp");

        dtfConn.setup(configParams);

        channel.rm("tempfile.tmp");

        StringBuilder newData = new StringBuilder();
        addRows(newData, "tempfile-", numRows * 2);
        ByteArrayInputStream newIs = new ByteArrayInputStream(newData.toString().getBytes("UTF-8"));
        channel.put(newIs, "tempfile.csv");

        for (int i = 2; i < 4; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", numRows);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        StringBuilder newTempData = new StringBuilder();
        addRows(newTempData, "newtempfile-", numRows);
        ByteArrayInputStream newTempIs = new ByteArrayInputStream(newTempData.toString().getBytes("UTF-8"));
        channel.put(newTempIs, "newtempfile.tmp");

        for (int r = numRows; r < numRows * 2; ++r) {
            Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
            expectedAttrs.put("UserId", singleValue("tempfile-UserId" + r));
            expectedAttrs.put("Name", singleValue("tempfile-Name" + r));
            expectedAttrs.put("FirstName", singleValue("tempfile-FirstName" + r));
            expectedAttrs.put("LastName", singleValue("tempfile-LastName" + r));
            expectedAttrs.put("Group", singleValue("tempfile-Group" + r));
            expectedAttrs.put("Role", singleValue("tempfile-Role" + r));
            dtfConn.opValidate(expectedAttrs);
        }

        for (int f = 2; f < 4; ++f) {
            for (int r = 0; r < numRows; ++r) {
                Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
                expectedAttrs.put("UserId", singleValue("file" + f + "-UserId" + r));
                expectedAttrs.put("Name", singleValue("file" + f + "-Name" + r));
                expectedAttrs.put("FirstName", singleValue("file" + f + "-FirstName" + r));
                expectedAttrs.put("LastName", singleValue("file" + f + "-LastName" + r));
                expectedAttrs.put("Group", singleValue("file" + f + "-Group" + r));
                expectedAttrs.put("Role", singleValue("file" + f + "-Role" + r));
                dtfConn.opValidate(expectedAttrs);
            }
        }

        for (int r = 0; r < numRows; ++r) {
            Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
            expectedAttrs.put("UserId", singleValue("newtempfile-UserId" + r));
            expectedAttrs.put("Name", singleValue("newtempfile-Name" + r));
            expectedAttrs.put("FirstName", singleValue("newtempfile-FirstName" + r));
            expectedAttrs.put("LastName", singleValue("newtempfile-LastName" + r));
            expectedAttrs.put("Group", singleValue("newtempfile-Group" + r));
            expectedAttrs.put("Role", singleValue("newtempfile-Role" + r));
            dtfConn.opValidate(expectedAttrs);
        }
    }

    public void testAdd() throws IdMUnitException, IOException, SftpException {
        dtfConn.setup(configParams);

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("UserId", singleValue("newtempfile-UserId"));
        expectedAttrs.put("Name", singleValue("newtempfile-Name"));
        expectedAttrs.put("FirstName", singleValue("newtempfile-FirstName"));
        expectedAttrs.put("LastName", singleValue("newtempfile-LastName"));
        expectedAttrs.put("Group", singleValue("newtempfile-Group"));
        expectedAttrs.put("Role", singleValue("newtempfile-Role"));
        dtfConn.opAdd(expectedAttrs);

        List<String> filenames = new ArrayList<String>();
        channel.cd(TEST_WRITE_DIR);
        Vector<?> dirEntries = channel.ls(".");
        if (dirEntries != null) {
            for (int i = 0; i < dirEntries.size(); i++) {
                Object obj = dirEntries.elementAt(i);
                if (obj instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)obj;
                    SftpATTRS attrs = entry.getAttrs();
                    if (attrs.isReg()) {
                        filenames.add(entry.getFilename());
                    }
                }
            }
        }

        assertEquals(1, filenames.size());

        assertTrue(filenames.get(0).endsWith(".csv"));
        ByteArrayOutputStream dst = new ByteArrayOutputStream();
        try {
            channel.get(filenames.get(0), dst);
        } catch (SftpException e) {
            throw new IdMUnitException("Error reading file", e);
        }

        BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(dst.toByteArray())));
        String data = r.readLine();
        assertEquals(-1, r.read()); // check that there is no more data
        r.close();

        assertEquals("newtempfile-UserId,newtempfile-Name,newtempfile-FirstName,newtempfile-LastName,newtempfile-Group,newtempfile-Role", data);
    }

    public void testValidateNoRollOverFromTMP() throws Exception {
        final int numRows = 4;

        for (int i = 0; i < 2; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", numRows);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        StringBuilder tempData = new StringBuilder();
        addRows(tempData, "tempfile-", numRows);
        ByteArrayInputStream tempIs = new ByteArrayInputStream(tempData.toString().getBytes("UTF-8"));
        channel.put(tempIs, "tempfile.tmp");

        dtfConn.setup(configParams);
        // Note: we can delete the file here; we are just re-writing a bunch of data to let it change in size and date
        channel.rm("tempfile.tmp");

        //NOTE: SWA bug: if tmp file doesn't roll over to a new csv file, in
        //      this case, we are simply recreating the tempfile.tmp,
        //      simulating an update to the file.
        StringBuilder newData = new StringBuilder();
        addRows(newData, "tempfile-", numRows * 2);
        ByteArrayInputStream newIs = new ByteArrayInputStream(newData.toString().getBytes("UTF-8"));
        channel.put(newIs, "tempfile.tmp");

        for (int i = numRows; i < numRows * 2; ++i) {
            Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
            expectedAttrs.put("UserId", singleValue("tempfile-UserId" + i));
            expectedAttrs.put("Name", singleValue("tempfile-Name" + i));
            expectedAttrs.put("FirstName", singleValue("tempfile-FirstName" + i));
            expectedAttrs.put("LastName", singleValue("tempfile-LastName" + i));
            expectedAttrs.put("Group", singleValue("tempfile-Group" + i));
            expectedAttrs.put("Role", singleValue("tempfile-Role" + i));
            dtfConn.opValidate(expectedAttrs);
        }

    }
}
