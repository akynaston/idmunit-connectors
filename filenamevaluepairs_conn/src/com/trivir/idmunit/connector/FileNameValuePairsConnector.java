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

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Implements an IdMUnit connector for files with name value pairs (java .properties files are an example)
 */

public class FileNameValuePairsConnector extends DTFConnector {

    /**
     * Compares a set of name value pairs to a file's name value pairs.  Note: if there are multiple, non unique keys, the expected and actual order of the values must be identical.
     * i.e.: If I have two values
     * Aaron=Kynaston
     * Aaron=Male
     * they both must appear in this order in the input file, as we simply combine values for non-unique keys.
     *
     * @param expectedAttrs
     * @throws IdMUnitException
     */
    public void opValidateNameValuePairsFile(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        HashMap<String, String> expectedAttrsPairs = new HashMap<String, String>();
        HashMap<String, String> actualAttrsPairs = new HashMap<String, String>();

        String expectedFileContentsRegEx = convertToSingleValueMap(expectedAttrs).get(FLAT_FILE_CONTENTS);
        if (expectedFileContentsRegEx == null) {
            throw new IdMUnitException("All opValidateNameValuePairsFile rows must contain a [" + FLAT_FILE_CONTENTS + "] column - please add the column and the appropriate data, and re-run.");
        }

        for (String nameValuePair : expectedFileContentsRegEx.split("\n")) {
            if (nameValuePair.indexOf("=") < 0) {
                throw new IdMUnitException("Expected name value pair entries must be separated by an '='. Value was: [" + nameValuePair + "].");
            }

            String[] nameValuePairSplit = nameValuePair.split("=");

            String currentValue = expectedAttrsPairs.get(nameValuePairSplit[0]);
            if (currentValue == null) {
                currentValue = "";
            }

            // We had an emtpy value . .store a marker so it's obvious
            if (nameValuePairSplit.length == 1) {
                expectedAttrsPairs.put(nameValuePairSplit[0], "##NOVALUE##" + currentValue);
                continue;
            }

            if (nameValuePairSplit.length != 2) {
                throw new IdMUnitException("Each value in the expected column must be a name value pair \nseparated by an equals sign, only one equals sign per row may exist. \nPlease resolve and restart.\n" +
                        "Attempted to split = on value: [" + nameValuePair + "]");
            }
            expectedAttrsPairs.put(nameValuePairSplit[0], nameValuePairSplit[1] + currentValue);
        }


        File[] candidateFiles = readPath.listFiles(new OnlyFilesFilter());
        if (candidateFiles == null || candidateFiles.length == 0) {
            throw new IdMUnitException("Could not list files, or no files existed at . . .");
        }
        Arrays.sort(candidateFiles, new MostRecentlyModifed());
        File latestFile = candidateFiles[0];
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(latestFile));
        } catch (FileNotFoundException fnfe) {
            throw new IdMUnitException("File was created then deleted, lost access to file: [" + latestFile + "]");
        }

        String temp = null;
        try {
            while ((temp = br.readLine()) != null) {
                String[] nameValuePairSplit = temp.split("=");

                String currentValue = actualAttrsPairs.get(nameValuePairSplit[0]);
                if (currentValue == null) {
                    currentValue = "";
                }


                // We had an emtpy value . .store a zero length string.
                if (nameValuePairSplit.length == 1) {
                    actualAttrsPairs.put(nameValuePairSplit[0], "##NOVALUE##" + currentValue);
                    continue;
                }

                if (nameValuePairSplit.length != 2) {
                    throw new IdMUnitException("Each value in the actual contents must be a name value pair separated by an equals sign, only one equals sign per row may exist. Please resolve and restart.");
                }
                actualAttrsPairs.put(nameValuePairSplit[0], nameValuePairSplit[1] + currentValue);
            }
        } catch (IOException e) {
            throw new IdMUnitException("Failed while reading file: [" + latestFile + "], can not continue!", e);
        }

        try {
            br.close();
        } catch (IOException e) {
            throw new IdMUnitException("Failed to close file", e);
        }

        // Compare HashMaps now:
        Failures failures = new Failures();
        for (String keyExpected : expectedAttrsPairs.keySet()) {
            String expectedValue = expectedAttrsPairs.get(keyExpected);
            String actualValue = actualAttrsPairs.remove(keyExpected);

            if (actualValue == null) {
                failures.add("Expected value: [" + keyExpected + "]:[" + expectedValue + "] did not exist in the actual values retrieved.");
                continue;
            }

            if (!actualValue.matches(expectedValue)) {
                failures.add("Values differ: [" + keyExpected + "]: Expected [" + expectedValue + "] but was: [" + actualValue + "]");
            }
        }

        if (actualAttrsPairs.size() > 0) {
            for (String actualKey : actualAttrsPairs.keySet()) {
                failures.add("Additional actual value not expected: [" + actualKey + "]:[" + actualAttrsPairs.get(actualKey) + "]");
            }

        }

        if (failures.hasFailures()) {
            throw new IdMUnitFailureException(failures.toString());
        }


        /*
        String actualContents = actualfileContentsBuffer.toString().trim(); // trim to remove the last \n
        String clean = actualfileContentsBuffer.toString();
        clean = clean.trim();

        if (!actualContents.toString().matches(expectedFileContentsRegEx)) {

            throw new IdMUnitFailureException("Expected:[" + expectedFileContentsRegEx + "] but was: [" + actualContents + "]");
        }
        */

    }
}
