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

import org.idmunit.IdMUnitException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class OneIdentityTests {

    private static final String MODULE = "module";
    private static final String SERVER = "server";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String BADTEST_SERVER = "d1im.examp1e.com/badend";
    private static final String TEST_SERVER = "d1im.example.com";
    private static final String JOBID = "jobID";
    private static final String TABLENAME = "tableName";

    //testAuthBadCredentials
    private static final Map<String, String> CONFIG;
    private static final Map<String, String> BADCONFIG;

    static {
        CONFIG = new HashMap<String, String>();
        CONFIG.put(SERVER, TEST_SERVER);
        CONFIG.put(USER, "trivir");
        CONFIG.put(PASSWORD, "Password1");
        CONFIG.put(MODULE, "DialogUser");
    }

    static {
        BADCONFIG = new HashMap<String, String>();
        BADCONFIG.put(SERVER, BADTEST_SERVER);
        BADCONFIG.put(USER, "trivir1");
        BADCONFIG.put(PASSWORD, "Password2");
    }

    private OneIdentityConnector connector;


    @Before
    public void setUp() throws Exception {
        connector = new OneIdentityConnector();
        connector.setup(CONFIG);
    }

    @Test
    public void testStartJobNoJobID() throws IdMUnitException {
        Map<String, Collection<String>> badAttrs = new HashMap<String, Collection<String>>();
        badAttrs.put(TABLENAME, singleValue("DPRProjectionStartInfo"));
        try {
            connector.opStartJob(badAttrs);
        } catch (IdMUnitException e) {
            assertEquals("No value specified for 'JobID'", e.getMessage().trim());
        }
    }

    @Test
    public void testStartJob() throws IdMUnitException {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put(JOBID, singleValue("CCC-CEE82348B3B4064F831F1130E21882BA"));
        attrs.put(TABLENAME, singleValue("DPRProjectionStartInfo"));

        connector.opStartJob(attrs);
        //status of 200 provides no exception
        //we have to maually go check the job queue to see the job being started but
        // were trusting that no error means it communicated and started properly
    }

    @Test
    public void testValidateUser() throws Exception {
        Map<String, Collection<String>> attrs = new HashMap<String, Collection<String>>();
        attrs.put("cn", singleValue("aabbass"));
        attrs.put("DisplayName", singleValue("aabbass"));
        attrs.put("tableName", singleValue("ADSAccount"));
        attrs.put("UserPrincipalName", singleValue("aabbass@example.com"));

        connector.opValidateObject(attrs);
    }

    private static Collection<String> singleValue(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    private static Collection<String> multiValue(String... values) {
        return Arrays.asList(values);
    }
}
