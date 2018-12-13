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
import org.idmunit.util.LdapConnectionHelper;

import javax.mail.MessagingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SMTPServerTemplateTests extends MailTemplateTestBase {
    private static final int SMTP_PORT = ConfigTests.DEFAULT_SMTP_STANDALONE_PORT;

    protected void setUp() throws Exception {
        // server is for setup and tear down usage, connector is for test usage.
        mailConnector = new SMTPServer();

        Map<String, String> config = new HashMap<String, String>();
        config.put(MailHelper.CONFIG_PORT, Integer.toString(SMTP_PORT));
        config.put(MailHelper.CONFIG_RECIPIENT, defaultRecipient);
        config.putAll(ConfigTests.getDefaultLdapConfig(LdapConnectionHelper.LDAP_CONFIG_PREFIX));
        mailConnector.setup(config);

        ldapConnector = ConfigTests.newLdapConnector();

        //delete and create the email template
        Map<String, Collection<String>> attrs = ConfigTests.newTemplateAddAttrs();

        ldapConnector.opDeleteObject(attrs);
        ldapConnector.opAddObject(attrs);
    }

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

    protected void sendMessage(String to, String from, String subject, String body) throws MessagingException {
        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(SMTP_PORT));
        MailTestHelper.sendMessage(smtpConfig, to, from, subject, body);
    }

}
