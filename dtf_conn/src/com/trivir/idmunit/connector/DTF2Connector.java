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

import au.com.bytecode.opencsv.CSVReader;
import com.jcraft.jsch.HostKey;
import org.idmunit.Failures;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class DTF2Connector extends BasicConnector {
    // This particular setting doesn't appear to be configurable in DTF driver, so we'll just hardcode it here:
    static final String DTF_DRIVER_DEFAULT_TMP_PREFIX = ".tmp";
    static final String SSH_HOST = "server";
    static final String SSH_HOST_KEY = "host-key";
    static final String SSH_HOST_KEY_TYPE = "host-key-type";
    static final String SSH_PORT = "port";
    static final String SSH_USER = "user";
    static final String SSH_PASSWORD = "password";
    static final String READ_PATH = "read-path";
    static final String WRITE_PATH = "write-path";
    static final String DELIM = "delimiter";
    static final String ROW_KEY = "row-key";
    static final String OUTPUT_FILE_EXT = "output-file-ext";
    static final String FIELD_DEFINITIONS = "field-definitions";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String DEFAULT_DELIMITER = ",";
    private static final int DEFAULT_SSH_PORT = 22;
    private static final String STR_SUCCESS = "...SUCCESS";
    private static Logger log = LoggerFactory.getLogger(DTF2Connector.class);
    private String delimiter = null;
    private String rowKey = null;
    private String outputFileExt = null;
    private LinkedHashMap<String, HeaderItem> fieldDefinitions = null;

    private int rowKeyIndex = -1;

    private DtfAggregator dtfAggregator = null;
    private FileUtil fileUtil;
    private LinkedList<String[]> cachedRows = new LinkedList<String[]>();

    // Items needed for compareAttribute as copied from LdapConnector:
    private boolean insensitive = false;

    private static Map<String, String> convertToSingleValueMap(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        Map<String, String> ret = new HashMap<String, String>();
        for (String key : expectedAttrs.keySet()) {
            ret.put(key, ConnectorUtil.getSingleValue(expectedAttrs, key));
        }
        return ret;
    }

    private static String[] trimSpaces(String[] data) {
        String[] results = new String[data.length];

        for (int i = 0; i < data.length; ++i) {
            results[i] = data[i].trim();
        }

        return results;
    }

    /*
     * Parses fields from IdMUnit config file:
     *         //"<field-definitions>USER ID(30), Name(10), FirstName(20), LastName(20), Group(3), Role(5)</field-definitions>"
     *
     */
    private static LinkedHashMap<String, HeaderItem> parseCSVHeaders(String fieldHeaders) {
        // Using TreeMap, to keep headers in the same order as the file; this helps keeps error reports in the same order as the file.
        LinkedHashMap<String, HeaderItem> headerValues = new LinkedHashMap<String, HeaderItem>();
        String[] fields = fieldHeaders.split(",");

        // Header indexes are 0 based to match Java's 0 based arrays:
        int ctr = 0;
        for (String field : fields) {
            // Splitting out Field length, example: USER ID(30) - name = USER ID, width = 30
            String[] nameAndWidth = field.split("\\(|\\)");
            String name = nameAndWidth[0].trim();
            Integer width;
            if (nameAndWidth.length > 1) {
                width = Integer.parseInt(nameAndWidth[1].trim());
            } else {
                width = -1;
            }
            headerValues.put(name, new HeaderItem(name, ctr++, width));
        }
        return headerValues;
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        String readPathTemp;
        String writePathTemp;
        readPathTemp = config.get(READ_PATH);
        writePathTemp = config.get(WRITE_PATH);
        delimiter = config.get(DELIM);
        rowKey = config.get(ROW_KEY);
        outputFileExt = config.get(OUTPUT_FILE_EXT);

        if (readPathTemp == null) {
            throw new IdMUnitException("'" + READ_PATH + "' not configured, or path does not exist.");
        }

        if (writePathTemp == null) {
            throw new IdMUnitException("'" + WRITE_PATH + "' not configured, or path does not exist.");
        }

        if (delimiter == null) {
            delimiter = DEFAULT_DELIMITER;
        }

        if (rowKey == null) {
            throw new IdMUnitException("'" + ROW_KEY + "' not configured");
        }

        if (outputFileExt == null) {
            throw new IdMUnitException("'" + OUTPUT_FILE_EXT + "' not configured");
        }

        String fieldHeaders = config.get(FIELD_DEFINITIONS);
        if (fieldHeaders == null || !fieldHeaders.contains(",")) {
            throw new IdMUnitException("'" + FIELD_DEFINITIONS + "' not configured, or is missing a comma between fields");
        } else {
            fieldDefinitions = parseCSVHeaders(fieldHeaders);
        }

        // Confirm the rowKey value is actually refers to one of the field definitions:
        HeaderItem keyHeaderField = fieldDefinitions.get(rowKey);
        if (keyHeaderField == null) {
            throw new IdMUnitException("The '" + ROW_KEY + "' value must be equal to one of the defined fields in the '" + FIELD_DEFINITIONS + "'.");
        }
        rowKeyIndex = keyHeaderField.index;

        // TODO: test: only have one field . . .
        // TODO: test: have different delimiter for field names . . .decide if this is a bad requirement

        String host = config.get(SSH_HOST);
        if (host != null) {
            int port = DEFAULT_SSH_PORT;
            if (config.containsKey(SSH_PORT)) {
                port = Integer.parseInt(config.get(SSH_PORT));
            }

            String user = config.get(SSH_USER);
            String password = config.get(SSH_PASSWORD);
            if (user == null || password == null) {
                throw new IdMUnitException("'" + SSH_USER + "', '" + SSH_PASSWORD + "' must be configured if '" + SSH_HOST + "' is configured.");
            }

            byte[] hostKey = null;
            String hostKeyBase64 = config.get(SSH_HOST_KEY);
            if (hostKeyBase64 != null) {
                try {
                    hostKey = new BASE64Decoder().decodeBuffer(hostKeyBase64);
                } catch (IOException e) {
                    throw new IdMUnitException("Error decoding " + SSH_HOST_KEY, e);
                }
            }

            int hostKeyType = -1;
            String type = config.get(SSH_HOST_KEY_TYPE);
            if (type != null) {
                if ("ssh-dss".equals(type)) {
                    hostKeyType = HostKey.SSHDSS;
                } else if ("ssh-rsa".equals(type)) {
                    hostKeyType = HostKey.SSHRSA;
                }
            }

            fileUtil = new RemoteFileUtil(host, port, hostKeyType, hostKey, user, password, readPathTemp, writePathTemp);
        } else {
            File readPath = new File(readPathTemp);
            if (!readPath.exists()) {
                throw new IdMUnitException("'" + READ_PATH + "' not configured, or path does not exist.");
            }

            File writePath = new File(writePathTemp);
            if (!writePath.exists()) {
                throw new IdMUnitException("'" + WRITE_PATH + "' not configured, or path does not exist.");
            }

            fileUtil = new LocalFileUtil(readPathTemp, writePathTemp);
        }
        dtfAggregator = new DtfAggregator(fileUtil, outputFileExt);
    }

    public void teardown() {
        fileUtil.close();
    }

    public void opAdd(Map<String, Collection<String>> data) throws IdMUnitException {
        StringBuilder row = new StringBuilder();
        for (Iterator<HeaderItem> i = fieldDefinitions.values().iterator(); i.hasNext(); ) {
            HeaderItem fieldDef = i.next();
            Collection<String> values = data.get(fieldDef.name);
            String value = "";
            if (values != null) {
                if (values.size() == 1) {
                    value = values.iterator().next();
                } else if (values.size() > 0) {
                    throw new IdMUnitException("Multiple values not supported in a single field (" + fieldDef.name + ")");
                }
            }
            if (fieldDef.fieldWidth == -1) {
                row.append(value);
            } else {
                String formatStr = "%-" + fieldDef.fieldWidth + "s";
                row.append(String.format(formatStr, value));
            }
            if (i.hasNext()) {
                row.append(delimiter);
            }
        }
        row.append(LINE_SEPARATOR);

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss.SSS");
        String filename = dateFormatter.format(new Date()) + outputFileExt;

        fileUtil.writeFile(filename, row.toString());
    }

    public void opValidate(Map<String, Collection<String>> data) throws IdMUnitException {
        Map<String, String> expectedValues = convertToSingleValueMap(data);

        String expectedKeyValue = expectedValues.get(rowKey);
        if (expectedKeyValue == null) {
            throw new IdMUnitException("ERROR: Validations must include the specified row key to properly lookup data in the resulting file!");
        }

        Failures failures = new Failures();
        boolean foundAtLeastOneRow = false;

        updateCachedRows();
        int successfulMatches = 0;

        for (String[] rowData : cachedRows) {
            successfulMatches = 0; // we're starting a new row; so reset the counter.
            if (rowData[rowKeyIndex].equalsIgnoreCase(expectedKeyValue)) {
                foundAtLeastOneRow = true;
                // We found a row to validate, Loop through the expected values:
                // compare each (actual) rowData value with the expected set
                for (String header : expectedValues.keySet()) {
                    String expectedValue = expectedValues.get(header);
                    int fieldIndex = fieldDefinitions.get(header).index;
                    if (fieldIndex >= rowData.length) {
                        throw new IdMUnitException("Found a row with not enough fields");
                    }
                    String actualValue = rowData[fieldIndex];

                    Pattern p = Pattern.compile(expectedValue, insensitive ? Pattern.CASE_INSENSITIVE : Pattern.DOTALL);
                    if (p.matcher(actualValue).matches()) {
                        log.info(STR_SUCCESS + ": validating attribute: [" + header + "] EXPECTED: [" + expectedValue + "] ACTUAL: [" + actualValue + "]");
                        successfulMatches++;
                        if (successfulMatches == rowData.length) {
                            // If we succeeded on every row in this validation, exit now!
                            break;
                        }
                    } else {
                        failures.add(header + " " + "expected:<[" + expectedValue + "]> but was:<[" + actualValue + "]>");
                    }
                }

                if (failures.hasFailures()) {
                    throw new IdMUnitFailureException(failures.toString());
                }
            }
        }
        if (!foundAtLeastOneRow) {
            throw new IdMUnitFailureException("None of the new data written out matched the expected key from the spreadsheet!");
        }
    }

    private void updateCachedRows() throws IdMUnitException {
        InputStream is;
        is = dtfAggregator.getInputStream();

        List<String[]> data;
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(is), delimiter.charAt(0));
            // TODO: this has a problem when the data is quoted: the quotes are removed when read, so are not included in the data.
            data = reader.readAll();
            reader.close();
        } catch (IOException e) {
            throw new IdMUnitException("Error reading data", e);
        }

        // Store what we read into the cache, but store it in reverse order, so the latest data is always first.
        for (String[] row : data) {
            cachedRows.addFirst(trimSpaces(row));
        }
    }

    private static class HeaderItem {
        final String name;
        final int index;
        final int fieldWidth;

        HeaderItem(String name, int index, int fieldWidth) {
            this.name = name;
            this.index = index;
            this.fieldWidth = fieldWidth;
        }
    }
}
