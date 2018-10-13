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

import junit.framework.TestCase;

import java.io.*;
import java.util.*;

public class FileFormatTests extends TestCase {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String READ_PATH = System.getProperty("java.io.tmpdir") + File.separator + "dtfFileFormatTests-output";
    private static final String WRITE_PATH = System.getProperty("java.io.tmpdir") + File.separator + "dtfFileFormatTests-input";
    private static final Map<String, String> DEFAULT_CONFIG_PARAMS;

    static {
        Map<String, String> configParams = new HashMap<String, String>();
        configParams.put(DTF2Connector.READ_PATH, READ_PATH);
        configParams.put(DTF2Connector.WRITE_PATH, WRITE_PATH);
        DEFAULT_CONFIG_PARAMS = Collections.unmodifiableMap(configParams);
    }

    private static void setupDir(String dir) throws IOException {
        File testDir = new File(dir);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            for (File f : files) {
                if (!f.delete()) {
                    throw new RuntimeException("Error deleting file: " + f.getName());
                }
            }
        } else {
            if (!testDir.mkdir()) {
                throw new RuntimeException("Error creating test directory: " + dir);
            }
        }

    }

    private static void cleanupDir(String dir) {
        File testDir = new File(dir);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            for (File f : files) {
                if (!f.delete()) {
                    throw new RuntimeException("Error deleting file: " + f.getAbsolutePath());
                }
            }
            if (!testDir.delete()) {
                throw new RuntimeException("Error deleting test directory: " + dir);
            }
        }

    }

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    public void setUp() throws Exception {
        super.setUp();
        setupDir(READ_PATH);
        setupDir(WRITE_PATH);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        cleanupDir(READ_PATH);
        cleanupDir(WRITE_PATH);
    }

    // The format of a row in the BEELINE file is:
    //          0         0         0         0         0         0         0         0         0         1         1         1         1         1         1         1         1         1         1         2         2         2         2         2         2         2         2         2         2         3         3         3         3         3         3         3         3         3         3         4         4         4         4         4         4         4         4
    // ....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7
    // U$x19194    $Alethia                       $Janel                         $Harris                        $Alethia                                                     $0101    $4389$                                                  $                                                  $Atlanta                       $GA$30354    $555-555-5555             $                         $gwen.willis@gatags.com                  $GAT Airline Ground Support                                                      $Delta Airlines Air Freight 710 Airport Road, Bay 7 & 8 Flowood MS 39232                             $          $  $    $                    $                    $                    $Outsourced          $20130627100000$20131021235959$N$e100343   $            $14583     $XSHL$Vendor Baggage Transporter                                                      $03$Ground Ops                                                                      $ATL  $Y$N$Y$N$Y$Y
    public void testValidateBeeline() throws Exception {
        final int numRows = 10;
        String fieldDefinitions = "UNUSED(1), USER ID(10), FirstName(30), MiddleName(30), LastName(60), PreferredName(60), Email(40), Enabled(1)";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, "$");
        configParams.put(DTF2Connector.ROW_KEY, "USER ID"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".csv");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        FileWriter f = new FileWriter(READ_PATH + File.separator + "BEELINE_ISS_DATA.201310281700.csv");
        for (int r = 0; r < numRows; ++r) {
            StringBuilder s = new StringBuilder();
            s.append(String.format("%-1s$", "U"));
            s.append(String.format("%-10s$", "x1919" + r));
            s.append(String.format("%-30s$", "Test" + r));
            s.append(String.format("%-30s$", "J."));
            s.append(String.format("%-60s$", "User" + r));
            s.append(String.format("%-60s$", "Tester" + r));
            s.append(String.format("%-40s$", "test.user" + r + "@example.com"));
            s.append(String.format("%-1s", "Y"));
            s.append(LINE_SEPARATOR);
            f.write(s.toString());
        }
        f.close();

        for (int r = 0; r < numRows; ++r) {
            Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
            expectedAttrs.put("USER ID", singleValue("x1919" + r));
            expectedAttrs.put("FirstName", singleValue("Test" + r));
            expectedAttrs.put("MiddleName", singleValue("J."));
            expectedAttrs.put("LastName", singleValue("User" + r));
            expectedAttrs.put("PreferredName", singleValue("Tester" + r));
            expectedAttrs.put("Email", singleValue("test.user" + r + "@example.com"));
            expectedAttrs.put("Enabled", singleValue("Y"));
            dtfConn.opValidate(expectedAttrs);
        }
    }

    public void testAddBeeline() throws Exception {
        String fieldDefinitions = "UNUSED(1), USER ID(10), FirstName(30), MiddleName(30), LastName(60)";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, "$");
        configParams.put(DTF2Connector.ROW_KEY, "USER ID"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".csv");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        Map<String, Collection<String>> values = new HashMap<String, Collection<String>>();
        values.put("UNUSED", singleValue("U"));
        values.put("USER ID", singleValue("x1919"));
        values.put("FirstName", singleValue("Test"));
        values.put("LastName", singleValue("User"));
        dtfConn.opAdd(values);

        File writePath = new File(WRITE_PATH);
        String[] filenames = writePath.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        });

        assertEquals(1, filenames.length);

        assertTrue(filenames[0].endsWith(".csv"));
        BufferedReader r = new BufferedReader(new FileReader(new File(writePath, filenames[0])));
        String data = r.readLine();
        assertEquals(-1, r.read()); // check that there is no more data
        r.close();

        assertEquals("U$x1919     $Test                          $                              $User                                                        ", data);
    }

    // The format of a row in the CorpSpec file is:
    // x206854;SWA Contractor;Gateway Group One;14561;Weyni;Weyni;;Yonaskinds;;;Las Vegas;NV;89103;wyonaskinds@gatewaygroupone.com;;;Charles D Eaton;Ground Ops;Contractors;;LAS;;9743;A;Vendor Baggage Transporter
    //
    // The format of a row in the MC file is:
    // x206854;Weyni Yonaskinds;XSHL;Vendor Baggage Transporter;LAS;03;Ground Ops;;;1
    // This is basically the same format as the CorpSpec file so this test will cover it.
    public void testValidateCorpSpecAndMC() throws Exception {
        final int numRows = 10;
        String fieldDefinitions = "EMPLOYEE_NUM, EMPLOYEE_TYPE, DEPARTMENT, DEPARTMENT_NUM, PREFERRED_NAME, FIRST_NAME, MIDDLE_NAME, LAST_NAME, ADDRESS_1, ADDRESS_2, CITY, STATE, ZIPCODE, EMAIL, UNUSED_1, UNUSED_2, FULL_NAME";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, ";");
        configParams.put(DTF2Connector.ROW_KEY, "EMPLOYEE_NUM"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".txt_CorpSec");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        FileWriter f = new FileWriter(READ_PATH + File.separator + "1383836505714.txt_CorpSec");
        for (int r = 0; r < numRows; ++r) {
            StringBuilder s = new StringBuilder();
            s.append("x1919" + r).append(';');
            s.append("Contractor").append(';');
            s.append("Testing Department").append(';');
            s.append("14561").append(';');
            s.append("Tester" + r).append(';');
            s.append("Test" + r).append(';');
            s.append("J.").append(';');
            s.append("User" + r).append(';');
            s.append(';');
            s.append(';');
            s.append("Las Vegas").append(';');
            s.append("NV").append(';');
            s.append("89103").append(';');
            s.append("test" + r + ".user" + r + "@example.com").append(';');
            s.append(';');
            s.append(';');
            s.append("Test" + r + " J. User" + r);
            s.append(LINE_SEPARATOR);
            f.write(s.toString());
        }
        f.close();

        for (int r = 0; r < numRows; ++r) {
            Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
            expectedAttrs.put("EMPLOYEE_NUM", singleValue("x1919" + r));
            expectedAttrs.put("EMPLOYEE_TYPE", singleValue("Contractor"));
            expectedAttrs.put("DEPARTMENT", singleValue("Testing Department"));
            expectedAttrs.put("DEPARTMENT_NUM", singleValue("14561"));
            expectedAttrs.put("PREFERRED_NAME", singleValue("Tester" + r));
            expectedAttrs.put("FIRST_NAME", singleValue("Test" + r));
            expectedAttrs.put("MIDDLE_NAME", singleValue("J."));
            expectedAttrs.put("LAST_NAME", singleValue("User" + r));
            expectedAttrs.put("CITY", singleValue("Las Vegas"));
            expectedAttrs.put("STATE", singleValue("NV"));
            expectedAttrs.put("ZIPCODE", singleValue("89103"));
            expectedAttrs.put("EMAIL", singleValue("test" + r + ".user" + r + "@example.com"));
            expectedAttrs.put("FULL_NAME", singleValue("Test" + r + " J. User" + r));
            dtfConn.opValidate(expectedAttrs);
        }
    }

    public void testAddCorpSpecAndMC() throws Exception {
        String fieldDefinitions = "EMPLOYEE_NUM, EMPLOYEE_TYPE, DEPARTMENT, DEPARTMENT_NUM, PREFERRED_NAME, FIRST_NAME, MIDDLE_NAME, LAST_NAME, ADDRESS_1, ADDRESS_2, CITY, STATE, ZIPCODE, EMAIL, UNUSED_1, UNUSED_2, FULL_NAME";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, ";");
        configParams.put(DTF2Connector.ROW_KEY, "EMPLOYEE_NUM"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".txt_CorpSec");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        Map<String, Collection<String>> values = new HashMap<String, Collection<String>>();
        values.put("EMPLOYEE_NUM", singleValue("x1919"));
        values.put("EMPLOYEE_TYPE", singleValue("Contractor"));
        values.put("DEPARTMENT", singleValue("Testing Department"));
        values.put("DEPARTMENT_NUM", singleValue("14561"));
        values.put("PREFERRED_NAME", singleValue("Tester"));
        values.put("FIRST_NAME", singleValue("Test"));
//        values.put("MIDDLE_NAME", singleValue("J."));
        values.put("LAST_NAME", singleValue("User"));
        values.put("CITY", singleValue("Las Vegas"));
        values.put("STATE", singleValue("NV"));
        values.put("ZIPCODE", singleValue("89103"));
        values.put("EMAIL", singleValue("test.user@example.com"));
        values.put("FULL_NAME", singleValue("Test J. User"));
        dtfConn.opAdd(values);

        File writePath = new File(WRITE_PATH);
        String[] filenames = writePath.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        });

        assertEquals(1, filenames.length);

        assertTrue(filenames[0].endsWith(".txt_CorpSec"));
        BufferedReader r = new BufferedReader(new FileReader(new File(writePath, filenames[0])));
        String data = r.readLine();
        assertEquals(-1, r.read()); // check that there is no more data
        r.close();

        assertEquals("x1919;Contractor;Testing Department;14561;Tester;Test;;User;;;Las Vegas;NV;89103;test.user@example.com;;;Test J. User", data);
    }

    // The format of a row in the PROLAW file is:
    //          0         0         0         0         0         0         0         0         0         1         1         1         1         1         1         1         1         1         1         2         2         2         2         2         2         2         2         2         2         3         3         3         3         3         3         3         3         3         3         4         4         4         4         4         4         4         4
    // ....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7
    // C|000000030343|Montez,Esther Marie           |7906 s 48 th ln         |                        |laveen                  |AZ|85339|06023261812|Customer Representative       |Customer Support & Services   |Customer Support & Svcs       |PC |03/24/1995|03/24/1995|A|00/00/0000|                                        |05/03/1964|H|M|F|527-69-5756 | |0000000.00|FULL TIME |03/24/1995|00/00/0000|10/04/2012|10/23/2012|09/27/2012|03/24/1995|03/24/1995|00/00/0000|00/00/0000|N|
    //
    //  Column                               Posi-
    //  sequence Column name           Size  tion  Description                             Example Data
    //  -------- --------------------  ----  ----  --------------------------------------  -----------------------------
    //         1                          1     1  ?                                       C
    //         2                         12     3  ?                                       000000030343
    //         3 Full Name               30    16                                          Montez,Esther Marie
    //         4 Address 1               24    47                                          7906 s 48 th ln
    //         5 Address 2               24    72                                          Apt 102
    //         6 City                    24    97                                          Laveen
    //         7 State                    2   122                                          AZ
    //         8 Zipcode                  5   125                                          85339
    //
    public void testValidateProlaw() throws Exception {
        final int numRows = 10;
        String fieldDefinitions = "UNKNOWN_1(1), ID(12), FULL_NAME(30), ADDRESS_1(24), ADDRESS_2(24), CITY(24), STATE(2), ZIPCODE(5)";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, "|");
        configParams.put(DTF2Connector.ROW_KEY, "ID"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".txt_PROLAW");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        FileWriter f = new FileWriter(READ_PATH + File.separator + "1373671843310.txt_PROLAW");
        for (int r = 0; r < numRows; ++r) {
            StringBuilder s = new StringBuilder();
            s.append(String.format("%-1s", "C")).append('|');
            s.append(String.format("%012d", 30343 + r)).append('|');
            s.append(String.format("%-30s", "Test J. User" + r)).append('|');
            s.append(String.format("%-24s", "220 N 1200 E" + r)).append('|');
            s.append(String.format("%-24s", "Apt 1" + r)).append('|');
            s.append(String.format("%-24s", "Laveen")).append('|');
            s.append(String.format("%-2s", "AZ")).append('|');
            s.append(String.format("%-5s", "84043"));
            s.append(LINE_SEPARATOR);
            f.write(s.toString());
        }
        f.close();

        for (int r = 0; r < numRows; ++r) {
            Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
            expectedAttrs.put("ID", singleValue(String.format("%012d", 30343 + r)));
            expectedAttrs.put("FULL_NAME", singleValue("Test J. User" + r));
            expectedAttrs.put("ADDRESS_1", singleValue("220 N 1200 E" + r));
            expectedAttrs.put("ADDRESS_2", singleValue("Apt 1" + r));
            expectedAttrs.put("CITY", singleValue("Laveen"));
            expectedAttrs.put("STATE", singleValue("AZ"));
            expectedAttrs.put("ZIPCODE", singleValue("84043"));
            dtfConn.opValidate(expectedAttrs);
        }
    }

    public void testAddProlaw() throws Exception {
        String fieldDefinitions = "UNKNOWN_1(1), ID(12), FULL_NAME(30), ADDRESS_1(24), ADDRESS_2(24), CITY(24), STATE(2), ZIPCODE(5)";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, "|");
        configParams.put(DTF2Connector.ROW_KEY, "ID"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".txt_PROLAW");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        Map<String, Collection<String>> values = new HashMap<String, Collection<String>>();
        values.put("UNKNOWN_1", singleValue("C"));
        values.put("ID", singleValue(String.format("%012d", 30343)));
        values.put("FULL_NAME", singleValue("Test J. User"));
        values.put("ADDRESS_1", singleValue("220 N 1200 E"));
        values.put("ADDRESS_2", singleValue("Apt 1"));
        values.put("CITY", singleValue("Laveen"));
        values.put("STATE", singleValue("AZ"));
        values.put("ZIPCODE", singleValue("84043"));
        dtfConn.opAdd(values);

        File writePath = new File(WRITE_PATH);
        String[] filenames = writePath.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        });

        assertEquals(1, filenames.length);

        assertTrue(filenames[0].endsWith(".txt_PROLAW"));
        BufferedReader r = new BufferedReader(new FileReader(new File(writePath, filenames[0])));
        String data = r.readLine();
        assertEquals(-1, r.read()); // check that there is no more data
        r.close();

        assertEquals("C|000000030343|Test J. User                  |220 N 1200 E            |Apt 1                   |Laveen                  |AZ|84043", data);
    }

    // The format of a row in the LMS file is:
    //
    //          0         0         0         0         0         0         0         0         0         1         1         1         1         1         1         1         1         1         1         2         2         2         2         2         2         2         2         2         2         3         3         3         3         3         3         3         3         3         3         4         4         4         4         4         4         4         4         4         4         5         5         5         5         5         5         5         5         5         5         6         6         6
    // ....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2....|....3....|....4....|....5....|....6....|....7....|....8....|....9....|....0....|....1....|....2
    // "   17695"|"CARTER                                  "|"JUDI                                    "|"W"|"JUDI                                    "|"4004 LUCERNE CT.                                            "|"                                        "|"MELBOURNE                               "|"FL "|"32904     "|"FA01                     "|"FLIGHT ATTENDANT                        "|"04        "|"INFLIGHT                      "|"MCO "|"32314     "|"2400   "|"18011     "|"11 20 1991"|"A"|"09 08 1943"|"214-417-5114   "|"04 22 1994"|"9B"|"01"|"9494"|"11 20 1991"|"D  "|"10/20/2009"|"10/27/2009"|"Judi.Carter@wnco.com                                                            "|"0402"|"INFLIGHT BASES                "|"Y"|"N"|"F"|"FA01    "|"          "|"                                        "|"N"|"          "|"          "|"                                        "|"  "
    public void testValidateLMS() throws Exception {
        final int numRows = 10;
        String fieldDefinitions = "ID(8), LAST_NAME(40), FIRST_NAME(40), MIDDLE_INITIAL(1), PREFERRED_NAME(40)";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, "|");
        configParams.put(DTF2Connector.ROW_KEY, "ID"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".txt_LMS");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        FileWriter f = new FileWriter(READ_PATH + File.separator + "1367623704863.txt_LMS");
        for (int r = 0; r < numRows; ++r) {
            StringBuilder s = new StringBuilder();
            s.append('"').append(String.format("%8d", 30343 + r)).append('"').append('|');
            s.append('"').append(String.format("%-40s", "User" + r)).append('"').append('|');
            s.append('"').append(String.format("%-40s", "Test" + r)).append('"').append('|');
            s.append('"').append(String.format("%-1s", "J")).append('"').append('|');
            s.append('"').append(String.format("%-40s", "Tester" + r)).append('"');
            s.append(LINE_SEPARATOR);
            f.write(s.toString());
        }
        f.close();

        for (int r = 0; r < numRows; ++r) {
            Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
            expectedAttrs.put("ID", singleValue(String.format("%d", 30343 + r)));
            expectedAttrs.put("LAST_NAME", singleValue("User" + r));
            expectedAttrs.put("FIRST_NAME", singleValue("Test" + r));
            expectedAttrs.put("MIDDLE_INITIAL", singleValue("J"));
            expectedAttrs.put("PREFERRED_NAME", singleValue("Tester" + r));
            dtfConn.opValidate(expectedAttrs);
        }
    }

    public void testAddLMS() throws Exception {
        String fieldDefinitions = "ID(8), LAST_NAME(40), FIRST_NAME(40), MIDDLE_INITIAL(1), PREFERRED_NAME(40)";

        Map<String, String> configParams = new HashMap<String, String>(DEFAULT_CONFIG_PARAMS);
        configParams.put(DTF2Connector.DELIM, "|");
        configParams.put(DTF2Connector.ROW_KEY, "ID"); // (has to match field name or definitions. .)
        configParams.put(DTF2Connector.OUTPUT_FILE_EXT, ".txt_LMS");
        configParams.put(DTF2Connector.FIELD_DEFINITIONS, fieldDefinitions);

        DTF2Connector dtfConn = new DTF2Connector();
        dtfConn.setup(configParams);

        Map<String, Collection<String>> values = new HashMap<String, Collection<String>>();
        values.put("ID", singleValue(String.format("%d", 30343)));
        values.put("LAST_NAME", singleValue("User"));
        values.put("FIRST_NAME", singleValue("Test"));
        values.put("MIDDLE_INITIAL", singleValue("J"));
        values.put("PREFERRED_NAME", singleValue("Tester"));
        dtfConn.opAdd(values);

        File writePath = new File(WRITE_PATH);
        String[] filenames = writePath.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        });

        assertEquals(1, filenames.length);

        assertTrue(filenames[0].endsWith(".txt_LMS"));
        BufferedReader r = new BufferedReader(new FileReader(new File(writePath, filenames[0])));
        r.readLine();
        assertEquals(-1, r.read()); // check that there is no more data
        r.close();

        // The connector currently does not support adding field qualifiers like " and
        // it doesn't support right justification
//        assertEquals("\"   30343\"|\"User                                    \"|\"Test                                    \"|\"J\"|\"Tester                                  \"", data);
    }
}
