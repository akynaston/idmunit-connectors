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

public class DxcmdLdapConnector extends AbstractConnector {
    static final String RUNNING = "running";
    static final String STARTING = "starting";
    static final String STOPPED = "stopped";
    // static final String DISABLED = "disabled";
    static final String SHUTDOWN_PENDING = "shutdown pending";
    // static final String CONFIG_PORT = "port";

    private LdapContext ldapContext;

    public void setup(Map<String, String> config) throws IdMUnitException {
        this.ldapContext = (LdapContext)LdapConnectionHelper.createLdapConnection(new HashMap<>(config));
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

        /*
         * TODO: this is for migrateObjectFromApp - using this function as the Connection interface does not support 'migrateObject'
         *   Support xml attribute containing one XDS query document that will be applied to application response instances will
         *   be applied to eDirectory as synthetic adds.
         */

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
            throw new IdMUnitException("unexpected error when stopping driver " + driverDn, e);
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
            throw new IdMUnitException("unexpected error when stopping driver " + driverDn, e);
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
            throw new IdMUnitException("unexpected error when stopping driver " + driverDn, e);
        }
    }

    private static File writeXMLData(String xmlDataToWrite) throws IdMUnitException {
        File xmlFileHandle;
        PrintWriter xmlFile;
        try {
            xmlFileHandle = File.createTempFile("tmpfile", ".xml");
            xmlFileHandle.deleteOnExit();
            xmlFile = new PrintWriter(new FileWriter(xmlFileHandle));
            xmlFile.write(xmlDataToWrite);
            xmlFile.close();
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

    /*
     * ----------------------------------------------------------------------------------------------------------------
     *
     * These modifications were added in SVN revision 13126 by krawlings with the note:
     *   "Checking in dxcmd patch from Andrew and Carl with some cleanup."
     *
     * Three hours later in SVN revision 13127 krawlings noted:
     *   "- Reordered some statements in the connector.
     *    - Existing behavior remains unmodified, but new behavior can not be tested due to missing XML files."
     *

    static final String CACHE_EMPTY_FLAG = "CACHE_EMPTY";
    static final String SUCCESS_FLAG = "SUCCESS";
    Map<String, String> eventProcessingDates;

    static Document loadXMLFromFS(String fullPath) throws IdMUnitException {
        Document doc;
        try {
            Builder parser = new Builder();
            doc = parser.build(fullPath);
        } catch (ParsingException e) {
            throw new IdMUnitException("Error parsing configuration.", e);
        } catch (IOException e) {
            throw new IdMUnitException("Error reading configuration.", e);
        }

        return doc;
    }

    public static String getXmlFsName(String driverDn) {
        return driverDn.replaceAll(" ", "_") + ".xml";
    }

    public void opCheckDriverProcessing(Map<String, Collection<String>> fields) throws IdMUnitException, ParseException, NamingException, LDAPException {
        Failures failures = new Failures();

        for (String currentDN : fields.get("dn")) {
            if (currentDN == null) {
                throw new IdMUnitException("'dn' of job not specified.");
            }

            if (!eventProcessingDates.containsKey(currentDN)) {
                eventProcessingDates.put(currentDN, null);
            }
            GetDriverStatsResponse resp = (GetDriverStatsResponse)ldapContext.extendedOperation(new GetDriverStatsRequest(currentDN, 10));

            try {
                validateCacheXml(currentDN, getXmlFsName(currentDN));
            } catch (IdMUnitFailureException e) {
                failures.add(e.getMessage());
            }
        }

        //clear everything
        if (failures.hasFailures()) {
            logger.info(failures.toString());
            throw new IdMUnitFailureException(failures.toString());
        } else { //No Failures, clear the success and fall out with success
            for (String currentDN : fields.get("dn")) {
                eventProcessingDates.put(currentDN, null);
            }
        }
    }

    public void validateCacheXml(String dn, String xmlFile) throws IdMUnitException, ParseException {
        File fileToDelete = new File(xmlFile);
        byte[] driverStatistics = null;
        if (eventProcessingDates.get(dn) != null && eventProcessingDates.get(dn).equalsIgnoreCase(SUCCESS_FLAG)) {
            fileToDelete.delete();
            return;
        }

        try {
            GetDriverStatsResponse cacheResponse = (GetDriverStatsResponse)ldapContext.extendedOperation(new GetDriverStatsRequest(dn, 10));
            driverStatistics = retrieveChunkedData(ldapContext, cacheResponse.getDataHandle(), cacheResponse.getDataSize());
        } catch (LDAPException | NamingException e) {
            e.printStackTrace();
        }

        Document doc = getXmlAsDocument(driverStatistics);

        Node newestTimeStamp;
        Node oldestTimeStamp;

        String eventProcessingDate = eventProcessingDates.get(dn);

        try {
            newestTimeStamp = getXPathResultNode(doc, "/driver-info/subscriber/cache/transactions/newest");
            oldestTimeStamp = getXPathResultNode(doc, "/driver-info/subscriber/cache/transactions/oldest");
            if (newestTimeStamp == null || oldestTimeStamp == null) { //Cache is empty. Toggle the flag
                if (eventProcessingDate == null) {
                    eventProcessingDates.put(dn, CACHE_EMPTY_FLAG);
                    throw new IdMUnitFailureException(dn + ": First Pass. Cache Is Empty");
                } else { //Flag is set or a date is set, Success: set to null
                    eventProcessingDates.put(dn, SUCCESS_FLAG);
                    //Success, fall out without error
                }
            } else if (eventProcessingDate == null) {
                //Set the processing date
                eventProcessingDates.put(dn, newestTimeStamp.getTextContent());
                throw new IdMUnitFailureException(dn + ": First Pass. Cache Not Empty");
            } else { //EVENT_PROCESSING_DATE isn't null
                if (eventProcessingDate.equalsIgnoreCase(CACHE_EMPTY_FLAG)) {
                    eventProcessingDates.put(dn, newestTimeStamp.getTextContent());
                    throw new IdMUnitFailureException(dn + ": Cache Just Set. Event Is Processing");
                } else {
                    //If the date is after the newest - fail
                    if (eventProcessingDate.compareTo(oldestTimeStamp.getTextContent()) < 0) {
                        //Success The event has been processed
                        eventProcessingDates.put(dn, SUCCESS_FLAG);
                    } else if (eventProcessingDate.compareTo(oldestTimeStamp.getTextContent()) >= 0) {
                        //Failure The event has not been processed yet
                        throw new IdMUnitFailureException(dn + ": Cache Processing, Event Still Processing");
                    }
                }
            }

        } finally {
            fileToDelete.delete();
        }
    }
    */
}
