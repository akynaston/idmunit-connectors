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

package com.trivir.idmunit.injector;

import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.injector.Injection;

public class URIFromQueryFilterInjectionTest extends TestCase {
    private static final String TEST_SERVER = "10.10.30.249";
    private static final String TEST_SERVER_PORT = "8080";

    private Injection injector;


    @Override
    protected void setUp() throws Exception {
        injector = new URIFromQueryFilterInjection();
    }

    public void testSingleResult() throws IdMUnitException {
        String jsonConfig = "{" +
                " 'host' : '" + TEST_SERVER + "'," +
                " 'port' : '" + TEST_SERVER_PORT + "'," +
                " 'systemObject' : 'managed/user'," +
                " 'queryFilter' : 'userName+eq+\"testuser\"'," +
                " 'useSSL' : false," +
                " 'username' : 'openidm-admin'," +
                " 'password' : 'openidm-admin'" +
                " }";
        injector.mutate(jsonConfig);

        injector.getDataInjection(null);
    }
}
