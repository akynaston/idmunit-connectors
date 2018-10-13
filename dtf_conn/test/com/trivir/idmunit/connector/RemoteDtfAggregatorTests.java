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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

public class RemoteDtfAggregatorTests extends TestCase {
    private static final String TEST_BASE_DIR = "/tmp/";
    private static final String TEST_DIR = "dtftests";
    private static final String HOST = "10.10.30.249";
    private static final int PORT = 22;
    private static final String USER = "trivir";
    private static final String PASSWORD = "Trivir#1";

    private RemoteFileUtil util;
    private Session session;
    private ChannelSftp channel;

    private static void addRows(StringBuilder data, String prefix, int numRows) {
        for (int i = 0; i < numRows; ++i) {
            data.append(prefix).append("field1-").append(i).append(", ");
            data.append(prefix).append("field2-").append(i).append(", ");
            data.append(prefix).append("field3-").append(i).append(", ");
            data.append(prefix).append("field4-").append(i).append("\r\n");
        }
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

        SftpATTRS attrs = channel.stat(TEST_DIR);
        if (!attrs.isDir()) {
            throw new RuntimeException(TEST_BASE_DIR + TEST_DIR + " exists but is not a directory");
        }

        channel.cd(TEST_DIR);

        util = new RemoteFileUtil(HOST, PORT, -1, null, USER, PASSWORD, TEST_BASE_DIR + TEST_DIR, TEST_BASE_DIR + TEST_DIR);
    }

    public void tearDown() throws Exception {
        super.tearDown();

        for (int i = 0; i < 4; ++i) {
            try {
                channel.rm("file" + i + ".csv");
            } catch (SftpException e) {
                if (e.id != 2) {
                    throw e;
                }
            }
        }

        try {
            channel.rm("tempfile.tmp");
        } catch (SftpException e) {
            if (e.id != 2) {
                throw e;
            }
        }

        try {
            channel.rm("newtempfile.tmp");
        } catch (SftpException e) {
            if (e.id != 2) {
                throw e;
            }
        }

        try {
            channel.rm("tempfile.csv");
        } catch (SftpException e) {
            if (e.id != 2) {
                throw e;
            }
        }

        channel.rmdir(TEST_BASE_DIR + TEST_DIR);

        if (channel != null) {
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }

        util.close();
    }

    public void testSkipExistingData() throws Exception {
        for (int i = 0; i < 4; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        StringBuilder data = new StringBuilder();
        addRows(data, "tempfile-", 4);
        ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
        channel.put(is, "tempfile.tmp");


        InputStream remoteDtfInputStream = new DtfAggregator(util, ".csv").getInputStream();

        assertEquals(0, remoteDtfInputStream.available());
        assertEquals(-1, remoteDtfInputStream.read());
    }

    public void testTempFileUpdate() throws Exception {
        StringBuilder data = new StringBuilder();
        addRows(data, "tempfile-", 4);
        ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
        channel.put(is, "tempfile.tmp");

        DtfAggregator cache = new DtfAggregator(util, ".csv");

        StringBuilder newData = new StringBuilder();
        addRows(newData, "tempfile-", 8);
        ByteArrayInputStream newIs = new ByteArrayInputStream(newData.toString().getBytes("UTF-8"));
        channel.put(newIs, "tempfile.tmp");

        InputStream remoteDtfInputStream = cache.getInputStream();
        assertTrue("available != 0", remoteDtfInputStream.available() != 0);

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        while (remoteDtfInputStream.available() != 0) {
            byte[] b = new byte[remoteDtfInputStream.available()];
            long bytesRead = remoteDtfInputStream.read(b);
            assertEquals(b.length, bytesRead);
            results.write(b);
        }

        assertTrue(Arrays.equals(newData.substring(data.length()).getBytes("UTF-8"), results.toByteArray()));

        assertEquals(-1, remoteDtfInputStream.read());
    }

    public void testTempFileRollover() throws Exception {
        StringBuilder data = new StringBuilder();
        addRows(data, "tempfile-", 4);
        ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
        channel.put(is, "tempfile.tmp");

        DtfAggregator cache = new DtfAggregator(util, ".csv");

        channel.rm("tempfile.tmp");

        StringBuilder newData = new StringBuilder();
        addRows(newData, "tempfile-", 8);
        ByteArrayInputStream newIs = new ByteArrayInputStream(newData.toString().getBytes("UTF-8"));
        channel.put(newIs, "tempfile.csv");

        InputStream remoteDtfInputStream = cache.getInputStream();
        assertTrue("available != 0", remoteDtfInputStream.available() != 0);

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        while (remoteDtfInputStream.available() != 0) {
            byte[] b = new byte[remoteDtfInputStream.available()];
            long bytesRead = remoteDtfInputStream.read(b);
            assertEquals(b.length, bytesRead);
            results.write(b);
        }

        assertTrue(Arrays.equals(newData.substring(data.length()).getBytes("UTF-8"), results.toByteArray()));

        assertEquals(-1, remoteDtfInputStream.read());
    }

    public void testNewTempFile() throws Exception {
        StringBuilder data = new StringBuilder();
        addRows(data, "tempfile-", 4);
        ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
        channel.put(is, "tempfile.tmp");

        DtfAggregator cache = new DtfAggregator(util, ".csv");

        channel.rm("tempfile.tmp");

        StringBuilder newData = new StringBuilder();
        addRows(newData, "tempfile-", 8);
        ByteArrayInputStream newIs = new ByteArrayInputStream(newData.toString().getBytes("UTF-8"));
        channel.put(newIs, "tempfile.csv");

        StringBuilder newTempData = new StringBuilder();
        addRows(newTempData, "newtempfile-", 4);
        ByteArrayInputStream newTempIs = new ByteArrayInputStream(newTempData.toString().getBytes("UTF-8"));
        channel.put(newTempIs, "newtempfile.tmp");

        InputStream remoteDtfInputStream = cache.getInputStream();
        assertTrue("available != 0", remoteDtfInputStream.available() != 0);

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        while (remoteDtfInputStream.available() != 0) {
            byte[] b = new byte[remoteDtfInputStream.available()];
            long bytesRead = remoteDtfInputStream.read(b);
            assertEquals(b.length, bytesRead);
            results.write(b);
        }

        String expected = newData.substring(data.length()) + newTempData;
        assertTrue(Arrays.equals(expected.getBytes("UTF-8"), results.toByteArray()));

        assertEquals(-1, remoteDtfInputStream.read());
    }

    public void testNewFiles() throws Exception {
        for (int i = 0; i < 2; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        DtfAggregator cache = new DtfAggregator(util, ".csv");

        StringBuilder expected = new StringBuilder();
        for (int i = 2; i < 4; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
            expected.append(data);
        }

        InputStream remoteDtfInputStream = cache.getInputStream();
        assertTrue("available != 0", remoteDtfInputStream.available() != 0);

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        while (remoteDtfInputStream.available() != 0) {
            byte[] b = new byte[remoteDtfInputStream.available()];
            long bytesRead = remoteDtfInputStream.read(b);
            assertEquals(b.length, bytesRead);
            results.write(b);
        }

        assertTrue(Arrays.equals(expected.toString().getBytes("UTF-8"), results.toByteArray()));

        assertEquals(-1, remoteDtfInputStream.read());
    }

    public void testTempRolloverWithNewFiles() throws Exception {
        for (int i = 0; i < 2; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        StringBuilder tempData = new StringBuilder();
        addRows(tempData, "tempfile-", 4);
        ByteArrayInputStream tempIs = new ByteArrayInputStream(tempData.toString().getBytes("UTF-8"));
        channel.put(tempIs, "tempfile.tmp");

        DtfAggregator cache = new DtfAggregator(util, ".csv");

        channel.rename("tempfile.tmp", "tempfile.csv");

        StringBuilder expected = new StringBuilder();
        for (int i = 2; i < 4; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
            expected.append(data);
        }

        InputStream remoteDtfInputStream = cache.getInputStream();
        ByteArrayOutputStream results = new ByteArrayOutputStream();
        while (remoteDtfInputStream.available() != 0) {
            byte[] b = new byte[remoteDtfInputStream.available()];
            long bytesRead = remoteDtfInputStream.read(b);
            assertEquals(b.length, bytesRead);
            results.write(b);
        }

        assertTrue(Arrays.equals(expected.toString().getBytes("UTF-8"), results.toByteArray()));

        assertEquals(-1, remoteDtfInputStream.read());
    }

    public void testTempRolloverWithNewTempDataAndNewFiles() throws Exception {
        for (int i = 0; i < 2; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        StringBuilder tempData = new StringBuilder();
        addRows(tempData, "tempfile-", 4);
        ByteArrayInputStream tempIs = new ByteArrayInputStream(tempData.toString().getBytes("UTF-8"));
        channel.put(tempIs, "tempfile.tmp");

        DtfAggregator cache = new DtfAggregator(util, ".csv");

        channel.rm("tempfile.tmp");

        StringBuilder newData = new StringBuilder();
        addRows(newData, "tempfile-", 8);
        ByteArrayInputStream newIs = new ByteArrayInputStream(newData.toString().getBytes("UTF-8"));
        channel.put(newIs, "tempfile.csv");

        StringBuilder expected = new StringBuilder();
        expected.append(newData.substring(newData.length() / 2));
        for (int i = 2; i < 4; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
            expected.append(data);
        }

        InputStream remoteDtfInputStream = cache.getInputStream();
        ByteArrayOutputStream results = new ByteArrayOutputStream();
        while (remoteDtfInputStream.available() != 0) {
            byte[] b = new byte[remoteDtfInputStream.available()];
            long bytesRead = remoteDtfInputStream.read(b);
            assertEquals(b.length, bytesRead);
            results.write(b);
        }

        assertTrue(Arrays.equals(expected.toString().getBytes("UTF-8"), results.toByteArray()));

        assertEquals(-1, remoteDtfInputStream.read());
    }

    public void testTempRolloverWithNewFilesAndNewTempFile() throws Exception {
        for (int i = 0; i < 2; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
        }

        StringBuilder tempData = new StringBuilder();
        addRows(tempData, "tempfile-", 4);
        ByteArrayInputStream tempIs = new ByteArrayInputStream(tempData.toString().getBytes("UTF-8"));
        channel.put(tempIs, "tempfile.tmp");

        DtfAggregator cache = new DtfAggregator(util, ".csv");

        channel.rm("tempfile.tmp");

        StringBuilder newData = new StringBuilder();
        addRows(newData, "tempfile-", 8);
        ByteArrayInputStream newIs = new ByteArrayInputStream(newData.toString().getBytes("UTF-8"));
        channel.put(newIs, "tempfile.csv");

        StringBuilder expected = new StringBuilder();
        expected.append(newData.substring(newData.length() / 2));
        for (int i = 2; i < 4; ++i) {
            StringBuilder data = new StringBuilder();
            addRows(data, "file" + i + "-", 4);
            ByteArrayInputStream is = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));
            channel.put(is, "file" + i + ".csv");
            expected.append(data);
        }

        StringBuilder newTempData = new StringBuilder();
        addRows(newTempData, "newtempfile-", 4);
        ByteArrayInputStream newTempIs = new ByteArrayInputStream(newTempData.toString().getBytes("UTF-8"));
        channel.put(newTempIs, "newtempfile.tmp");
        expected.append(newTempData);

        InputStream remoteDtfInputStream = cache.getInputStream();
        ByteArrayOutputStream results = new ByteArrayOutputStream();
        while (remoteDtfInputStream.available() != 0) {
            byte[] b = new byte[remoteDtfInputStream.available()];
            long bytesRead = remoteDtfInputStream.read(b);
            assertEquals(b.length, bytesRead);
            results.write(b);
        }

        assertTrue(Arrays.equals(expected.toString().getBytes("UTF-8"), results.toByteArray()));

        assertEquals(-1, remoteDtfInputStream.read());
    }
}
