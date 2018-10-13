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


import org.idmunit.Failures;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import java.io.*;
import java.util.*;


/**
 * Implements an IdMUnit connector for Delimited Text File drivers
 * <p>
 * <connection>
 * <name>DTF</name>
 * <description>Connector to output of DTF driver</description>
 * <type>com.trivir.idmunit.connector.DTFConnector</type>
 * <server>writePath=/var/opt/novell/IdM/files|readPath=/var/opt/novell/IdM/files|Delimiter=,|FieldNames=USER ID, Name, FirstName, LastName, Group, Role</server>
 * <p>
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
public class DTFConnector extends BasicConnector {
    protected static final String FLAT_FILE_CONTENTS = "FileContents";
    private static final String WRITE_PATH = "write-path";
    private static final String READ_PATH = "read-path";
    private static final String DELIM = "delimiter";
    private static final String FIELDNAMES = "field-names";
    private static Logger log = LoggerFactory.getLogger(DTFConnector.class);
    protected File writePath = null;
    protected File readPath = null;
    protected String delim = null;
    protected String[] fieldNames = null;


    private String delRegEx = null;

    public void setup(Map<String, String> config) throws IdMUnitException {
        /**
         * write-path=/var/opt/novell/IdM/files - Full path to directory to write files
         * read-path=/dummy/path - Full path to directory to validate files
         * Delimiter=, - Delimiter used in target database, and in the 'FieldNames' value here . .
         * field-names=USER ID, Name, FirstName, LastName, Group, Role
         */


        if (config.get(WRITE_PATH) == null) {
            throw new IdMUnitException("'" + WRITE_PATH + "' not configured");
        }
        writePath = new File(config.get(WRITE_PATH));
        if (!writePath.exists()) {
            throw new IdMUnitException("'" + WRITE_PATH + "' (" + config.get(WRITE_PATH) + ") does not exist");
        }

        if (config.get(READ_PATH) == null) {
            throw new IdMUnitException("'" + READ_PATH + "' not configured");
        }
        readPath = new File(config.get(READ_PATH));
        if (!readPath.exists()) {
            throw new IdMUnitException("'" + READ_PATH + "' (" + config.get(READ_PATH) + ") does not exist");
        }

        delim = config.get(DELIM);
        if (delim == null) {
            throw new IdMUnitException("'" + DELIM + "' not configured");
        }

        String f = config.get(FIELDNAMES);
        if (f == null) {
            throw new IdMUnitException("'" + FIELDNAMES + "' not configured");
        }

        fieldNames = f.split(" *" + delim + " *");
        if (fieldNames.length == 0) {
            throw new IdMUnitException("There must be at least one field specified in the configuration!");
        }
    }

//  public static void main(String[] args) throws Exception {
//      DTFConnector dtf = new DTFConnector();
//      dtf.readPath = new File("/");
//      dtf.opValidate(null);
//  }

    protected Map<String, String> convertToSingleValueMap(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        Map<String, String> ret = new HashMap<String, String>();
        for (String key : expectedAttrs.keySet()) {
            ret.put(key, ConnectorUtil.getSingleValue(expectedAttrs, key));
        }
        return ret;
    }

    private Map<String, String> getLastLineLastModifiedFile() throws IdMUnitException {
        File[] candidateFiles = readPath.listFiles(new OnlyFilesFilter());
        if (candidateFiles.length == 0) {
            throw new IdMUnitFailureException("No files to validate in: [" + readPath + "]");
        }

        Arrays.sort(candidateFiles, new MostRecentlyModifed());
        File mostRecentlyModified = candidateFiles[0];

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(mostRecentlyModified));
        } catch (FileNotFoundException e) {
            // Should never happen, unless file was deleted in the last few milliseconds, or there is a bug in the File apis.
            throw new IdMUnitException("Most recent file seems to have disappeared: [" + mostRecentlyModified + "], can not continue!", e);
        }

        String actualLine = null;
        String temp = null;
        try {
            while ((temp = br.readLine()) != null) {
                actualLine = temp;
            }
        } catch (IOException e) {
            throw new IdMUnitException("Failed while reading file: [" + mostRecentlyModified + "], can not continue!", e);
        }

        Map<String, String> retActualValues = new HashMap<String, String>();

        if (actualLine == null) {
            throw new IdMUnitException("Was not able to get any data from file: " + mostRecentlyModified.getAbsolutePath());
        }

        // split with negative parameter to include all fields that might be blank.
        String[] actualValues = actualLine.split(delim, -1);

        if (actualValues.length != fieldNames.length) {
            throw new IdMUnitException("Error: there were [" + fieldNames.length + "] fields specified, but [" + actualValues.length + "] were found in the file!");
        }

        for (int ctr = 0; ctr < fieldNames.length; ctr++) {
            retActualValues.put(fieldNames[ctr], actualValues[ctr]);
        }

        return retActualValues;
    }

    private File getMostRecentlyModifiedFileRel(File[] candidateFiles, int relativeFileMostRecent) {
        Arrays.sort(candidateFiles, new MostRecentlyModifed());
        if (candidateFiles.length > 0) {
            // TODO: need error checking
            return candidateFiles[relativeFileMostRecent];
        } else {
            return null;
        }
    }

    private Map<String, String> getRequestedLineRelModifiedFile(int posFromEnd, int fileFromEnd) throws IdMUnitException {
        File[] candidateFiles = readPath.listFiles(new OnlyFilesFilter());
        if (candidateFiles == null || candidateFiles.length == 0) {
            throw new IdMUnitFailureException("No files to validate in: [" + readPath + "]");
        }

        File mostRecentlyModified = getMostRecentlyModifiedFileRel(candidateFiles, fileFromEnd);

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(mostRecentlyModified));
        } catch (FileNotFoundException e) {
            // Should never happen, unless file was deleted in the last few milliseconds, or there is a bug in the File apis.
            throw new IdMUnitException("Most recent file seems to have disappeared: [" + mostRecentlyModified + "], can not continue!", e);
        }

        ArrayList<String> fileContents = new ArrayList<String>();
        String actualLine = null;
        String temp = null;
        try {
            while ((temp = br.readLine()) != null) {
                fileContents.add(temp);
            }
        } catch (IOException e) {
            throw new IdMUnitException("Failed while reading file: [" + mostRecentlyModified + "], can not continue!", e);
        }

        try {
            br.close();
        } catch (IOException e) {
            // failed to close .. .can't/don't need to do anything about it . .
        }
        int zeroBasedIndexes = -1;
        actualLine = fileContents.get(fileContents.size() + posFromEnd + zeroBasedIndexes);

        Map<String, String> retActualValues = new HashMap<String, String>();

        // split with negative parameter to include all fields that might be blank.
        String[] actualValues = actualLine.split(delim, -1);

        if (actualValues.length != fieldNames.length) {
            throw new IdMUnitException("Error: there were [" + fieldNames.length + "] fields specified, but [" + actualValues.length + "] were found in the file!");
        }

        for (int ctr = 0; ctr < fieldNames.length; ctr++) {
            retActualValues.put(fieldNames[ctr], actualValues[ctr]);
        }

        return retActualValues;
    }

    public void opValidateObject(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {

        Map<String, String> allActualValues = getLastLineLastModifiedFile();
        Map<String, String> allExpectedValues = convertToSingleValueMap(expectedAttrs);

        Failures failures = new Failures();

        for (String keyFieldName : allExpectedValues.keySet()) {
            String expected = allExpectedValues.get(keyFieldName);
            String actual = allActualValues.get(keyFieldName);

            if (!actual.matches(expected)) {
                List<String> expectedValues = new ArrayList<String>();
                List<String> actualValues = new ArrayList<String>();
                expectedValues.add(expected);
                actualValues.add(actual);
                failures.add("Field values were not equal for :[" + keyFieldName + "]", expectedValues, actualValues);
            }
            allActualValues.remove(keyFieldName);
        }

        if (allActualValues.size() > 0) {
            failures.add("There were more values than expected! :[" + allActualValues + "]");
        }

        if (failures.hasFailures()) {
            throw new IdMUnitFailureException(failures.toString());
        }
    }

    public void opValidateRow(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {

        int lineRequested = 0;
        int fileRequested = 0;
        Map<String, String> allExpectedValues = convertToSingleValueMap(expectedAttrs);
        String relativeEndOfFile = allExpectedValues.remove("relativeEndOfFile");
        String relativeRecentlyModifiedFile = allExpectedValues.remove("relativeRecentlyModifiedFile");

        if (relativeEndOfFile != null) {
            lineRequested = Integer.valueOf(relativeEndOfFile).intValue();
        }

        if (relativeRecentlyModifiedFile != null) {
            fileRequested = Integer.valueOf(relativeRecentlyModifiedFile).intValue();
        }

        Map<String, String> allActualValues = getRequestedLineRelModifiedFile(lineRequested, fileRequested);

        Failures failures = new Failures();

        for (String keyFieldName : allExpectedValues.keySet()) {
            String expected = allExpectedValues.get(keyFieldName);
            String actual = allActualValues.get(keyFieldName);

            if (actual == null) {
                actual = "![NO VALUE RECIEVED]!";
            }

            if (!actual.matches(expected)) {
                List<String> expectedValues = new ArrayList<String>();
                List<String> actualValues = new ArrayList<String>();
                expectedValues.add(expected);
                actualValues.add(actual);
                failures.add("Field values were not equal for :[" + keyFieldName + "]", expectedValues, actualValues);
            }
            allActualValues.remove(keyFieldName);
        }

        if (failures.hasFailures()) {
            throw new IdMUnitFailureException(failures.toString());
        }
    }

    public void opValidateXML(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {

        //TODO: possibly setup a call to assertXMLEquals . .

        int fileRequested = 0;
        Map<String, String> allExpectedValues = convertToSingleValueMap(expectedAttrs);
        String relativeRecentlyModifiedFile = allExpectedValues.remove("relativeRecentlyModifiedFile");
        String xpathPrefix = allExpectedValues.remove("XpathPrefix").replaceAll("\"", "");

        if (relativeRecentlyModifiedFile != null) {
            fileRequested = Integer.valueOf(relativeRecentlyModifiedFile).intValue();
        }

        File mostRecentXMLFile = getMostRecentlyModifiedFileRel(readPath.listFiles(new OnlyXMLFilesFilter()), fileRequested);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource;
        try {
            inputSource = new InputSource(new FileReader(mostRecentXMLFile));
        } catch (FileNotFoundException e) {
            throw new IdMUnitException("Found file: [" + mostRecentXMLFile.getAbsolutePath() + "] to validate, but appears to be deleted before validating!");
        }

        Failures failures = new Failures();
        String expressionClean = null;

        for (String expression : allExpectedValues.keySet()) {
            String actual = null;
            try {
                if (!expression.startsWith("@")) {
                    expressionClean = xpathPrefix + expression + "/text()";
                } else {
                    expressionClean = xpathPrefix + expression;
                }
                XPathExpression xpathCompiled = xpath.compile(expressionClean);
                FileReader mostRecentXMLFileReader = new FileReader(mostRecentXMLFile);
                inputSource = new InputSource(mostRecentXMLFileReader);
                Node nodeActual = (Node)xpathCompiled.evaluate(inputSource, XPathConstants.NODE);
                if (nodeActual == null) {
                    actual = "null";
                } else {
                    // Note: this call requires JDK 6.0!/eurupoa - xml-apis.jar appears to be different//
                    // getTextContent Seems to require europa jars, previous line: actual = nodeActual.getTextContent();
                    actual = nodeActual.getChildNodes().item(0).getNodeValue();
                }
                mostRecentXMLFileReader.close();

            } catch (XPathException e) {
                throw new IdMUnitException("Could not evaluate expresison: [" + expressionClean + "]! please resolve and try again: " + e);
            } catch (FileNotFoundException e) {
                throw new IdMUnitException("Could not find file: [" + mostRecentXMLFile.getAbsolutePath() + "]");
            } catch (IOException e) {
                throw new IdMUnitException("Failed while trying to close file: " + e);
            }

            String expected = allExpectedValues.get(expression);
            if (!actual.matches(expected)) {
                failures.add("Expected: [" + expected + "], but expression [" + expressionClean + "] resulted in: [" + actual + "]");
            }
        }


        if (failures.hasFailures()) {
            throw new IdMUnitFailureException(failures.toString());
        }

    }

    public void opDeleteFiles(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        Map<String, String> allExpectedValues = convertToSingleValueMap(expectedAttrs);
        delRegEx = allExpectedValues.get("DeleteByRegularExpression");

        File[] candidateFiles = readPath.listFiles(new RegularExpressionFileFilter());
        for (File deleteMe : candidateFiles) {
            if (deleteMe.delete()) {
                log.info("...deleted file: " + deleteMe.getAbsolutePath());
            } else {
                log.info("...unable to delete file: " + deleteMe.getAbsolutePath());
            }
        }
        // clear the regular expression for now.
        delRegEx = null;
    }

    public void opValidateFlatFile(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        Map<String, String> allExpectedValues = convertToSingleValueMap(expectedAttrs);
        String expectedFileContentsRegEx = allExpectedValues.get(FLAT_FILE_CONTENTS);

        File[] candidateFiles = readPath.listFiles(new OnlyFilesFilter());
        if (candidateFiles == null || candidateFiles.length == 0) {
            throw new IdMUnitException("Could not list files, or no files existed at . . .");
        }
        Arrays.sort(candidateFiles, new MostRecentlyModifed());
        File latestFile = candidateFiles[0];
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(latestFile));
        } catch (FileNotFoundException ex) {
            throw new IdMUnitException("File was created then deleted, lost access to file: [" + latestFile + "]");
        }

        StringBuffer actualfileContentsBuffer = new StringBuffer("");
        String temp = null;
        try {
            while ((temp = br.readLine()) != null) {
                actualfileContentsBuffer.append(temp + "\n");
            }
        } catch (IOException e) {
            throw new IdMUnitException("Failed while reading file: [" + latestFile + "], can not continue!", e);
        }

        String actualContents = actualfileContentsBuffer.toString().trim(); // trim to remove the last \n
        String clean = actualfileContentsBuffer.toString();
        clean = clean.trim();


        if (!actualContents.toString().matches(expectedFileContentsRegEx)) {
            throw new IdMUnitFailureException("Expected:[" + expectedFileContentsRegEx + "] but was: [" + actualContents + "]");
        }
    }

    public void opDeleteMostRecentFile(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        File[] candidateFiles = readPath.listFiles(new OnlyFilesFilter());
        if (candidateFiles == null || candidateFiles.length == 0) {
            throw new IdMUnitFailureException("No files to validate in: [" + readPath + "]");
        }
        File mostRecentFile = getMostRecentlyModifiedFileRel(candidateFiles, 0);
        if (!mostRecentFile.delete()) {
            throw new IdMUnitException("Could not delete file: [" + mostRecentFile + "]");
        }

    }

    class OnlyFilesFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isFile();
        }

    }

    class OnlyXMLFilesFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getAbsolutePath().endsWith(".xml");
        }

    }

    class MostRecentlyModifed implements Comparator<File> {
        public int compare(File filea, File fileb) {
            if (filea.lastModified() < fileb.lastModified()) {
                return 1;
            } else {
                return -1;
            }

        }
    }

    class RegularExpressionFileFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.getName().matches(delRegEx);
        }

    }
}
