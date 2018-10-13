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

import au.com.bytecode.opencsv.CSVWriter;
import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.ConnectionConfigData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class DTF2ConnectorTests extends TestCase {
    public final int numTestFiles = 10;
    File readPath = null;
    List<File> testFiles = null;
    String fieldDefinitions = "USER ID(10), Name(15), FirstName(20), LastName(25), Group(30), Role(35)";
    ConnectionConfigData configurationData = null;
    private DTF2Connector dtfConn = null;

    public DTF2ConnectorTests() {
        readPath = new File("tempPath");
        dtfConn = new DTF2Connector();
        testFiles = new ArrayList<File>();

        // Setup default configuration for all tests:
        configurationData = new ConnectionConfigData("DTF", "com.trivir.idmunit.connector.DTFConnector");
        configurationData.setParam(DTF2Connector.READ_PATH, readPath.getAbsolutePath());
        configurationData.setParam(DTF2Connector.WRITE_PATH, System.getProperty("java.io.tmpdir"));
        configurationData.setParam(DTF2Connector.DELIM, ",");
        configurationData.setParam(DTF2Connector.ROW_KEY, "USER ID"); // (has to match field name or definitions. .)
        configurationData.setParam(DTF2Connector.OUTPUT_FILE_EXT, ".csv");
        configurationData.setParam(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);
    }

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    protected void setUp() throws Exception {
        super.setUp();

        if (!readPath.exists() && !readPath.mkdirs()) {
            readPath.delete();
            if (!readPath.mkdirs()) {
                throw new IdMUnitException("Could not create base path: [" + readPath + "], delete directory, or change the path to test with and try again.");
            }
        }

        // Make the first file in our file set a .tmp file, so we can modify it as the connector is expecting it to change.
        testFiles.add(File.createTempFile("testFile", DTF2Connector.DTF_DRIVER_DEFAULT_TMP_PREFIX, readPath));

        for (int i = 0; i < numTestFiles; i++) {
            try {
                File file = File.createTempFile("testFile", ".csv", readPath);
                testFiles.add(file);
            } catch (IOException e) {
                throw new IdMUnitException("Failed while trying to create temporary files", e);
            }
        }
        dtfConn.setup(configurationData.getParams());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        List<String> messages = new ArrayList<String>();

        //TODO: WARNING: this deletes all files in the directory; but is only for testing . .
        for (File f : readPath.listFiles()) {
            if (!f.delete()) {
                messages.add("Could not delete file: [" + f + "]");
            }
        }

        if (messages.size() > 0) {
            throw new IdMUnitException("Could not delete all files and folders!\n" + messages.toString());
        }
        dtfConn.tearDown();
    }

    private void writeData(File tempFile) throws IOException {
        writeData(tempFile, "");
    }

    /**
     * Writes out data to the given file, just using the header names with a postfixed number, and a prefix as specified for 'prefix' as the content.
     *
     * @throws IOException
     */
    private void writeData(File tempFile, String prefix) throws IOException {
        List<String[]> rowsData = new ArrayList<String[]>();
        List<String> rowData = new ArrayList<String>();

        String[] fields = fieldDefinitions.split(" *, *");

        for (int row = 0; row < 10; row++) {
            for (String field : fields) {
                String fieldName = field.substring(0, field.indexOf("("));
                int fieldLength = Integer.parseInt(field.substring(field.indexOf("(")).replace("(", "").replace(")", ""));
                StringBuilder fieldData = new StringBuilder(prefix + fieldName + row);
                while (fieldData.length() < fieldLength) {
                    fieldData.append(" ");
                }
                rowData.add(fieldData.toString());
            }
            rowsData.add(rowData.toArray(new String[rowData.size()]));
            rowData.clear();

        }
        //don't use same API to test with . .TODO replace this
        boolean append = true;
        CSVWriter writer = new CSVWriter(new PrintWriter(new FileWriter(tempFile, append)), ',', '"');
        writer.writeAll(rowsData);
        writer.close();
    }

    /**
     * Testing the connector properly fails if the setup is called, then a validate is called immediately afterwards (no changes written to files)
     * @throws IdMUnitException
     * @throws IOException
     */
//    public void testValidateNoFilesChanged() throws IdMUnitException, IOException {
//        try {
//            dtfConn.opValidate(new HashMap<String, Collection<String>>());
//        } catch(IdMUnitFailureException e) {
//            assertEquals("No files changed since the DTF connector checked last, failed validation!", e.getMessage());
//            return;
//        }
//        fail("Test should have thrown an exception, no files changed!");
//    }

    /**
     * Testing situation where row-key can not be found in the data written.
     *
     * @throws IdMUnitException
     * @throws IOException
     */
    public void testValidateNoRowsMatch() throws IdMUnitException, IOException {
        // Get the temp file from our test cache, and write some test data to it:
        writeData(testFiles.get(0));

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("ffletcher-wrong"));
        expectedAttrs.put("Name", singleValue("FerbFletcher-wrong"));
        expectedAttrs.put("FirstName", singleValue("Ferb-wrong"));
        expectedAttrs.put("LastName", singleValue("Fletcherwrong"));
        expectedAttrs.put("Group", singleValue("TVShows-wrong"));
        expectedAttrs.put("Role", singleValue("Awesome-wrong"));
        //http://javacsv.sourceforge.net/

        try {
            dtfConn.opValidate(expectedAttrs);
        } catch (IdMUnitFailureException e) {
            assertEquals("None of the new data written out matched the expected key from the spreadsheet!", e.getMessage());
            return;
        }
        fail("Should have thrown no rows match exception");
    }

    /**
     * Tests failure response with data that doesn't match validation, but did match the key
     *
     * @throws IdMUnitException
     * @throws IOException
     */
    public void testValidateWrong() throws IdMUnitException, IOException {
        // Get the temp file from our test cache, and write to it:
        writeData(testFiles.get(0));

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("USER ID5"));
        expectedAttrs.put("Name", singleValue("FerbFletcher-wrong"));
        expectedAttrs.put("FirstName", singleValue("Ferb-wrong"));
        expectedAttrs.put("LastName", singleValue("Fletcherwrong"));
        expectedAttrs.put("Group", singleValue("TVShows-wrong"));
        expectedAttrs.put("Role", singleValue("Awesome-wrong"));

        try {
            dtfConn.opValidate(expectedAttrs);
        } catch (IdMUnitFailureException e) {
            assertEquals("Name expected:<[FerbFletcher-wrong]> but was:<[Name5]>\r\n" +
                            "FirstName expected:<[Ferb-wrong]> but was:<[FirstName5]>\r\n" +
                            "Role expected:<[Awesome-wrong]> but was:<[Role5]>\r\n" +
                            "LastName expected:<[Fletcherwrong]> but was:<[LastName5]>\r\n" +
                            "Group expected:<[TVShows-wrong]> but was:<[Group5]>",
                    e.getMessage());
            return;
        }
        fail("Should have thrown IdmUnitFailure exception, expected did not match the actual!");
    }

    /**
     * @throws IdMUnitException
     * @throws IOException
     */
    public void testValidate() throws IdMUnitException, IOException {
        // Get the temp file from our test cache, and write to it:
        writeData(testFiles.get(0));

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("USER ID5"));
        expectedAttrs.put("Name", singleValue("Name5"));
        expectedAttrs.put("FirstName", singleValue("FirstName5"));
        expectedAttrs.put("LastName", singleValue("LastName5"));
        expectedAttrs.put("Group", singleValue("Group5"));
        expectedAttrs.put("Role", singleValue("Role5"));
        dtfConn.opValidate(expectedAttrs);
    }

    public void testValidateMultipleWriteSessions() throws IdMUnitException, IOException {
        // Get the temp file from our test cache, and write to it:
        writeData(testFiles.get(0));

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("USER ID6"));
        expectedAttrs.put("Name", singleValue("Name6"));
        expectedAttrs.put("FirstName", singleValue("FirstName6"));
        expectedAttrs.put("LastName", singleValue("LastName6"));
        expectedAttrs.put("Group", singleValue("Group6"));
        expectedAttrs.put("Role", singleValue("Role6"));
        dtfConn.opValidate(expectedAttrs);

        // Now, write another set of data to the same file, and validate it:
        writeData(testFiles.get(0), "NewData");
        expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("NewDataUSER ID6"));
        expectedAttrs.put("Name", singleValue("NewDataName6"));
        expectedAttrs.put("FirstName", singleValue("NewDataFirstName6"));
        expectedAttrs.put("LastName", singleValue("NewDataLastName6"));
        expectedAttrs.put("Group", singleValue("NewDataGroup6"));
        expectedAttrs.put("Role", singleValue("NewDataRole6"));
        //http://javacsv.sourceforge.net/
        dtfConn.opValidate(expectedAttrs);
    }

    public void testValidateTmpFileRollOver() throws IdMUnitException, IOException {
        // Get the temp file from our test cache, and write to it:
        File testTmpFile = testFiles.get(0);
        writeData(testTmpFile);

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("USER ID6"));
        expectedAttrs.put("Name", singleValue("Name6"));
        expectedAttrs.put("FirstName", singleValue("FirstName6"));
        expectedAttrs.put("LastName", singleValue("LastName6"));
        expectedAttrs.put("Group", singleValue("Group6"));
        expectedAttrs.put("Role", singleValue("Role6"));
        //http://javacsv.sourceforge.net/
        dtfConn.opValidate(expectedAttrs);

        writeData(testTmpFile, "NewData");
        expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("NewDataUSER ID6"));
        expectedAttrs.put("Name", singleValue("NewDataName6"));
        expectedAttrs.put("FirstName", singleValue("NewDataFirstName6"));
        expectedAttrs.put("LastName", singleValue("NewDataLastName6"));
        expectedAttrs.put("Group", singleValue("NewDataGroup6"));
        expectedAttrs.put("Role", singleValue("NewDataRole6"));
        //http://javacsv.sourceforge.net/
        dtfConn.opValidate(expectedAttrs);

        // Rename the tmp file to .csv, then create a new .tmp file:
        File tmpFileRenamed = new File(readPath, testTmpFile.getName().replaceFirst("[.][^.]+$", ".csv"));
        File newTmpFile = File.createTempFile("testFile", DTF2Connector.DTF_DRIVER_DEFAULT_TMP_PREFIX, readPath);

        //Rename our temp file, then add the renamed one back:
        testFiles.get(0).renameTo(tmpFileRenamed);
        testFiles.remove(0);
        testFiles.add(tmpFileRenamed);
        testFiles.add(newTmpFile);

        writeData(newTmpFile, "newtmpfile");
        // now validate some new data
        expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("newtmpfileUSER ID6"));
        expectedAttrs.put("Name", singleValue("newtmpfileName6"));
        expectedAttrs.put("FirstName", singleValue("newtmpfileFirstName6"));
        expectedAttrs.put("LastName", singleValue("newtmpfileLastName6"));
        expectedAttrs.put("Group", singleValue("newtmpfileGroup6"));
        expectedAttrs.put("Role", singleValue("newtmpfileRole6"));
        dtfConn.opValidate(expectedAttrs);
    }

    public void testValidateTmpFileRollOverReadCacheData() throws IdMUnitException, IOException {
        // Get the temp file from our test cache, and write to it:
        File testTmpFile = testFiles.get(0);
        writeData(testTmpFile, "FirstData");

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("FirstDataUSER ID6"));
        expectedAttrs.put("Name", singleValue("FirstDataName6"));
        expectedAttrs.put("FirstName", singleValue("FirstDataFirstName6"));
        expectedAttrs.put("LastName", singleValue("FirstDataLastName6"));
        expectedAttrs.put("Group", singleValue("FirstDataGroup6"));
        expectedAttrs.put("Role", singleValue("FirstDataRole6"));
        //http://javacsv.sourceforge.net/
        dtfConn.opValidate(expectedAttrs);

        writeData(testTmpFile, "NewData");
        expectedAttrs.clear();
        expectedAttrs.put("USER ID", singleValue("NewDataUSER ID6"));
        expectedAttrs.put("Name", singleValue("NewDataName6"));
        expectedAttrs.put("FirstName", singleValue("NewDataFirstName6"));
        expectedAttrs.put("LastName", singleValue("NewDataLastName6"));
        expectedAttrs.put("Group", singleValue("NewDataGroup6"));
        expectedAttrs.put("Role", singleValue("NewDataRole6"));
        //http://javacsv.sourceforge.net/
        dtfConn.opValidate(expectedAttrs);

        // Rename the tmp file to .csv, then create a new .tmp file:
        File tmpFileRenamed = new File(readPath, testTmpFile.getName().replaceFirst("[.][^.]+$", ".csv"));
        testFiles.get(0).renameTo(tmpFileRenamed);
        testFiles.remove(0);
        testFiles.add(0, tmpFileRenamed);
        File newTmpFile = File.createTempFile("testFile", DTF2Connector.DTF_DRIVER_DEFAULT_TMP_PREFIX, readPath);
        testFiles.add(newTmpFile);

        writeData(newTmpFile, "newtmpfile");
        // See that we can still validate the old data:
        expectedAttrs.clear();
        expectedAttrs.put("USER ID", singleValue("NewDataUSER ID6"));
        expectedAttrs.put("Name", singleValue("NewDataName6"));
        expectedAttrs.put("FirstName", singleValue("NewDataFirstName6"));
        expectedAttrs.put("LastName", singleValue("NewDataLastName6"));
        expectedAttrs.put("Group", singleValue("NewDataGroup6"));
        expectedAttrs.put("Role", singleValue("NewDataRole6"));
        dtfConn.opValidate(expectedAttrs);

        // now read data from the cache; but we have to start by writing more data to our temp file; so the DTF dirver will allow a validation (at least one change has to occur)
        writeData(newTmpFile, "morenewtmpfile");
        // Validate old data from cache:
        expectedAttrs.clear();
        expectedAttrs.put("USER ID", singleValue("FirstDataUSER ID6"));
        expectedAttrs.put("Name", singleValue("FirstDataName6"));
        expectedAttrs.put("FirstName", singleValue("FirstDataFirstName6"));
        expectedAttrs.put("LastName", singleValue("FirstDataLastName6"));
        expectedAttrs.put("Group", singleValue("FirstDataGroup6"));
        expectedAttrs.put("Role", singleValue("FirstDataRole6"));
        dtfConn.opValidate(expectedAttrs);

        // now write more data, and check the new data:
        writeData(newTmpFile, "AAA");
        // Validate old data from cache:
        expectedAttrs.clear();
        expectedAttrs.put("USER ID", singleValue("AAAUSER ID6"));
        expectedAttrs.put("Name", singleValue("AAAName6"));
        expectedAttrs.put("FirstName", singleValue("AAAFirstName6"));
        expectedAttrs.put("LastName", singleValue("AAALastName6"));
        expectedAttrs.put("Group", singleValue("AAAGroup6"));
        expectedAttrs.put("Role", singleValue("AAARole6THIS SHOULDFAIL"));
        try {
            dtfConn.opValidate(expectedAttrs);
        } catch (IdMUnitFailureException e) {
            assertEquals("Role expected:<[AAARole6THIS SHOULDFAIL]> but was:<[AAARole6]>", e.getMessage());
        }
    }
}
