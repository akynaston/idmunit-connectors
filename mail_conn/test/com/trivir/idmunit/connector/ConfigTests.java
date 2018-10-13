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
import org.idmunit.IdMUnitException;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.LdapConnector;
import org.idmunit.util.KeyStoreHelper;
import org.idmunit.util.LdapConnectionHelper;

import java.io.File;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

public class ConfigTests {

    //TODO: Create test Google accounts on the fly and clean them up
    //TODO: Create a second valid email account for To and CC regex tests with multiple recipients to avoid
    // mail delivery errors during testing (they complicate testing results)

    static final int DEFAULT_SMTP_STANDALONE_PORT = 1025;

    static final String DEFAULT_SENDER = "test@franklin.ro";
    static final String DEFAULT_RECIPIENT = "test@franklin.ro";

    static final String KEYSTORE = "idmunit_mail_conn_keystore";
    static final String KEY_STORE_PASSPHRASE = "changeit";

//------------------
    //EXTERNAL MAIL CONFIGURATION (Google)

    //Google IMAP server
    static final String DEFAULT_IMAP_HOST = "imap.gmail.com";
    static final String DEFAULT_IMAP_USER = "test@franklin.ro";
    static final String DEFAULT_IMAP_PASSWORD = "thisisa";
    static final int DEFAULT_IMAP_PORT = 993;
    static final String DEFAULT_IMAP_SSL = "true";

    // Google SMTP server
    static final String DEFAULT_SMTP_HOST = "smtp.gmail.com";
    static final String DEFAULT_SMTP_USER = "test@franklin.ro";
    static final String DEFAULT_SMTP_PASSWORD = "thisisa";
    static final int DEFAULT_SMTP_PORT = 587;
    static final String DEFAULT_SMTP_SSL = "true";

//------------------
    //LOCAL MAIL CONFIGURATION

/*    // Local IMAP server
    // NOTE: If Greenmail, the user must exist in the users.properties file
    static final String DEFAULT_IMAP_HOST = MailHelper.HOST_LOCALHOST;
    static final String DEFAULT_IMAP_USER = "test@franklin.ro";
    static final String DEFAULT_IMAP_PASSWORD = "thisisa";
    static final int DEFAULT_IMAP_PORT = MailHelper.IMAP_PORT_STANDARD;
    static final String DEFAULT_IMAP_SSL = "false";

    // Local SMTP server
    // NOTE: If Greenmail, the user must exist in the users.properties file
    static final String DEFAULT_SMTP_HOST = MailHelper.HOST_LOCALHOST;
    static final String DEFAULT_SMTP_USER = "test@franklin.ro";
    static final String DEFAULT_SMTP_PASSWORD = "thisisa";
    static final int DEFAULT_SMTP_PORT = MailHelper.SMTP_PORT_STANDARD;
    static final String DEFAULT_SMTP_SSL = "false";*/

//------------------
    //LDAP CONFIGURATION

    // Data center LDAP server
    static final String DEFAULT_LDAP_HOST = "10.10.30.249";
    static final int DEFAULT_LDAP_PORT = 636;
    static final String DEFAULT_LDAP_SERVER = DEFAULT_LDAP_HOST + ":" + DEFAULT_LDAP_PORT;
    static final String DEFAULT_LDAP_USER = "cn=admin,o=services";
    static final String DEFAULT_LDAP_PASSWORD = "trivir";
    static final String DEFAULT_LDAP_TRUST_ALL_CERTS = "true";
    static final String DEFAULT_LDAP_USE_TLS = "true";

/*    // Local LDAP server
    static final String DEFAULT_LDAP_HOST = "172.17.2.61";
    static final int DEFAULT_LDAP_PORT = 636;
    static final String DEFAULT_LDAP_SERVER = DEFAULT_LDAP_HOST + ":" + DEFAULT_LDAP_PORT;
    static final String DEFAULT_LDAP_USER = "cn=admin,o=services";
    static final String DEFAULT_LDAP_PASSWORD = "trivir";
    static final String DEFAULT_LDAP_TRUST_ALL_CERTS = "true";
    static final String DEFAULT_LDAP_USE_TLS = "true";*/

//------------------
    //TEMPLATE CONFIGURATION

    static final String DEFAULT_LDAP_TEMPLATE_CN = "IdMUnit Mail Connector Test Email Template";
    static final String DEFAULT_LDAP_TEMPLATE_DN = "cn=" + DEFAULT_LDAP_TEMPLATE_CN + ",cn=Default Notification Collection,cn=Security";

    static final String DEFAULT_TEMPLATE_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns:form=\"http://www.novell.com/dirxml/workflow/form\">\n" +
            "\t<form:token-descriptions>\n" +
            "\t\t<form:token-description description=\"The user's full name\" item-name=\"UserFullName\"/>\n" +
            "\t\t<form:token-description description=\"The user's given name\" item-name=\"UserGivenName\"/>\n" +
            "\t\t<form:token-description description=\"The user's last name\" item-name=\"UserLastName\"/>\n" +
            "\t\t<form:token-description description=\"The external applicaton name\" item-name=\"ConnectedSystemName\"/>\n" +
            "\t\t<form:token-description description=\"The failure reason\" item-name=\"FailureReason\"/>\n" +
            "\t</form:token-descriptions>\n" +
            "\t<head>\n" +
            "\t\t<title>Notice of Password Reset Failure</title>\n" +
            "\t\t<style>\n" +
            "\t\t\t<!-- body { font-family: Trebuchet MS } -->\n" +
            "\t\t</style>\n" +
            "\t</head>\n" +
            "\t<body BGCOLOR=\"#FFFFFF\">\n" +
            "\t\t<p>Dear $UserFullName$,</p>\n" +
            "\t\t<p>This is a notice that your password could not be reset in the $ConnectedSystemName$ system..  The reason for failure is indicated below:</p>\n" +
            "\t\t<p>Reason: $FailureReason$</p>\n" +
            "\t\t<p>If you have any further questions,\n" +
            "     please contact the help desk at (012) 345-6789 or email\n" +
            "     at <a href=\"mailto:help.desk@mycompany.com\">\n" +
            "     help.desk@mycompany.com </a>\n" +
            "\t\t</p>\n" +
            //"\t\t<p>this message originated from a2idmv3</p>\n" +
            "\t\t<p> - Automated Security</p>\n" +
            "\t\t<p>\n" +
            "\t\t\t<img ALT=\"Powered by Novell\" height=\"29\" width=\"80\"/>\n" +
            "\t\t</p>\n" +
            "\t</body>\n" +
            "</html>";

//------------------

    static LdapConnector newLdapConnector() throws Exception {
        return newLdapConnector(null);
    }

    static LdapConnector newLdapConnector(Map<String, String> config) throws Exception {
        if ((config == null) || config.isEmpty()) {
            config = getDefaultLdapConfig();
        }

        KeyStoreHelper.writeCertificatesToKeyStore(
                KEYSTORE,
                KEY_STORE_PASSPHRASE.toCharArray(),
                "test",
                DEFAULT_LDAP_HOST, DEFAULT_LDAP_PORT);

        LdapConnector connector;

        try {
            connector = new LdapConnector();
            connector.setup(config);
            return connector;
        } catch (IdMUnitException e) {
            throw e;
        } finally {
            new File(KEYSTORE).delete();
        }
    }

    static Map<String, String> getDefaultLdapConfig() {
        return getDefaultLdapConfig("");
    }

    static Map<String, String> getDefaultLdapConfig(String prefix) {
        if (prefix == null) {
            prefix = "";
        }

        Map<String, String> config = new TreeMap<String, String>();
        config.put(prefix + BasicConnector.CONFIG_SERVER,
                DEFAULT_LDAP_SERVER);
        config.put(prefix + BasicConnector.CONFIG_USER,
                DEFAULT_LDAP_USER);
        config.put(prefix + BasicConnector.CONFIG_PASSWORD,
                DEFAULT_LDAP_PASSWORD);
        config.put(prefix + LdapConnectionHelper.CONFIG_TRUST_ALL_CERTS,
                DEFAULT_LDAP_TRUST_ALL_CERTS);
        config.put(prefix + LdapConnectionHelper.CONFIG_USE_TLS,
                DEFAULT_LDAP_USE_TLS);
        return config;
    }

    static Map<String, String> newDefaultImapConfig() {
        Map<String, String> config = new HashMap<String, String>();
        config.put(MailHelper.CONFIG_SERVER, DEFAULT_IMAP_HOST);
        if (!isBlank(DEFAULT_IMAP_USER)) {
            config.put(MailHelper.CONFIG_USER, DEFAULT_IMAP_USER);
        }
        if (!isBlank(DEFAULT_IMAP_PASSWORD)) {
            config.put(MailHelper.CONFIG_PASSWORD, DEFAULT_IMAP_PASSWORD);
        }
        if (!isBlank(DEFAULT_IMAP_SSL)) {
            config.put(MailHelper.CONFIG_SSL, DEFAULT_IMAP_SSL);
        }
        if (DEFAULT_IMAP_PORT > 0) {
            config.put(MailHelper.CONFIG_PORT, String.valueOf(DEFAULT_IMAP_PORT));
        }
        return config;
    }

    static Map<String, String> newDefaultSmtpConfig() {
        Map<String, String> config = new HashMap<String, String>();
        config.put(MailHelper.CONFIG_SERVER, DEFAULT_SMTP_HOST);
        if (!isBlank(DEFAULT_SMTP_USER)) {
            config.put(MailHelper.CONFIG_USER, DEFAULT_SMTP_USER);
        }
        if (!isBlank(DEFAULT_SMTP_PASSWORD)) {
            config.put(MailHelper.CONFIG_PASSWORD, DEFAULT_SMTP_PASSWORD);
        }
        if (!isBlank(DEFAULT_SMTP_SSL)) {
            config.put(MailHelper.CONFIG_SSL, DEFAULT_SMTP_SSL);
        }
        if (DEFAULT_IMAP_PORT > 0) {
            config.put(MailHelper.CONFIG_PORT, String.valueOf(DEFAULT_SMTP_PORT));
        }
        return config;
    }

    static Map<String, Collection<String>> newTemplateAddAttrs() {
        //create the email template
        Map<String, Collection<String>> attrs = new TreeMap<String, Collection<String>>();
        attrs.put("dn", Arrays.asList(ConfigTests.DEFAULT_LDAP_TEMPLATE_DN));
        attrs.put("objectClass", Arrays.asList("notfMergeTemplate", "DirXML-PkgItemAux"));
        attrs.put("cn", Arrays.asList(ConfigTests.DEFAULT_LDAP_TEMPLATE_CN));
        attrs.put("notfMergeTemplateSubject", Arrays.asList("Notice of Password Reset Failure"));
        attrs.put("notfMergeTemplateData", Arrays.asList(ConfigTests.DEFAULT_TEMPLATE_XML));
        return attrs;
    }

    static Map<String, Collection<String>> newTemplateDeleteAttrs() {
        //create the email template
        Map<String, Collection<String>> attrs = new TreeMap<String, Collection<String>>();
        attrs.put("dn", Arrays.asList(ConfigTests.DEFAULT_LDAP_TEMPLATE_DN));
        return attrs;
    }

}
