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

import com.trivir.idmunit.connector.mail.MailHelper;
import com.trivir.idmunit.connector.mail.MailTestHelper;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.BasicConnector;
import org.idmunit.util.LdapConnectionHelper;

import javax.mail.*;
import java.util.*;

import static org.idmunit.connector.ConnectorUtil.addSingleValue;

public class IMAPConnectorTemplateTests extends MailTemplateTestBase {

    @Override
    protected void setUp() throws Exception {
        // Server is for setup and tear down usage, connector is for test usage.
        mailConnector = new IMAPConnector();

        Map<String, String> config = new HashMap<String, String>();
        config.put(BasicConnector.CONFIG_SERVER, ConfigTests.DEFAULT_IMAP_HOST);
        config.put(BasicConnector.CONFIG_USER, ConfigTests.DEFAULT_IMAP_USER);
        config.put(BasicConnector.CONFIG_PASSWORD, ConfigTests.DEFAULT_IMAP_PASSWORD);
        config.put(MailHelper.CONFIG_PORT, String.valueOf(ConfigTests.DEFAULT_IMAP_PORT));
        config.put(MailHelper.CONFIG_SSL, ConfigTests.DEFAULT_IMAP_SSL);
        config.putAll(ConfigTests.getDefaultLdapConfig(LdapConnectionHelper.LDAP_CONFIG_PREFIX));
        //mailConnector.setup(config);
        //mailConnector.execute("DeleteMail", Collections.<String, Collection<String>>emptyMap());

        deliveryPause = true; // in MailTestBase. Needed for IMAP tests.

        ldapConnector = ConfigTests.newLdapConnector();

        //delete the email template
        try {
            ldapConnector.opDeleteObject(ConfigTests.newTemplateDeleteAttrs());
        } catch (IdMUnitException e) {
            //ignore
        }

        //create the email template
        ldapConnector.opAddObject(ConfigTests.newTemplateAddAttrs());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            mailConnector.tearDown();
        } catch (IdMUnitException e) {
            //ignore
        }

        //delete the email template
        try {
            ldapConnector.opDeleteObject(ConfigTests.newTemplateDeleteAttrs());
        } catch (IdMUnitException e) {
            //ignore
        }

        try {
            ldapConnector.tearDown();
        } catch (IdMUnitException e) {
            //ignore
        }
    }

    @Override
    protected void sendMessage(String to, String from, String subject, String body) throws MessagingException {
        MailTestHelper.sendMessage(
                ConfigTests.newDefaultSmtpConfig(),
                to,
                from,
                subject,
                body);
    }

    public void testValidateUnrecognizedAttrs() throws MessagingException, IdMUnitException {

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "Bogus", "value");
        addSingleValue(data, "Unvalued1", null);
        addSingleValue(data, "Unvalued2", "");

        // Legitimate values
        addSingleValue(data, "Mailbox", "value");
        addSingleValue(data, "To", "value");
        addSingleValue(data, "From", "value");
        addSingleValue(data, "Subject", "value");
        addSingleValue(data, "Body", "value");
        addSingleValue(data, "Template", ConfigTests.DEFAULT_LDAP_TEMPLATE_DN);
        addSingleValue(data, "UserFullName", "value");
        addSingleValue(data, "UserGivenName", "value");
        addSingleValue(data, "UserLastName", "value");
        addSingleValue(data, "ConnectedSystemName", "value");
        addSingleValue(data, "FailureReason", "value");

        try {
            mailConnector.execute("Validate", data);
            fail("Expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("Unsupported attribute(s): [Bogus]", e.getMessage());
        }
    }

    public void testXmlTemplateParsing() throws IdMUnitException {
        String template = ConfigTests.BASIC_XML_TEMPLATE;
        String parsedTemplate = MailCompare.normalizeText(template);
        assertEquals(parsedTemplate, ConfigTests.HTML_RESULT_FROM_XML);
    }

    //NOT WORKING At the moment.
//    public void testValidateXmlTemplateWithLinks() throws IdMUnitException {
//        String template = ConfigTests.XML_TEMPLATE_LINKS_WITH_SEMICOLONS;
//        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
//
//        // Legitimate values
//        addSingleValue(data, "Mailbox", "value");
//        addSingleValue(data, "To", "value");
//        addSingleValue(data, "From", "value");
//        addSingleValue(data, "Subject", "value");
//        addSingleValue(data, "Body", "value");
//        addSingleValue(data, "Template", template);
//        addSingleValue(data, "UserFullName", "value");
//        addSingleValue(data, "UserGivenName", "value");
//        addSingleValue(data, "UserLastName", "value");
//        addSingleValue(data, "ConnectedSystemName", "value");
//        addSingleValue(data, "FailureReason", "value");
//
//        try {
//            mailConnector.execute("Validate", data);
//            fail("Expected an exception to be thrown");
//        } catch (IdMUnitException e) {
//            assertEquals("Unsupported attribute(s): [Bogus]", e.getMessage());
//        }
//    }
//
//    public void testXmlParsingWithLinks() {
//        String template = ConfigTests.XML_TEMPLATE_LINKS_WITH_SEMICOLONS;
//    }
}
