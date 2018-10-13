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

import com.trivir.idmunit.extension.SCPUtil;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implements an IdMUnit connector for SAP that simulates iDoc format transactions originating from SAP to the SAP IDM Driver
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connector
 */
public class SAP extends AbstractConnector {
    protected static final String ENABLE_SCP = "enable-scp"; //Enable/disable the push of the generated IDoc to a UNIX server over WinSCP
    protected static final String SCP_PROFILE = "scp-profile"; //Name of the configured WinSCP profile
    protected static final String WIN_SCP_EXE_PATH = "win-scp-exe-path"; //Path to the WinSCP executable file (should be quoted if spaces reside in the name ex: \"C:\\Program Files\\WinSCP\\WinSCP.exe \"
    protected static final String SAP_CLIENT_NUMBER = "sap-client-number"; //Used in IDoc file name - Driver will ignore if not a match with the driver option: "SAP User Client Number"
    protected static final String INITIAL_IDOC_DATA_OFFSET = "initial-idoc-data-offset"; //(default: 64): This is the distance between the left column of the IDoc and the actual start of data
    protected static final String IDOC_LOCAL_PATH = "idoc-local-path"; //Path where IDocs are cached before being sent to an external SAP server (if necessary)
    protected static final String IDOC_FILENAME_PREFIX = "idoc-transaction-prefix"; //Static value to prepend onto the file name of the IDoc, typically 0000000000
    protected static final String IDOC_FILE_EXTENSION = "idoc-file-extension"; //Used to easily identify and clean up IdMUnit-generated IDocs
    protected static final String SCP_SCRIPT_PATH = "scp-script-path"; //The path where the dynamic WinSCP script will be written for execution by the WinSCP client when pushing IDocs to the UNIX server
    protected static final String SCP_SERVER_IDOC_PATH = "scp-server-idoc-path"; //The destination path for IDocs on the server
    protected static final String WORKING_DIRECTORY = System.getProperty("user.dir"); //The current working directory (used to suppor the relative access of iDocCache files)

    private static final String STR_IDOC_TEMPLATE_PATH = "IDocTemplate";
    private static final String STR_DN = "dn";
    private static final String STR_DELIM = "=";
    private static final String STR_KEY = "key";

    private static Logger log = LoggerFactory.getLogger(SAP.class);

    private boolean scpEnabled;
    private String scpProfileName;
    private String sapClientNumber;
    private int initialIdocDataOffset = 64; //default
    private String idocLocalFilePath;
    private String idocFilenamePrefix;
    private String idocFileExtension;
    private String scpPath;
    private String scpScriptPath;
    private String scpServerIdocPath;

    /**
     * This method provides a mechanism to pad IDoc fields so that when inserted into the IDoc the existing contents will be entirely overwritten
     * Note: Some of these helper methods are friendly to the class so they can be directly tested from test/org.idmunit.connector.SAPTests
     *
     * @param List<String> templateFileData read from an IDoc template and now mutating to become the test IDoc
     * @param String       attributeName The attribute identification and offset information required to replace the template values
     */
    static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    /**
     * This method stores the IDoc to a local path on the IdMUnit machine (this may also be the IDM machine possibly)
     * If the IdMUnit machine is not the IDM server where the SAP HR driver is running, SCP should be used to push the new IDoc
     * to it's final location
     *
     * @param file is a file path which can be written to.
     * @throws IdMUnitException if the write fails
     */
    public static void writeFile(String file, List<String> fileData) throws IdMUnitException {
        File fullPathAndFileName = new File(file);

        BufferedWriter outputFile = null;
        try {
            if (file == null || file.length() < 1) {
                throw new IdMUnitException("Failure to write the IDoc to the following path [No path configured] Please see the " + IDOC_LOCAL_PATH + " setting in idmunit-config.xml");
            }
            outputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
            for (String fileDataRow : fileData) {
                outputFile.write(fileDataRow);
                outputFile.newLine();
            }
            outputFile.flush();
            outputFile.close();
        } catch (IOException e) {
            throw new IdMUnitException("Failure to write the IDoc to [" + fullPathAndFileName + "] Please see the " + IDOC_LOCAL_PATH + " setting in idmunit-config.xml.  Error: " + e.getMessage());
        } finally {
            if (outputFile != null) {
                try {
                    outputFile.close();
                } catch (IOException ioe) {
                    throw new IdMUnitException("Failure to close IDoc file handle on [" + fullPathAndFileName + "] Please see the " + IDOC_LOCAL_PATH + " setting in idmunit-config.xml.  Error: " + ioe.getMessage());
                }
            }
        }
    }

    public String getSAPUserClientNumber() {
        return sapClientNumber;
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        scpEnabled = (config.get(ENABLE_SCP).equalsIgnoreCase("true")) ? true : false;
        scpProfileName = config.get(SCP_PROFILE);
        scpPath = config.get(WIN_SCP_EXE_PATH);
        scpScriptPath = config.get(SCP_SCRIPT_PATH);
        scpServerIdocPath = config.get(SCP_SERVER_IDOC_PATH);
        sapClientNumber = config.get(SAP_CLIENT_NUMBER);

        initialIdocDataOffset = Integer.parseInt(config.get(INITIAL_IDOC_DATA_OFFSET));
        idocLocalFilePath = config.get(IDOC_LOCAL_PATH);
        idocFilenamePrefix = config.get(IDOC_FILENAME_PREFIX);
        idocFileExtension = config.get(IDOC_FILE_EXTENSION);


        log.info("### SAP Connector Initialization: Using SAP Client Number [" + sapClientNumber + "] - This should match the SAP User Client Number in the SAP HR driver - Java working path [" + WORKING_DIRECTORY + "] ###");
    }

    public void tearDown() throws IdMUnitException {
        //TODO: Create routine to clean up local generated IDocs and IDocs pushed to a remote server
    }

    public void opValidateObject(Map<String, Collection<String>> data) throws IdMUnitException {
        //No business requirements to implement this method yet
        log.info("### SAP Validation is Not Yet Implemented. ###");
    }

    /**
     * Loads data from an IDoc template, which is a real IDoc generated for a particular transaction in SAP.  These
     * templates are used to compile synthesized IDocs with test data substituted within.
     * Note: Some of these helper methods are friendly to the class so they can be directly tested from test/org.idmunit.connector.SAPTests
     *
     * @param String fileName The name of the file to load and cache in memory
     * @return List<String> The data cached from the template file read from the disk
     * @throws IdMUnitException
     */
    List<String> loadFileData(String fileName) throws IdMUnitException {
        if ((fileName == null) || ("".equals(fileName))) {
            throw new IdMUnitException("Failed to load the specified SAP template file [" + fileName + "]");
        }

        List<String> fileDataList = new LinkedList<String>();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));
            if (!bufferedReader.ready()) {
                throw new IOException("Input stream was not ready");
            }

            String fileRowData;
            while ((fileRowData = bufferedReader.readLine()) != null) {
                fileDataList.add(fileRowData);
            }
            if (fileDataList.size() < 1) {
                throw new IdMUnitException("Failed to read any data from the SAP template file [" + fileName + "]");
            }
        } catch (IOException e) {
            throw new IdMUnitException("Failed to process the specified SAP template file [" + fileName + "] with error message [" + e.getMessage() + "]", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                throw new IdMUnitException("Failed to close the SAP template file [" + fileName + "] with error message [" + e.getMessage() + "]", e);
            }
        }

        return fileDataList;
    }

    /**
     * Replaces the a key in a template file with an actual test value (to be used for DirXML Association purposes in the SAP driver)
     * Note: Some of these helper methods are friendly to the class so they can be directly tested from test/org.idmunit.connector.SAPTests
     * Additional Note: This method is called recursively to resolve additional templatized keys to the correct key values for the test objects.  This
     * is to facilitate the replacement of multiple keys in a single IDOC row (previously only the key in the dn column was transformed.  Now the DN column and any
     * number of key1, key2... columns will also be transformed.
     *
     * @param List<String> templateFileData read from an IDoc template
     * @param String       dn The key to replace and the new unique value (guid) - for example: EMPLOYID=12345678
     */
    void replaceAssociationKeyWithObjectGUID(List<String> templateFileData, String key, Map<String, Collection<String>> data) throws IdMUnitException {
        dumpList(templateFileData);
        ListIterator<String> dataIterator = templateFileData.listIterator();
        String dn = ConnectorUtil.getSingleValue(data, key);
        if (dn == null || dn.length() < 1) {
            return;
        }
        String searchKey = dn.substring(0, dn.indexOf(STR_DELIM));
        if (searchKey == null || searchKey.length() < 1) {
            return;
        }
        String replaceValue = dn.substring(dn.indexOf(STR_DELIM) + 1);
        log.info("... replacing template association key [" + searchKey + "] with value [" + replaceValue + "]");
        while (dataIterator.hasNext()) {
            String currentRow = dataIterator.next();
            dataIterator.set(currentRow.replaceAll(searchKey, replaceValue));
        }
        dumpList(templateFileData);

        //Recursively call this method in order to apply any additional key transformations that might exist (as necessary)
        //  These additional keys will be specified as columns named key1, key2, key3, and so forth.
        String newKey = "";
        if (!key.startsWith(STR_KEY)) {
            newKey = STR_KEY + "1";
        } else {
            int keyNumber = Integer.parseInt(key.substring(3));
            newKey = STR_KEY + (keyNumber + 1);
        }
        replaceAssociationKeyWithObjectGUID(templateFileData, newKey, data);
    }

    /**
     * Displays list data out to the log
     *
     * @param dataList The list data to dump
     */
    private void dumpList(List<String> dataList) {
        for (String data : dataList) {
            log.info("### File data: " + data); //TODO: Change this to log.debug once I can figure out how to configure my apache logging to a level of debug!
        }
    }

    /**
     * This method performs a search and replace within the IDoc data on fields located by a data offset, following standard IDoc protocol.
     * Note: Some of these helper methods are friendly to the class so they can be directly tested from test/org.idmunit.connector.SAPTests
     *
     * @param List<String> templateFileData read from an IDoc template and now mutating to become the test IDoc
     * @param String       attributeName The attribute identification and offset information required to replace the template values
     */
    void replaceAttrValueByOffSet(List<String> templateFileData, String replaceKey, String replaceValue) throws IdMUnitException {
        dumpList(templateFileData);
        StringTokenizer tokenizer = new StringTokenizer(replaceKey, ":");
        if (tokenizer.countTokens() < 5) {
            throw new IdMUnitException("Failed to parse data column [" + replaceKey + "].  Should be in the format: InfoType:FieldName:Subtype:Offset:Length (ex: P0002:VORNA:none:134:25)");
        }
        String infoType = tokenizer.nextToken();
        tokenizer.nextToken(); // String fieldName
        tokenizer.nextToken(); // String subType
        String dataOffset = tokenizer.nextToken();
        int fieldDataOffset = initialIdocDataOffset + Integer.parseInt(dataOffset);
        int fieldDataLength = Integer.parseInt(tokenizer.nextToken());
        replaceValue = padRight(replaceValue, fieldDataLength);
        if (replaceValue.length() > fieldDataLength) {
            throw new IdMUnitException("Field [" + replaceKey + "] replacement data: [" + replaceValue + "] is too long, it can only be [" + fieldDataLength + "] characters long.");
        }
        ListIterator<String> dataIterator = templateFileData.listIterator();
        log.info("... replacing template data attribute identifiers[" + replaceKey + "] with value [" + replaceValue + "]");
        while (dataIterator.hasNext()) {
            StringBuffer currentRowBuffer = new StringBuffer(dataIterator.next());
            if (currentRowBuffer.indexOf(infoType) != -1) {
                currentRowBuffer.replace(fieldDataOffset - 1, fieldDataOffset + fieldDataLength - 1, replaceValue);
                dataIterator.set(currentRowBuffer.toString());
            }
        }
        dumpList(templateFileData);

    }

    /**
     * This method generates an industry standard Output IDoc ready for consumption by Novell Identity Manager.  The formatting may be tweaked as necessary.
     *
     * @return List<String> The generated IDoc name
     */
    String composeIdocFileName() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date timestamp = new Date();
        String currentTimeStamp = dateFormatter.format(timestamp);
        return idocLocalFilePath + System.getProperty("file.separator") + "O_" + this.getSAPUserClientNumber() + "_" + idocFilenamePrefix + currentTimeStamp + ((idocFileExtension != null && idocFileExtension.length() > 0) ? ("." + idocFileExtension) : "");
    }

    /**
     * This is the implementation of opCreateIdoc.  This was abstracted out in order to aid testing by returning the generated IDoc for analysis.  At the time of this writing,
     * IdMUnit does not expect a return value during a standard operation, thus the public version of this method with a return of void.
     *
     * @param Map<String, Collection<String>> data The data map of instructions and data values from the IdMUnit spreadsheet
     * @return List<String> The generated IDoc data
     * @throws IdMUnitException
     */
    List<String> opCreateIdocIMPL(Map<String, Collection<String>> data) throws IdMUnitException {
        String idocTemplatePath = ConnectorUtil.getSingleValue(data, STR_IDOC_TEMPLATE_PATH);
        String dn = ConnectorUtil.getSingleValue(data, STR_DN);
        if (idocTemplatePath == null) {
            // TODO: should be a test error, not a failure . .
            throw new IdMUnitException("Please include your full IDoc template path in a column titled [" + STR_IDOC_TEMPLATE_PATH + "] to generate SAP transactions.");
        } else if (dn == null) {
            // TODO: should be a test error, not a failure . .
            throw new IdMUnitException("Please include your template key and unique SAP ID in a column titled [" + STR_DN + "] in order to process the SAP IDoc template called [" + STR_IDOC_TEMPLATE_PATH + "]. For example: EMPLOYID=12345678");
        }

        //Read in the IDoc template
        List<String> templateFileData = loadFileData(idocTemplatePath);
        /*Search for key specified in DN and replace it with the specified value throughout the entire IDoc template
         * ex: DN: EMPLOYID=12345678 (this will replace the template's EMPLOYID key with the test object value 12345678.)*/
        replaceAssociationKeyWithObjectGUID(templateFileData, "dn", data);
        //- For each attribute column in the spreadsheet parse as follows: (example: P0002:VORNA:none:134:25 with data of "Happy")
        for (String attrName : data.keySet()) {
            if ((!(attrName.equalsIgnoreCase(STR_IDOC_TEMPLATE_PATH))) && (!(attrName.equalsIgnoreCase(STR_DN))) && ((!(attrName.startsWith(STR_KEY) && (attrName.length() > 3))))) {
                String attrVal = ConnectorUtil.getSingleValue(data, attrName);
                log.info("##### attrName [" + attrName + "] and attrVal [" + attrVal + "] ###");
                //--Find all rows with the current info-type (ex: P0002)
                replaceAttrValueByOffSet(templateFileData, attrName, attrVal);
            }
        }

        String idocName = composeIdocFileName();
        log.info("...generating IDoc: " + idocName);
        log.info("...writing IDoc to local directory....");
        writeFile(idocName, templateFileData);
        log.info("...successful.");

        File iDocFile = new File(idocName);
        String fileRDN = iDocFile.getName();

        if (scpEnabled) {
            //SCP the file to the target IDM server
            //TODO: Abstract these constants into the idmunit-config.xml interface
            SCPUtil.scpSendFile(scpPath, scpProfileName, scpScriptPath, idocName, scpServerIdocPath + "/", fileRDN);

        }
        return templateFileData;
    }


    /**
     * Simulates data and transaction flow from SAP to the identity vault by generating IDocs based
     * on a specified template with data from an IdMUnit spreadsheet
     *
     * @param Map<String, Collection<String>> data The data map of instructions and data values from the IdMUnit spreadsheet
     * @throws IdMUnitException
     */
    public void opCreateIdoc(Map<String, Collection<String>> data) throws IdMUnitException {
        @SuppressWarnings("unused")
        List<String> generatedIDoc;
        generatedIDoc = opCreateIdocIMPL(data);
    }
}
