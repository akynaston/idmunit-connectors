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

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.ParsingException;
import org.idmunit.Failures;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DxcmdLdapConnector extends AbstractConnector {
    static final String CONFIG_PORT = "port";
    static final String CACHE_EMPTY_FLAG = "CACHE_EMPTY";
    static final String SUCCESS_FLAG = "SUCCESS";

    private static Logger log = LoggerFactory.getLogger(DxcmdLdapConnector.class);
    Map<String, String> eventProcessingDates;
    private String[] commonArgs;

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

    private static File writeXMLData(String xmlDataToWrite) throws IdMUnitException {
        File xmlFileHandle = null;
        PrintWriter xmlFile = null;
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

    public void setup(Map<String, String> config) throws IdMUnitException {
        eventProcessingDates = new HashMap<String, String>();
        String host = config.get(BasicConnector.CONFIG_SERVER);
        String user = config.get(BasicConnector.CONFIG_USER);
        String pass = config.get(BasicConnector.CONFIG_PASSWORD);
        String port = config.get(CONFIG_PORT);
        if (port == null) {
            commonArgs = new String[] {
                "-q",
                "-host", host,
                "-user", user,
                "-password", pass, };
        } else {
            commonArgs = new String[] {
                "-q",
                "-host", host,
                "-port", port,
                "-user", user,
                "-password", pass, };
        }
    }

    public void tearDown() throws IdMUnitException {
    }

    public void opMigrateApp(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDN = ConnectorUtil.getSingleValue(fields, "driverdn");
        if (driverDN == null) {
            driverDN = ConnectorUtil.getSingleValue(fields, "dn");
            if (driverDN == null) {
                throw new IdMUnitException("The driver dot format of the driver to process the command must be specified 'driverdn' or 'dn' must be specified");
            }
        }

        /**
         * TODO: this is for migrateObjectFromApp - using this function as the Connection interface does not support 'migrateObject'
         * Support xml attribute containing one XDS query document that will be applied to application response instances will
         *  be applied to eDirectory as synthetic adds.
         */
        String xmlFile = ConnectorUtil.getSingleValue(fields, "xmlFile");
        String xmlFileData = ConnectorUtil.getSingleValue(fields, "xmlFileData");

        if (xmlFile == null && xmlFileData == null) {
            throw new IdMUnitException("Either 'xmlFile' or 'xmlFileData' must be specified.");
        }

        if (xmlFile != null && xmlFileData != null) {
            throw new IdMUnitException("Specify either 'xmlFile' or 'xmlFileData', do not use both.");
        }

        if (xmlFileData != null) {
            xmlFile = writeXMLData(xmlFileData).getAbsolutePath();
        }

        if (!new File(xmlFile).exists()) {
            throw new IdMUnitException("'xmlFile' specifed: '" + xmlFile + "' does not exist!");
        }

        String[] args = new String[] {"-migrateapp", driverDN, xmlFile};

        executeCommand(args);
    }

    public void opStartJob(Map<String, Collection<String>> fields) throws IdMUnitException {
        String jobDN = ConnectorUtil.getSingleValue(fields, "dn");
        if (jobDN == null) {
            throw new IdMUnitException("'dn' of job not specified.");
        }

        executeCommand(new String[] {"-startjob", jobDN});
    }

    public void opStartDriver(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to start not specified.");
        }

        executeCommand(new String[] {"-start", driverDn});
    }

    public void opStopDriver(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to start not specified.");
        }

        executeCommand(new String[] {"-stop", driverDn});
    }

    public void opSetDriverStatusManual(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to start not specified.");
        }

        String[] args = new String[] {
            "-setstartoption", driverDn,
            "manual",
            "noresync", };

        executeCommand(args);
    }

    public void opSetDriverStatusAuto(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to start not specified.");
        }

        String[] args = new String[] {
            "-setstartoption", driverDn,
            "auto",
            "noresync", };

        executeCommand(args);
    }

    public void opSetDriverStatusDisabled(Map<String, Collection<String>> fields) throws IdMUnitException {
        String driverDn = ConnectorUtil.getSingleValue(fields, "dn");
        if (driverDn == null) {
            throw new IdMUnitException("'dn' of driver to start not specified.");
        }

        String[] args = new String[] {
            "-setstartoption", driverDn,
            "disabled",
            "noresync", };

        executeCommand(args);
    }

    private void executeCommand(String[] commandArgs) throws IdMUnitException {
        Class<?> clssDxcmd;
        try {
            clssDxcmd = Class.forName("com.novell.nds.dirxml.util.DxCommand");
        } catch (ClassNotFoundException cnfe) {
            throw new IdMUnitException("Please ensure the following libraries are included in the project: dirxml.jar, dirxml_misc.jar, jclient.jar, nxsl.jar, xp.jar - This action requires access to com.novell.nds.dirxml.util.DxCommand", cnfe);
        }

        Method meth;
        try {
            meth = clssDxcmd.getMethod("commandLine", new Class[] {String[].class});
        } catch (SecurityException e) {
            throw new IdMUnitException("Error while attempting to call 'commandLine' in DXCMD", e);
        } catch (NoSuchMethodException e) {
            throw new IdMUnitException("Error while attempting to call 'commandLine' in DXCMD", e);
        }

        // setup arguments:
        String[] args = new String[commonArgs.length + commandArgs.length];
        System.arraycopy(commonArgs, 0, args, 0, commonArgs.length);
        System.arraycopy(commandArgs, 0, args, commonArgs.length, commandArgs.length);
        Object[] argsToUse = new Object[] {args};
        int ret = -1;
        try {
            PrintStream console = System.err;
            ByteArrayOutputStream systemErrorReDirectionStream = new ByteArrayOutputStream();
            System.setErr(new PrintStream(systemErrorReDirectionStream));
            try {

                ret = ((Integer)meth.invoke(clssDxcmd, argsToUse)).intValue();
            } catch (IllegalAccessException e) {
                // could be a IllegalAccessException, IllegalArgumentException, or InvocationTargetException.  In each case, lets throw it wrapped in our exception:
                throw new IdMUnitException("Failed while attempting to call a method in DXCMD.", e);
            } catch (IllegalArgumentException e) {
                // could be a IllegalAccessException, IllegalArgumentException, or InvocationTargetException.  In each case, lets throw it wrapped in our exception:
                throw new IdMUnitException("Failed while attempting to call a method in DXCMD.", e);
            } catch (InvocationTargetException e) {
                // could be a IllegalAccessException, IllegalArgumentException, or InvocationTargetException.  In each case, lets throw it wrapped in our exception:
                throw new IdMUnitException("Failed while attempting to call a method in DXCMD.", e);
            } finally {
                System.setErr(console);
            }

            if (ret != 0) {
                throw new IdMUnitException("DXCMD returned: [" + ret + "], error: [" + systemErrorReDirectionStream.toString() + "]");
            }
        } catch (RuntimeException e) {
            throw new IdMUnitException("Error while attempting to invoke 'commandLine'", e);
        }
    }

    public void opCheckDriverProcessing(Map<String, Collection<String>> fields) throws IdMUnitException, ParseException {
        Failures failures = new Failures();

        for (String currentDN : fields.get("dn")) {
            if (currentDN == null) {
                throw new IdMUnitException("'dn' of job not specified.");
            }

            if (!eventProcessingDates.containsKey(currentDN)) {
                eventProcessingDates.put(currentDN, null);
            }

            String[] args = new String[] {
                "-getdriverstats",
                currentDN,
                getXmlFsName(currentDN), };
            executeCommand(args);

            try {
                validateCacheXml(currentDN, getXmlFsName(currentDN));
            } catch (IdMUnitFailureException e) {
                failures.add(e.getMessage());
            }
        }

        //clear everything
        if (failures.hasFailures()) {
            log.info(failures.toString());
            throw new IdMUnitFailureException(failures.toString());
        } else { //No Failures, clear the success and fall out with success
            for (String currentDN : fields.get("dn")) {
                eventProcessingDates.put(currentDN, null);
            }
        }
    }

    public void validateCacheXml(String dn, String xmlFile) throws IdMUnitException, ParseException {
        File fileToDelete = new File(xmlFile);
        if (eventProcessingDates.get(dn) != null && eventProcessingDates.get(dn).equalsIgnoreCase(SUCCESS_FLAG)) {
            fileToDelete.delete();
            return;
        }

        Document doc = loadXMLFromFS(xmlFile);
        Node newestTimeStamp = null;
        Node oldestTimeStamp = null;

        String eventProcessingDate = eventProcessingDates.get(dn);

        try {
            newestTimeStamp = doc.query("/driver-info/subscriber/cache/transactions/newest").get(0);
            oldestTimeStamp = doc.query("/driver-info/subscriber/cache/transactions/oldest").get(0);
            if (eventProcessingDate == null) {
                //Set the processing date
                eventProcessingDates.put(dn, newestTimeStamp.getValue());
                throw new IdMUnitFailureException(dn + ": First Pass. Cache Not Empty");
            } else { //EVENT_PROCESSING_DATE isn't null
                if (eventProcessingDate.equalsIgnoreCase(CACHE_EMPTY_FLAG)) {
                    eventProcessingDates.put(dn, newestTimeStamp.getValue());
                    throw new IdMUnitFailureException(dn + ": Cache Just Set. Event Is Processing");
                } else {
                    //If the date is after the newest - fail
                    if (eventProcessingDate.compareTo(oldestTimeStamp.getValue()) < 0) {
                        //Success The event has been processed
                        eventProcessingDates.put(dn, SUCCESS_FLAG);
                    } else if (eventProcessingDate.compareTo(oldestTimeStamp.getValue()) >= 0) {
                        //Failure The event has not been processed yet
                        throw new IdMUnitFailureException(dn + ": Cache Processing, Event Still Processing");
                    }
                }
            }

        } catch (IndexOutOfBoundsException e) { //Cache is empty. Toggle the flag
            if (eventProcessingDate == null) {
                eventProcessingDates.put(dn, CACHE_EMPTY_FLAG);
                throw new IdMUnitFailureException(dn + ": First Pass. Cache Is Empty");
            } else { //Flag is set or a date is set, Success: set to null
                eventProcessingDates.put(dn, SUCCESS_FLAG);
                //Success, fall out without error
            }
        } finally {
            fileToDelete.delete();
        }
    }
}
