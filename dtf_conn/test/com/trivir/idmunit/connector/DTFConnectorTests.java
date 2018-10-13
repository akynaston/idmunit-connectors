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
import org.idmunit.IdMUnitException;
import org.idmunit.connector.ConnectionConfigData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Implements an IdMUnit connector for SCPConnector that simulates iDoc format transactions originating from SCPConnector to the SCPConnector IDM Driver
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connector
 */
public class DTFConnectorTests extends TestCase {
    File tempReadFile = null;
    private DTFConnector dtfConnector = null;

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    protected void setUp() throws Exception {
        super.setUp();
        dtfConnector = new DTFConnector();

        ConnectionConfigData configurationData = new ConnectionConfigData("DTF", "com.trivir.idmunit.connector.DTFConnector");
        configurationData.setParam("field-names", "USER ID, Name, FirstName, LastName, Group, Role");
        configurationData.setParam("delimiter", ",");
        configurationData.setParam("write-path", System.getProperty("java.io.tmpdir"));
        configurationData.setParam("description", "Connector to read and write DTF files");
        configurationData.setParam("read-path", System.getProperty("java.io.tmpdir"));

        dtfConnector.setup(configurationData.getParams());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        dtfConnector.tearDown();
    }

    public void testValidate() throws IdMUnitException, IOException {
        tempReadFile = File.createTempFile("tempDTFFileReadTest", ".csv");
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(tempReadFile));
        } catch (IOException e) {
            System.out.println("error:" + e);
        }
        pw.println("ffletcher,FerbFletcher,Ferb,Fletcher,TVShows,Awesome and Funny");
        pw.close();

        Map<String, Collection<String>> expectedAttrs = new HashMap<String, Collection<String>>();
        expectedAttrs.put("USER ID", singleValue("ffletcher"));
        expectedAttrs.put("Name", singleValue("FerbFletcher"));
        expectedAttrs.put("FirstName", singleValue("Ferb"));
        expectedAttrs.put("LastName", singleValue("Fletcher"));
        expectedAttrs.put("Group", singleValue("TVShows"));
        expectedAttrs.put("Role", singleValue("Awesome and Funny"));

        dtfConnector.opValidateObject(expectedAttrs);
    }

}
