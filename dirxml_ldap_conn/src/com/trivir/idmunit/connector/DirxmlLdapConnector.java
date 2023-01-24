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

import com.novell.ldap.LDAPException;
import com.novell.nds.dirxml.ldap.*;
import com.novell.nds.dirxml.util.DxConst;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.idmunit.util.LdapConnectionHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DirxmlLdapConnector extends AbstractConnector {
    static final String RUNNING = "running";
    static final String STARTING = "starting";
    static final String STOPPED = "stopped";
    // static final String DISABLED = "disabled";
    static final String SHUTDOWN_PENDING = "shutdown pending";
    // static final String CONFIG_PORT = "port";

    private LdapContext ldapContext;

    public void setup(Map<String, String> config) throws IdMUnitException {
        this.ldapContext = LdapConnectionHelper.createLdapConnection(new HashMap<>(config));
    }

    public void tearDown() {
    }

    public void opMigrateApp(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to start not specified.");
        }
        if (ConnectorUtil.getSingleValue(fields, "xmlfile") != null) {
            throw new IdMUnitException("'xmlfile' option is no longer supported. use 'xmlfiledata'");
        }

        String xmlFileData = ConnectorUtil.getSingleValue(fields, "xmlfiledata");
        if (xmlFileData == null) {
            throw new IdMUnitException("'xmlfiledata' must be specified.");
        }

        try {
            ldapContext.extendedOperation(new MigrateAppRequest(driverDn, xmlFileData.getBytes()));
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("unexpected error when migrating the job " + driverDn, e);
        }
    }

    public void opStartJob(Map<String, Collection<String>> fields) throws IdMUnitException {
        String jobDN = ConnectorUtil.getSingleValue(fields, "dn");
        if (jobDN == null) {
            throw new IdMUnitException("'dn' of job not specified.");
        }

        try {
            ldapContext.extendedOperation(new StartJobRequest(jobDN));
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("unexpected error when starting job " + jobDN, e);
        }
    }

    public void opValidateDriverState(Map<String, Collection<String>> fields) throws IdMUnitException {

        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver not specified.");
        }

        String expectStatus = ConnectorUtil.getSingleValue(fields, "expectedStatus");
        if (expectStatus == null) {
            throw new IdMUnitException("No expected status given");
        }

        int intStatus = parseStatusString(expectStatus);
        int state = getDriverState(driverDn);
        if (intStatus != state) {
            throw new IdMUnitFailureException("Driver state is not the same, expected " + expectStatus + " was " + parseStatusInt(state));
        }
    }

    String parseStatusInt(int status) throws IdMUnitException {
        switch (status) {
            case DxConst.VR_DRIVER_RUNNING:
                return RUNNING;
            case DxConst.VR_DRIVER_STARTING:
                return STARTING;
            case DxConst.VR_DRIVER_STOPPED:
                return STOPPED;
            case DxConst.VR_DRIVER_SHUTDOWN_PENDING:
                return SHUTDOWN_PENDING;
            default:
                throw new IdMUnitException("Invalid status: " + status);
        }
    }

    int parseStatusString(String status) throws IdMUnitException {
        if (status.equalsIgnoreCase(RUNNING)) {
            return DxConst.VR_DRIVER_RUNNING;
        } else if (status.equalsIgnoreCase(STARTING)) {
            return DxConst.VR_DRIVER_STARTING;
        } else if (status.equalsIgnoreCase(STOPPED)) {
            return DxConst.VR_DRIVER_STOPPED;
        } else if (status.equalsIgnoreCase(SHUTDOWN_PENDING)) {
            return DxConst.VR_DRIVER_SHUTDOWN_PENDING;
        } else {
            throw new IdMUnitException("Invalid status: " + status);
        }
    }

    public void opStartDriver(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to start not specified.");
        }

        try {
            ldapContext.extendedOperation(new StartDriverRequest(driverDn));
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("unexpected error when starting driver " + driverDn, e);
        }
    }

    public void opStopDriver(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to stop not specified.");
        }

        try {
            ldapContext.extendedOperation(new StopDriverRequest(driverDn));
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("unexpected error when stopping driver " + driverDn, e);
        }
    }

    public void opSetDriverStatusManual(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver not specified.");
        }

        try {
            ldapContext.extendedOperation(new SetDriverStartOptionRequest(driverDn, DxConst.VR_DRIVER_MANUAL_START, false));
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("setting driver status to disabled " + driverDn, e);
        }
    }

    public void opSetDriverStatusAuto(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver not specified.");
        }

        try {
            ldapContext.extendedOperation(new SetDriverStartOptionRequest(driverDn, DxConst.VR_DRIVER_AUTO_START, false));
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("setting driver status to disabled " + driverDn, e);
        }
    }

    public void opSetDriverStatusDisabled(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver not specified.");
        }

        try {
            ldapContext.extendedOperation(new SetDriverStartOptionRequest(driverDn, DxConst.VR_DRIVER_DISABLED, false));
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("setting driver status to disabled " + driverDn, e);
        }
    }

    private static File writeXMLData(String xmlDataToWrite) throws IdMUnitException {
        File xmlFileHandle;
        try {
            xmlFileHandle = File.createTempFile("tmpfile", ".xml");
            xmlFileHandle.deleteOnExit();
            try (PrintWriter xmlFile = new PrintWriter(new FileWriter(xmlFileHandle))) {
                xmlFile.write(xmlDataToWrite);
            }
        } catch (IOException e) {
            throw new IdMUnitException("Could not create temp file, or failed writing data: " + e.getMessage(), e);
        }
        return xmlFileHandle;
    }

    int getDriverState(String driverDn) throws IdMUnitException {
        try {
            GetDriverStateResponse response = (GetDriverStateResponse)ldapContext.extendedOperation(new GetDriverStateRequest(driverDn));
            return response.getDriverState();
        } catch (NamingException | LDAPException e) {
            throw new IdMUnitException("Could not read driver state", e);
        }
    }

    boolean isDriverRunning(String driverDn) throws IdMUnitException {
        return getDriverState(driverDn) == DxConst.VR_DRIVER_RUNNING;
    }

    boolean isDriverStopped(String driverDn) throws IdMUnitException {
        return getDriverState(driverDn) == DxConst.VR_DRIVER_STOPPED;
    }

    int getDriverStartOption(String driverDn) throws Exception {
        GetDriverStartOptionResponse response = (GetDriverStartOptionResponse)ldapContext.extendedOperation(new GetDriverStartOptionRequest(driverDn));
        return response.getDriverStartOption();

    }

    int getJobState(String jobDn) throws Exception {
        GetJobStateResponse response = (GetJobStateResponse)ldapContext.extendedOperation(new GetJobStateRequest(jobDn));
        return response.getRunningState();
    }

    private byte[] retrieveChunkedData(LdapContext ctx, int dataHandle, int dataSize) throws IdMUnitException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int remainingBytes = dataSize;
            int chunkSize = Math.min(remainingBytes, 64512);

            readLoop:
            while (true) {
                if (remainingBytes > 0) {
                    byte[] replyData = ((GetChunkedResultResponse)ctx.extendedOperation(new GetChunkedResultRequest(dataHandle, chunkSize, 0))).getData();
                    remainingBytes -= replyData.length;
                    int outSize = 8192;
                    int i = 0;

                    while (true) {
                        if (i >= replyData.length) {
                            continue readLoop;
                        }

                        if (replyData.length - i < outSize) {
                            outSize = replyData.length - i;
                        }

                        bos.write(replyData, i, outSize);
                        i += outSize;
                    }
                }
                ctx.extendedOperation(new CloseChunkedResultRequest(dataHandle));
                break;
            }
            bos.close();
            return bos.toByteArray();
        } catch (NamingException | LDAPException | IOException e) {
            throw new IdMUnitException(e);
        }
    }

    private Document getXmlAsDocument(byte[] xml) throws IdMUnitException {
        String newXml = new String(xml);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(newXml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IdMUnitException(e);
        }
    }

    private Node getXPathResultNode(Document document, String xpath) throws IdMUnitException {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPath = xpf.newXPath();
        XPathExpression expr;
        try {
            expr = xPath.compile(xpath);
            return (Node)expr.evaluate(document, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new IdMUnitException(e);
        }
    }
}
