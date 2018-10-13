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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.idmunit.IdMUnitException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class SoapClientConnectorTests extends TestCase {
    private SoapClientConnector conn = null;

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(SoapClientConnectorTests.class)); // This file
        suite.addTest(new TestSuite(SoapClientConnectorConfigTests.class));
        return suite;
    }

    protected void setUp() throws Exception {
        conn = new SoapClientConnector();

        Map<String, String> config = new TreeMap<String, String>();
        config.put("user", "testUserName");
        config.put("password", "testPassword");

        conn.setup(config);
    }

    protected void tearDown() throws Exception {
        conn = null;
    }

    //This test fails because the return document re-defines the default namespace in the returned document: "xmlns=\"http://www.kirupafx.com\""
    public void testOpValidate() throws IdMUnitException, KeyManagementException, NoSuchAlgorithmException, URISyntaxException {
        final String request =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://predic8.com/wsdl/IDService/1/\">" +
                        "  <soapenv:Header/>" +
                        "  <soapenv:Body>" +
                        "    <ns:generate>892</ns:generate>" +
                        "  </soapenv:Body>" +
                        "</soapenv:Envelope>";

        final String xpath = "//id";

        //Returned Document:
        //<?xml version="1.0" encoding="utf-8"?><soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema"><soap:Body><GetTop10Response xmlns="http://www.kirupafx.com"><GetTop10Result><string>The Godfather (1972)</string><string>The Shawshank Redemption (1994)</string><string>The Godfather: Part II (1974)</string><string>The Lord of the Rings: The Return of the King (2003)</string><string>Casablanca</string><string>Schindler's List</string><string>Shichinin no samurai (1954)</string><string>Buono, il brutto, il cattivo, Il (1966)</string><string>Pulp Fiction (1994)</string><string>Star Wars: Episode V - The Empire Strikes Back (1980)</string></GetTop10Result></GetTop10Response></soap:Body></soap:Envelope>

        @SuppressWarnings("serial")
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
            //Here is where I found the test URL: http://www.service-repository.com/client/input?operation=GetMovieAtNumber&wsdlURL=http%3A%2F%2Fwww.kirupafx.com%2FWebService%2FTopMovies.asmx%3FWSDL&portType=TopMoviesSoap
                put(SoapClientConnector.ATTR_URL, new ArrayList<String>() {{
                        add("http://www.predic8.com:8080/base/IDService");
                    }});
                put(SoapClientConnector.ATTR_REQUEST, new ArrayList<String>() {{
                        add(request);
                    }});
                put(SoapClientConnector.ATTR_RESPONSE, new ArrayList<String>() {{
                        add(xpath);
                    }});
            }};

        conn.opValidateXpath(data);
    }

    public void testOpRegex() throws IdMUnitException, KeyManagementException, NoSuchAlgorithmException, URISyntaxException {
        final String request =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://predic8.com/wsdl/IDService/1/\">" +
                        "  <soapenv:Header/>" +
                        "  <soapenv:Body>" +
                        "    <ns:generate>892</ns:generate>" +
                        "  </soapenv:Body>" +
                        "</soapenv:Envelope>";
        final String regex = ".+<id>.+</id>.+";
        @SuppressWarnings("serial")
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>() {{
                put(SoapClientConnector.ATTR_URL, new ArrayList<String>() {{
                        add("http://www.predic8.com:8080/base/IDService");
                    }});
                put(SoapClientConnector.ATTR_REQUEST, new ArrayList<String>() {{
                        add(request);
                    }});
                put(SoapClientConnector.ATTR_RESPONSE, new ArrayList<String>() {{
                        add(regex);
                    }});
            }};

        conn.opValidateRegex(data);
    }

    public void testXmlUnitWithNamespaces() throws XpathException, IOException, SAXException {
        final String response =
                "<?xml version='1.0' encoding='UTF-8'?>" +
                        "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                        "<S:Body>" +
                        "<ns2:generateResponse xmlns:ns2=\"http://predic8.com/wsdl/IDService/1/\">" +
                        "<id>892-00002</id>" +
                        "</ns2:generateResponse>" +
                        "</S:Body>" +
                        "</S:Envelope>";

        // for now, just use local-name to deal with ignore namespaces in xpath.
        XMLAssert.assertXpathExists("//*[local-name()='generateResponse']/id/text()", response);

        // TODO: in the future, figure out if there is a way to parse the namespaces out of the dom
        // to build a namespace context map for any given response.
        @SuppressWarnings("serial")
        Map<String, String> nsContextMap = new HashMap<String, String>() {{
                put("ns2", "http://predic8.com/wsdl/IDService/1/");
            }};

        NamespaceContext ctx = new SimpleNamespaceContext(nsContextMap);
        XMLUnit.setXpathNamespaceContext(ctx);
        XMLAssert.assertXpathExists("//ns2:generateResponse/id/text()", response);
    }
}
