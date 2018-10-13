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

import com.trivir.idmunit.extension.WinSCPLib;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implements an IdMUnit connector for SCPConnector that simulates delimited text file generation and can push the file to an SCP interface for use by the DTF IDM Driver
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connector
 * <p>
 * Sample Config:
 * <connection>
 * <name>SCP</name>
 * <description>Connector to generate DTF data feed and push to an SCP interface on a UNIX server</description>
 * <type>com.trivir.idmunit.connector.SCPConnector</type>
 * <!-- Delimited text generation section -->
 * <write-path>/vm/trivirvm/VMs/CensusVMs</write-path>
 * <delimiter>,</delimiter>
 * <local-cache-path>C:\idmunit\dtfdata\</local-cache-path>
 * <dtf-data-file-extension>csv</dtf-data-file-extension>
 * <!-- SCP Interface Configuration Section -->
 * <enable-scp>true</enable-scp>
 * <win-scp-exe-path>C:\Program Files\WinSCP3\WinSCP3.exe</win-scp-exe-path>
 * <scp-profile>trivirvm</scp-profile>
 * <user/>
 * <password/>
 * <keystore-path/>
 * <multiplier>
 * <retry>0</retry>
 * <wait>0</wait>
 * </multiplier>
 * <substitutions>
 * <substitution>
 * <replace/>
 * <new/>
 * </substitution>
 * </substitutions>
 * </connection>
 */
public class SCPConnector extends AbstractConnector {
    protected static final String WRITE_PATH = "write-path"; //Full path to directory to write files
    protected static final String DELIM = "delimiter"; //Token delimiter for the DTF generated feed
    protected static final String LOCAL_CACHE = "local-cache-path"; //Path where generated DTF files are cached before being sent to an external SCPConnector server (if necessary) - should end with a trailing slash
    protected static final String ENABLE_SCP = "enable-scp"; //Enable/disable the push of the generated DTF file to a UNIX server over WinSCP
    protected static final String WIN_SCP_EXE_PATH = "win-scp-exe-path"; //Path to the WinSCP executable file (should be quoted if spaces reside in the name ex: \"C:\\Program Files\\WinSCP\\WinSCP.exe \"
    protected static final String SCP_PROFILE = "scp-profile"; //Name of the configured WinSCP profile to contain a target IP, port, user name and cached password
    protected static final String DTF_FILE_EXTENSION = "dtf-data-file-extension"; //Used to give a file type to the generated data file
    private static final int DTF_BUFFER = 1000; //pre-allocate up to this many bytes for the output to insert into the delimited text file
    private static Logger log = LoggerFactory.getLogger(SCPConnector.class);

    private String targetFile;
    private String delimiter;
    private boolean scpEnabled;
    private String scpPath;
    private String scpProfileName;
    private String localCachePath;
    private String fileExtension;

    public void setup(Map<String, String> config) throws IdMUnitException {
        delimiter = config.get(DELIM);
        scpPath = config.get(WIN_SCP_EXE_PATH);
        scpProfileName = config.get(SCP_PROFILE);
        targetFile = config.get(WRITE_PATH);
        fileExtension = config.get(DTF_FILE_EXTENSION);

        if (config.get(LOCAL_CACHE) == null) {
            throw new IdMUnitException("'" + LOCAL_CACHE + "' not configured");
        }
        localCachePath = config.get(LOCAL_CACHE);
        File tmp = new File(localCachePath);
        if (!tmp.exists()) {
            throw new IdMUnitException("'" + LOCAL_CACHE + "' (" + config.get(LOCAL_CACHE) + ") does not exist");
        }

        if (delimiter == null) {
            throw new IdMUnitException("'" + DELIM + "' not configured");
        }

        if (config.get(ENABLE_SCP) == null) {
            throw new IdMUnitException("'" + ENABLE_SCP + "' not configured");
        } else {
            scpEnabled = (config.get(ENABLE_SCP).equalsIgnoreCase("true")) ? true : false;
        }


        if (scpPath == null) {
            throw new IdMUnitException("'" + WIN_SCP_EXE_PATH + "' not configured");
        }

        if (scpProfileName == null) {
            throw new IdMUnitException("'" + SCP_PROFILE + "' not configured");
        }

        if (targetFile == null) {
            throw new IdMUnitException("'" + WRITE_PATH + "' not configured");
        }

        if (fileExtension == null) {
            throw new IdMUnitException("'" + DTF_FILE_EXTENSION + "' not configured");
        }
    }

    public void tearDown() throws IdMUnitException {
        //TODO: Create routine to clean up local generated files and clean from the remote server if SCP is enabled
    }

    public void opValidateObject(Map<String, Collection<String>> data) throws IdMUnitException {
        //No business requirements to implement this method yet
        log.info("### SCPConnector Validation is Not Yet Implemented. ###");
    }

    /**
     * This method generates an industry standard Output IDoc ready for consumption by Novell Identity Manager.  The formatting may be tweaked as necessary.
     *
     * @return List<String> The generated IDoc name
     */
    private String composeDTFFileName() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date timestamp = new Date();
        String currentTimeStamp = dateFormatter.format(timestamp);
        return currentTimeStamp + "." + fileExtension;
    }

    /**
     * This method generates the file data to DTF format to be consumed by Novell Identity Manager.  The formatting may be tweaked as necessary.
     *
     * @return String file data
     */
    private String buildFileData(Map<String, Collection<String>> data) {
        Set<String> keySet = data.keySet();
        StringBuffer fileData = new StringBuffer(DTF_BUFFER);
        for (Iterator<String> attributeNameIterator = keySet.iterator(); attributeNameIterator.hasNext(); ) {
            String attrName = (String)attributeNameIterator.next();
            Collection<String> attrVal = (Collection<String>)data.get(attrName);
            if (attrName == null || attrVal == null) {
                continue;
            }
            //Append the data to the data entry here
            for (Iterator<String> attributeValueIterator = attrVal.iterator(); attributeValueIterator.hasNext(); ) {
                fileData.append(attributeValueIterator.next().trim());
            }
            fileData.append(delimiter);
        }
        return fileData.toString();
    }


    /**
     * This is the implementation of opAddObject.  This was abstracted out in order to aid testing by returning the generated DTF docs for analysis.  At the time of this writing,
     * IdMUnit does not expect a return value during a standard operation, thus the public version of this method with a return of void.
     * <p>
     * Note: this connector relies on the data collection being a order sensitive collection; it currently appears to use a linked hash map.
     *
     * @param Map<String, Collection<String>> data The data map of instructions and data values from the IdMUnit spreadsheet.
     * @return String The generated DTF data
     * @throws IdMUnitException
     */
    public String opAddObjectIMPL(Map<String, Collection<String>> data) throws IdMUnitException {

        String fileData = buildFileData(data);
        log.info("...writing delimited text file entry: ");
        log.info(fileData);
        BufferedWriter outputFile = null;
        String fileName = this.composeDTFFileName();
        String fullFileNameAndPath = localCachePath + fileName;
        try {
            //Write the data entry to the DTF file (note that once this file has a single row it may be picked up by IDM and processed)
            outputFile = new BufferedWriter(new FileWriter(fullFileNameAndPath, false));
            outputFile.write(fileData);
            outputFile.newLine();
            outputFile.flush();
            outputFile.close();
        } catch (IOException e) {
            throw new IdMUnitException("...Failed to write to the log file: " + fullFileNameAndPath + " Error: " + e.getMessage());
        }
        log.info("...successfully cached DTF file data to [" + fileName + "].");

        if (scpEnabled) {
            log.info("...pushing to SCP interface [" + scpProfileName + "].");
            //SCPConnector the file to the target IDM server
            WinSCPLib.scpSendFile(scpPath, scpProfileName, localCachePath + "winscpscript.txt", fullFileNameAndPath, targetFile + "/", fileName);
        }

        return fileData;
    }


    /**
     * Generates DTF feed data based on a test-spreadsheet data.
     *
     * @param Map<String, Collection<String>> data The data map of instructions and data values from the IdMUnit spreadsheet
     * @throws IdMUnitException
     */
    public void opAddObject(Map<String, Collection<String>> data) throws IdMUnitException {
        opAddObjectIMPL(data);
    }
}
