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
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

public class SshConnector extends AbstractConnector {
    private static final String PASSWORD = "password";
    private static final String PORT = "port";
    private static final String SERVER = "server";
    private static final String USER = "user";
    private static final String SERVER_KEY = "host-key";
    private static final String SERVER_KEY_TYPE = "host-key-type";
    private static final String RSA_PRIVATE_KEY = "rsa-private-key";
    private static final String DSA_PRIVATE_KEY = "dsa-private-key";

    private static Marker fatal = MarkerFactory.getMarker("FATAL");
    private static Logger log = LoggerFactory.getLogger(SshConnector.class);

    private int port = 22;
    private String host;
    private byte[] hostKey = null;
    private int hostKeyType = 0;
    private String user;
    private String password;
    private Identity identity;

    public void setup(Map<String, String> config) throws IdMUnitException {
        host = config.get(SERVER);
        String serverPort = config.get(PORT);
        if (serverPort != null) {
            this.port = Integer.parseInt(serverPort);
        }
        user = config.get(USER);
        password = config.get(PASSWORD);

        if (config.get(SERVER_KEY) != null) {
            try {
                hostKey = new BASE64Decoder().decodeBuffer(config.get(SERVER_KEY));
            } catch (IOException e) {
                throw new IdMUnitException("Error decoding " + SERVER_KEY, e);
            }
        }

        String type = config.get(SERVER_KEY_TYPE);
        if (type != null) {
            if ("ssh-dss".equals(type)) {
                hostKeyType = HostKey.SSHDSS;
            } else if ("ssh-rsa".equals(type)) {
                hostKeyType = HostKey.SSHRSA;
            }
        }

        String rsaPrivateKey = config.get(RSA_PRIVATE_KEY);
        if (rsaPrivateKey != null) {
            String name = "idmunit";
            byte[] prvkey;
            try {
                prvkey = new BASE64Decoder().decodeBuffer(config.get(RSA_PRIVATE_KEY));
            } catch (IOException e) {
                throw new IdMUnitException("Error decoding " + RSA_PRIVATE_KEY, e);
            }
            try {
                identity = IdentityRSA.parsePKCS1(name, prvkey);
            } catch (JSchException e) {
                throw new IdMUnitException("Error parsing " + RSA_PRIVATE_KEY, e);
            }
        }

        String dsaPrivateKey = config.get(DSA_PRIVATE_KEY);
        if (dsaPrivateKey != null) {
            String name = "idmunit";
            byte[] prvkey = null;
            try {
                prvkey = dsaPrivateKey.getBytes("US-ASCII");
            } catch (UnsupportedEncodingException e) {
                //ignore exception
            }
//            try {
//                prvkey = new BASE64Decoder().decodeBuffer(config.get(RSA_PRIVATE_KEY));
//            } catch (IOException e) {
//                throw new IdMUnitException("Error decoding " + RSA_PRIVATE_KEY, e);
//            }
            try {
                identity = new IdentityPEM(name, prvkey);
            } catch (JSchException e) {
                throw new IdMUnitException("Unable to create identity for keys", e);
            }
        }
    }

    public void opExec(Map<String, Collection<String>> attrs) throws IdMUnitException {
        String output = executeSshCommand(attrs);
        log.info("ssh command output:" + System.getProperty("line.separator") + output);
    }

    public void opValidate(Map<String, Collection<String>> attrs) throws IdMUnitException {
        String actualOutput = executeSshCommand(attrs);

        String expectedOutput = ConnectorUtil.getSingleValue(attrs, "output");

        if (actualOutput.matches(expectedOutput) == false) {
            throw new IdMUnitFailureException("output expected:<" + expectedOutput + "> but was:<" + actualOutput + ">");
        }
    }

    private String executeSshCommand(Map<String, Collection<String>> attrs) throws IdMUnitException {
        try {
            JSch jsch = new JSch();
            JSch.setLogger(new com.jcraft.jsch.Logger() {
                private Logger logJSch = LoggerFactory.getLogger(JSch.class); // "com.jcraft.jsch.JSch"

                public boolean isEnabled(int i) {
                    switch (i) {
                        case com.jcraft.jsch.Logger.DEBUG:
                            return logJSch.isDebugEnabled();
                        case com.jcraft.jsch.Logger.INFO:
                            return logJSch.isInfoEnabled();
                        case com.jcraft.jsch.Logger.WARN:
                            return logJSch.isWarnEnabled();
                        case com.jcraft.jsch.Logger.ERROR:
                            return logJSch.isErrorEnabled();
                        case com.jcraft.jsch.Logger.FATAL:
                            return logJSch.isErrorEnabled();
                        default:
                            throw new RuntimeException("Error: a log level of: [" + i + "] was requested, and is not available!");
                    }
                }

                public void log(int i, String s) {
                    switch (i) {
                        case com.jcraft.jsch.Logger.DEBUG:
                            logJSch.debug(s);
                            break;
                        case com.jcraft.jsch.Logger.INFO:
                            logJSch.info(s);
                            break;
                        case com.jcraft.jsch.Logger.WARN:
                            logJSch.warn(s);
                            break;
                        case com.jcraft.jsch.Logger.ERROR:
                            logJSch.error(s);
                            break;
                        case com.jcraft.jsch.Logger.FATAL:
                            logJSch.error(fatal, s);
                            break;
                        default:
                            throw new RuntimeException("Error: can not log on given log level: [" + i + "].  Message was: [" + s + "]");
                    }
                }
            });

            if (hostKey == null) {
                JSch.setConfig("StrictHostKeyChecking", "no");
            } else {
                JSch.setConfig("StrictHostKeyChecking", "yes");
                HostKey hk = null;
                if (hostKeyType == 0) {
                    hk = new HostKey(host, hostKey);
                } else {
                    hk = new HostKey(host, hostKeyType, hostKey);
                }
                HostKeyRepository hkr = jsch.getHostKeyRepository();
                hkr.add(hk, null);
            }

            Session session = jsch.getSession(user, host, port);
            if (password != null) {
                session.setPassword(password);
                JSch.setConfig("PreferredAuthentications", "keyboard-interactive,password");
            } else if (identity != null) {
                jsch.addIdentity(identity, null);
                JSch.setConfig("PreferredAuthentications", "publickey");
            }

            session.connect();

            String command = ConnectorUtil.getSingleValue(attrs, "exec");

            ChannelExec channel = (ChannelExec)session.openChannel("exec");
            channel.setCommand(command);

            channel.setInputStream(null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            channel.setOutputStream(out);

            ByteArrayOutputStream err = new ByteArrayOutputStream();
            channel.setErrStream(err);

            channel.connect();

            int exitStatus;
            while (true) {
                if (channel.isClosed()) {
                    exitStatus = channel.getExitStatus();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore exception
                }
            }

            if (exitStatus != 0) {
                throw new IdMUnitException("Non zero exit status (" + exitStatus +
                        ")\r\n" + new String(err.toByteArray()));
            }

            channel.disconnect();
            session.disconnect();
            if (err.size() != 0) {
                log.debug("stderr: " + new String(err.toByteArray()));
            }
            return new String(out.toByteArray());
        } catch (JSchException e) {
            throw new IdMUnitException("Error executing command.", e);
        } catch (IdMUnitException e) {
            throw new IdMUnitException("Error executing command.", e);
        }
    }
}
