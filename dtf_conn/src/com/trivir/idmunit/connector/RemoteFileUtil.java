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
import org.idmunit.IdMUnitException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

class RemoteFileUtil implements FileUtil {
    private Session session;
    private ChannelSftp channel;

    private String writePath;

    RemoteFileUtil(String host, int port, int hostKeyType, byte[] hostKey, String user, String password, String readPath, String writePath) throws IdMUnitException {
        this.writePath = writePath;

        JSch jsch = new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");

        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
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

        try {
            if (readPath != null) {
                channel.cd(readPath);
            }
        } catch (SftpException e) {
            throw new IdMUnitException("Error changing to directory '" + readPath + "'", e);
        }
    }

    public void close() {
        if (channel != null) {
            channel.disconnect();
        }

        if (session != null) {
            session.disconnect();
        }
    }

    public List<FileInfo> listFiles() throws IdMUnitException {
        List<FileInfo> files = new ArrayList<FileInfo>();
        Vector<?> dirEntries;
        try {
            dirEntries = channel.ls(".");
            if (dirEntries == null) {
                throw new IdMUnitException("No remote files returned");
            }
        } catch (SftpException e) {
            throw new IdMUnitException("Error listing remote files", e);
        }

        for (int i = 0; i < dirEntries.size(); i++) {
            Object obj = dirEntries.elementAt(i);
            if (!(obj instanceof ChannelSftp.LsEntry)) {
                continue;
            }
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)obj;

            SftpATTRS attrs = entry.getAttrs();
            if (attrs.isReg()) {
                files.add(new FileInfo(entry.getFilename(), entry.getAttrs().getSize(), entry.getAttrs().getMTime()));
            }
        }

        return files;
    }

    public byte[] readFile(String filename, long skip) throws IdMUnitException {
        ByteArrayOutputStream dst = new ByteArrayOutputStream();
        try {
            if (skip != 0) {
                channel.get(filename, dst, null, ChannelSftp.RESUME, skip);
            } else {
                channel.get(filename, dst);
            }
        } catch (SftpException e) {
            throw new IdMUnitException("Error reading file", e);
        }

        return dst.toByteArray();
    }

    public void writeFile(String filename, String contents) throws IdMUnitException {
        String fullFilename = writePath + '/' + filename;
        ByteArrayInputStream newIs;
        try {
            newIs = new ByteArrayInputStream(contents.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 ");
        }

        try {
            channel.put(newIs, fullFilename);
        } catch (SftpException e) {
            throw new IdMUnitException("Error writing file", e);
        }
    }
}
