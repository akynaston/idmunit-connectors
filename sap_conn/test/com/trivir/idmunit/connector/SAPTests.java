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

import java.util.*;

/**
 * Implements an IdMUnit connector for SAP that simulates iDoc format transactions originating from SAP to the SAP IDM Driver
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 * @see org.idmunit.connector.Connector
 */
public class SAPTests extends TestCase {
    private static final String SAP_TEMPLATE_FILE = "idocTemplates\\orgRoleAdd.idoc";
    private static final String SAP_IDOC_CACHE_DIRECTORY = "idocCache";
    private SAP sapConnectorInstance;


    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    protected void setUp() throws Exception {
        super.setUp();
        sapConnectorInstance = new SAP();
        ConnectionConfigData configurationData = new ConnectionConfigData("SAP", "org.idmunit.connector.SAP");
        configurationData.setParam(SAP.ENABLE_SCP, "false");
        configurationData.setParam(SAP.SCP_PROFILE, "none");
        configurationData.setParam(SAP.SAP_CLIENT_NUMBER, "030");
        configurationData.setParam(SAP.INITIAL_IDOC_DATA_OFFSET, "64");
        configurationData.setParam(SAP.IDOC_LOCAL_PATH, "idocCache");
        configurationData.setParam(SAP.IDOC_FILENAME_PREFIX, "0000000000");
        configurationData.setParam(SAP.IDOC_FILE_EXTENSION, "idmunit");
        sapConnectorInstance.setup(configurationData.getParams());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        sapConnectorInstance.tearDown();
    }

    public void testGetSAPClientNumber() throws IdMUnitException {
        assertEquals(sapConnectorInstance.getSAPUserClientNumber(), "030");
    }

    public void testCreateGoodIDocGeneration() throws IdMUnitException {
        System.out.println("\n\n\n\n\n####### Testing good IDoc generation #######\n\n\n\n");
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("IDocTemplate", singleValue(SAP_TEMPLATE_FILE));
        data.put("dn", singleValue("ORGROLID=60005100"));
        try {
            sapConnectorInstance.opCreateIdoc(data);
        } catch (IdMUnitException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
        //TODO: Validate new IDoc in the file system, read console trace to validate in the mean time
    }

    public void testCreateGoodIDocGenerationWithSingleDataAttr() throws IdMUnitException {
        System.out.println("\n\n\n\n\n####### Testing good IDoc generation #######\n\n\n\n");
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("IDocTemplate", singleValue(SAP_TEMPLATE_FILE));
        String keyValue = "ORGROLID";
        String replaceValue = "03030303";
        data.put("dn", singleValue(keyValue + "=" + replaceValue));
        // This description attr should be used to overwrite the templates description attr which contains "DATA SECURITY ANALYST"
        // at offset 98 (length 40)
        String descriptionAttrSAPName = "P1000:STEXT:none:98:40";
        String descriptionAttrValue = "TSTOrgRoleDescription";
        String originalDescriptionValueInTemplate = "DATA SECURITY ANALYST";
        data.put(descriptionAttrSAPName, singleValue(descriptionAttrValue));
        try {
            List<String> generatedIDoc = sapConnectorInstance.opCreateIdocIMPL(data);
            for (String dataRow : generatedIDoc) {
                if (dataRow.contains(keyValue)) {
                    fail("Not all Key values [" + keyValue + "] were replaced with [" + replaceValue + "].");
                }
                if (dataRow.contains(originalDescriptionValueInTemplate)) {
                    fail("Not all attr values [" + descriptionAttrSAPName + "] were replaced with [" + descriptionAttrValue + "].");
                }
            }

        } catch (IdMUnitException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
        //TODO: Validate new IDoc in the file system, read console trace to validate in the mean time
    }

    public void testHandleBadColumnDataGracefully() throws IdMUnitException {
        System.out.println("\n\n\n\n\n####### Testing good IDoc generation with a bad column header #######\n\n\n\n");
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("IDocTemplate", singleValue(SAP_TEMPLATE_FILE));
        String keyValue = "ORGROLID";
        String replaceValue = "03030303";
        data.put("dn", singleValue(keyValue + "=" + replaceValue));
        /* This description attr should be used to overwrite the templates description attr which contains "DATA SECURITY ANALYST"
         *     at offset 98 (length 40)*/
        String descriptionAttrSAPName = "P1000:STEXT:none:98"; //Leaving off the length component to make this column in valid (readable exception should be thrown)
        String descriptionAttrValue = "TSTOrgRoleDescription";
        String originalDescriptionValueInTemplate = "DATA SECURITY ANALYST";
        data.put(descriptionAttrSAPName, singleValue(descriptionAttrValue));
        try {
            List<String> generatedIDoc = sapConnectorInstance.opCreateIdocIMPL(data);
            for (String dataRow : generatedIDoc) {
                if (dataRow.contains(keyValue)) {
                    fail("Not all Key values [" + keyValue + "] were replaced with [" + replaceValue + "].");
                }
                if (dataRow.contains(originalDescriptionValueInTemplate)) {
                    fail("Not all attr values [" + descriptionAttrSAPName + "] were replaced with [" + descriptionAttrValue + "].");
                }
            }
            fail("Should have thrown a readable exception");
        } catch (IdMUnitException e) {
            e.printStackTrace();
            assertTrue(e.getMessage().startsWith("Failed to parse data column"));
        }
        //TODO: Validate new IDoc in the file system, read console trace to validate in the mean time
    }

    public void testCreateIdocNoTemplateSupplied() throws IdMUnitException {
        try {
            Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
            sapConnectorInstance.opCreateIdoc(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("Please include your full IDoc template path in a column titled [IDocTemplate] to generate SAP transactions.", e.getMessage());
        }
    }

    public void testCreateIdocNoDnSupplied() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("IDocTemplate", singleValue(SAP_TEMPLATE_FILE));
        try {
            sapConnectorInstance.opCreateIdoc(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertEquals("Please include your template key and unique SAP ID in a column titled [dn] in order to process the SAP IDoc template called [IDocTemplate]. For example: EMPLOYID=12345678", e.getMessage());
        }
    }

    public void testCreateBadIDocTemplateFilePath() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("IDocTemplate", singleValue("C:/idmunit/idocCache/NONExistantFile.idoc"));
        data.put("dn", singleValue("ORGROLID=60005100"));
        try {
            sapConnectorInstance.opCreateIdoc(data);
            fail("Should have thrown exception");
        } catch (IdMUnitException e) {
            assertTrue(e.getMessage().startsWith("Failed to process the specified SAP template file"));
        }
    }

// TODO: FIXME
// Uses SAP.replaceAssociationKeyWithObjectGUID(List<String>,String) which no longer exists
// Should it be refactored to use SAP.replaceAssociationKeyWithObjectGUID(List<String>,String,Map<String,Collection<String>>)?
//    public void testReplaceAssociationKeyWithObjectGUID() throws IdMUnitException {
//        List<String> dataList = new LinkedList<String>();
//        dataList.add("test1 REPLACEME String");
//        dataList.add("REPLACEMEtest2 REPLACEME StringREPLACEME");
//        dataList.add("REPLACEMEtest3 REPLACEME String");
//        dataList.add("test4 REPLACEME StringREPLACEME");
//
//        sapConnectorInstance.replaceAssociationKeyWithObjectGUID(dataList, "REPLACEME=NewValue");
//        for(int ctr=0;ctr<dataList.size();++ctr) {
//            assertFalse(dataList.get(ctr).contains("REPLACEME"));
//        }
//    }


    public void testLoadFileData() throws IdMUnitException {
        List<String> fileData = sapConnectorInstance.loadFileData(SAP_TEMPLATE_FILE);
        if (fileData.size() < 1) {
            fail("No data read from specified template file.");
        }
    }

    public void testWriteFileData() throws IdMUnitException {
        List<String> fileData = sapConnectorInstance.loadFileData(SAP_TEMPLATE_FILE);
        if (fileData.size() < 1) {
            fail("No data read from specified template file.");
        }

        SAP.writeFile(SAP_IDOC_CACHE_DIRECTORY + "JUnitTestOutput1.idoc", fileData);

        //Validate the freshly written data
        List<String> idocData = sapConnectorInstance.loadFileData(SAP_TEMPLATE_FILE);
        if (idocData.size() < 1) {
            fail("No data read from generated IDoc template file.");
        }
    }

    public void testFieldRightPadding() throws IdMUnitException {
        System.out.println("\n\nPadding: " + SAP.padRight("test1", 20) + "#");
        System.out.println("\n\nPadding: " + SAP.padRight("test1", 7) + "#");
        System.out.println("\n\nPadding: " + SAP.padRight("test1", 3) + "#");
        System.out.println("\n\nPadding: " + SAP.padRight("test1", 10) + "#");
        if (!(SAP.padRight("test1", 20) + "#").equalsIgnoreCase("test1               #")) {
            fail("Right padding to 20 chars did not work.");
        }
        if (!(SAP.padRight("test2", 7) + "#").equalsIgnoreCase("test2  #")) {
            fail("Right padding to 7 chars did not work.");
        }
        if (!(SAP.padRight("test3", 3) + "#").equalsIgnoreCase("test3#")) {
            fail("Right padding to 3 chars did not work.");
        }
        if (!(SAP.padRight("test4", 10) + "#").equalsIgnoreCase("test4     #")) {
            fail("Right padding to 10 chars did not work.");
        }
    }

    public void testComposeIDocFileName() throws IdMUnitException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();
        data.put("IDocTemplate", singleValue(SAP_TEMPLATE_FILE));
        data.put("dn", singleValue("ORGROLID=60005100"));
        String expectedFileNamePrefix = "O_030_000000000020";
        String expectedFileNameSuffix = ".idmunit";
        String actualFileName = sapConnectorInstance.composeIdocFileName();
        if (actualFileName.indexOf(expectedFileNamePrefix) == -1) {
            fail("The file name prefix expected was [" + expectedFileNamePrefix + "] but this was the name value [" + sapConnectorInstance.composeIdocFileName() + "]");
        }
        if (actualFileName.indexOf(expectedFileNameSuffix) == -1) {
            fail("The file name suffix expected was [" + expectedFileNameSuffix + "] but this was the name value [" + sapConnectorInstance.composeIdocFileName() + "]");
        }
    }

}
