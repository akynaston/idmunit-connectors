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
package com.trivir.idmunit.connector.mail;

import org.idmunit.IdMUnitException;
import org.idmunit.util.LdapConnectionHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.naming.directory.DirContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;

public class TemplateHelper {

    //public static final String XMLNS_FORM = "http://www.novell.com/dirxml/workflow/form";
    private static final String ELEMENT_TOKEN_DESCRIPTION = "token-description";
    private static final String ELEMENT_TOKEN_DESCRIPTIONS = "token-descriptions";

    // NOTE: This should have work, but doesn't. Something about the namespace-uri() call causes problems.
/*    public static final String XPATH_TOKEN_DESCRIPTION = "/html"
            + "*//*[local-name()='" + ELEMENT_TOKEN_DESCRIPTIONS + "' and namespace-uri()='" + XMLNS_FORM + "']"
            + "*//*[local-name()='" + ELEMENT_TOKEN_DESCRIPTION + "' and namespace-uri()='" + XMLNS_FORM + "']";*/

    public static final String XPATH_TOKEN_DESCRIPTION = "/html" +
            "/*[local-name()='" + ELEMENT_TOKEN_DESCRIPTIONS + "']" +
            "/*[local-name()='" + ELEMENT_TOKEN_DESCRIPTION + "']";

    public static Document toDocument(String templateData) throws IdMUnitException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(templateData));
            return builder.parse(is);
        } catch (ParserConfigurationException e) {
            throw new IdMUnitException("Could not parse the template data: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new IdMUnitException("Could not parse the template data: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IdMUnitException("Could not parse the template data: " + e.getMessage(), e);
        }
    }

    public static String getTemplateData(DirContext context, String dn) throws IdMUnitException {
        if (context == null) {
            throw new IllegalArgumentException("Param 'context' is null");
        }

        if (isBlank(dn)) {
            throw new IllegalArgumentException("Param 'dn' is blank");
        }

        Map<String, Collection<String>> templateObj = LdapConnectionHelper.attributesToMap(LdapConnectionHelper.getAttributes(context, dn));
        Collection<String> notfMergeTemplateData = templateObj.get("notfMergeTemplateData");

        // Template Data should not be multi-valued
        if (notfMergeTemplateData.size() == 0) {
            throw new IdMUnitException("Failure retrieving email template");
        } else if (notfMergeTemplateData.size() > 1) {
            throw new IdMUnitException("Failure retrieving email template - notfMergeTemplateData has multiple values - which is not supported");
        }

        return notfMergeTemplateData.iterator().next();
    }

    public static NodeList getNodeList(Document doc, String xpath) throws IdMUnitException {
        if (doc == null) {
            throw new IllegalArgumentException("Param 'doc' is null");
        }

        if (isBlank(xpath)) {
            throw new IllegalArgumentException("Param 'xpath' is blank");
        }

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpathObj = xPathfactory.newXPath();
        try {
            XPathExpression expr = xpathObj.compile(xpath);
            return (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IdMUnitException("Unable to compile XPATH expression '" + xpath + "'", e);
        }
    }

    public static Map<String, String> getTokensAsMap(NodeList nodes) {
        Map<String, String> tokenMap = new HashMap<String, String>();

        if ((nodes == null) || (nodes.getLength() == 0)) {
            return tokenMap;
        }

        for (int n = 0; n < nodes.getLength(); n++) {
            Node node = nodes.item(n);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)node;
                String key;
                String value;

                key = element.getAttribute("item-name");
                if (!isBlank(key)) {
                    value = element.getAttribute("description");
                    if (value == null) {
                        value = "";
                    }

                    tokenMap.put(key, value);
                }
            }
        }

        return tokenMap;
    }

}
