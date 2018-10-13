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
import org.idmunit.IdMUnitFailureException;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class SoapClientConnectorConfigTests extends TestCase {

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testOpRegexWithHTTPRequestHeader() throws IdMUnitException, KeyManagementException, NoSuchAlgorithmException, URISyntaxException {

        // Setup a new connect, but don't add the SOAPAction http header yet:
        SoapClientConnector conn = null;
        Map<String, String> config = new TreeMap<String, String>();
        conn = new SoapClientConnector();
        config.put("user", "testUserName");
        config.put("password", "testPassword");
        conn.setup(config);

        final String request =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://www.restfulwebservices.net/ServiceContracts/2008/01\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ns:GetCitiesByCountry>" +
                        "<ns:Country>USA</ns:Country>" +
                        "</ns:GetCitiesByCountry>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";
        final String regex = ".+<a:string>BOISE</a:string>.+<a:string>PEARL HARBOR</a:string>.+";
        @SuppressWarnings("serial")
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
            // note: http://www.restfulwebs ervices.net/wcf/WeatherForecastService.svc is simply a public service that happens to use an HTTP
            // header of "SOAPAction"; so I've chosen to use it.
                put(SoapClientConnector.ATTR_URL, new ArrayList<String>() {{
                        add("http://www.restfulwebservices.net/wcf/WeatherForecastService.svc");
                    }});
                put(SoapClientConnector.ATTR_REQUEST, new ArrayList<String>() {{
                        add(request);
                    }});
                put(SoapClientConnector.ATTR_RESPONSE, new ArrayList<String>() {{
                        add(regex);
                    }});
            }};
        try {
            conn.opValidateRegex(data);
            fail("Should have thrown an IdMUnitFailureException, the HTTP SOAPAction with the value of 'GetCitiesByCountry' was missing, so we should have recieved an ActionNotSupported soap fault.");
        } catch (IdMUnitFailureException e) {
            // We just want to see that we failed due to the missing SOAP action header:
            String expected = "ActionNotSupported";
            String actual = e.getMessage();
            assertTrue("Expected string containing '" + expected + "'. Got: " + actual, actual.indexOf(expected) > 0);
        }

        // Now simulate some one fixing their idmunit-config.xml by adding the httpRequestHeader column names setting:
        config.put("httpRequestHeaders", "SOAPAction");
        conn.setup(config);
        // Add the required header value:
        data.put("SOAPAction", Arrays.asList(new String[]{"GetCitiesByCountry"}));

        conn.opValidateRegex(data);

    }

}
