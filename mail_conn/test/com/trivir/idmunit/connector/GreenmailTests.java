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
import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.BasicConnector;

import javax.mail.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.idmunit.connector.ConnectorUtil.addSingleValue;

// These tests are Greenmail-specific.
// NOTE: You MUST restart the Greenmail server to rerun these tests.
public class GreenmailTests extends TestCase {

    private static int userNUM = 0;

    // NOTE: The user/password combination must exist in the users.properties file
    private static final String GREENMAIL_IMAP_HOST = MailHelper.HOST_LOCALHOST;
    private static final String GREENMAIL_IMAP_USER = "test@franklin.ro";
    private static final String GREENMAIL_IMAP_PASSWORD = "thisisa";
    private static final int GREENMAIL_IMAP_PORT = MailHelper.IMAP_PORT_STANDARD;
    private static final String GREENMAIL_IMAP_SSL = "false";

    // NOTE: The user/password combination must exist in the users.properties file
    private static final String GREENMAIL_SMTP_HOST = MailHelper.HOST_LOCALHOST;
    //private static final String GREENMAIL_SMTP_USER = "test@franklin.ro";
    //private static final String GREENMAIL_SMTP_PASSWORD = "thisisa";
    private static final int GREENMAIL_SMTP_PORT = MailHelper.SMTP_PORT_STANDARD;
    //private static final String GREENMAIL_SMTP_SSL = "false";

    // Test whether the mailbox attribute works for the Validate operation.
    //  NOTE: This test is specific to Greenmail.
    public void testValidateMailbox() throws MessagingException, IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);
        final String subject = "subject";
        final String body = "body";

        Map<String, String> imapConfig = new HashMap<String, String>();
        imapConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConfig.put(BasicConnector.CONFIG_USER, GREENMAIL_IMAP_USER);
        imapConfig.put(BasicConnector.CONFIG_PASSWORD, GREENMAIL_IMAP_PASSWORD);
        imapConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));
        imapConfig.put(MailHelper.CONFIG_SSL, GREENMAIL_IMAP_SSL);

        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_SMTP_HOST);
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_SMTP_PORT));

        Map<String, Collection<String>> validateAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(validateAttrs, "To", testUser);
        addSingleValue(validateAttrs, "From", testUser);
        addSingleValue(validateAttrs, "Subject", subject);
        addSingleValue(validateAttrs, "Body", body);
        addSingleValue(validateAttrs, MailHelper.CONFIG_MAILBOX, testUser);

        Folder folder = null;
        Store store = null;
        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConfig);

            try {
                try {
                    imapConnector.execute("Validate", validateAttrs);
                } catch (IdMUnitException e) {
                    assertTrue(e.getMessage().contains("It's possible that the user doesn't exist because they haven't received any mail"));
                }

                MailTestHelper.sendMessage(smtpConfig, testUser, testUser, subject, body);

                try {
                    imapConnector.execute("Validate", validateAttrs);
                } catch (IdMUnitException e) {
                    fail(e.getMessage());
                }
            } finally {
                MailHelper.close(folder);
                MailHelper.close(store);
            }

        } finally {
            imapConnector.tearDown();
        }
    }

    // Test what happens when the DeleteMail operation is called when the mailbox exists.
    public void testDeleteMailMailbox() throws MessagingException, IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);
        final String subject = "subject";
        final String body = "body";

        Map<String, String> imapConnectorConfig = new HashMap<String, String>();
        imapConnectorConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConnectorConfig.put(BasicConnector.CONFIG_USER, GREENMAIL_IMAP_USER);
        imapConnectorConfig.put(BasicConnector.CONFIG_PASSWORD, GREENMAIL_IMAP_PASSWORD);
        imapConnectorConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> imapMailboxConfig = new HashMap<String, String>();
        imapMailboxConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapMailboxConfig.put(BasicConnector.CONFIG_USER, testUser);
        imapMailboxConfig.put(BasicConnector.CONFIG_PASSWORD, testUser);
        imapMailboxConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_SMTP_HOST);
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_SMTP_PORT));

        Map<String, Collection<String>> deleteAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(deleteAttrs, MailHelper.CONFIG_MAILBOX, testUser);

        Folder folder = null;
        String folderName = MailHelper.FOLDER_INBOX;
        Store store = null;
        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConnectorConfig);

            try {
                // Send message
                MailTestHelper.sendMessage(smtpConfig, testUser, testUser, subject, body);

                // Verify there's at least one message
                Session session = MailHelper.newImapSession(imapMailboxConfig);
                store = MailHelper.getStore(session, imapMailboxConfig);
                folder = store.getFolder(folderName);
                folder.open(Folder.READ_ONLY);
                assertTrue(folder.getMessageCount() > 0);
                MailHelper.close(folder);

                // Delete messages
                imapConnector.execute("DeleteMail", deleteAttrs);

                // Verify there are no messages
                folder = store.getFolder(folderName);
                folder.open(Folder.READ_ONLY);
                assertEquals(folder.getMessageCount(), 0);
            } finally {
                MailHelper.close(folder);
                MailHelper.close(store);
            }
        } finally {
            imapConnector.tearDown();
        }
    }

    // Test what happens when authentication is delayed and a mailbox attriute is passed to the DeleteMail operation.
    public void testDeleteLazyAuthWithMailbox() throws MessagingException, IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);
        final String subject = "subject";
        final String body = "body";

        Map<String, String> imapConnectorConfig = new HashMap<String, String>();
        imapConnectorConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConnectorConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> imapMailboxConfig = new HashMap<String, String>();
        imapMailboxConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapMailboxConfig.put(BasicConnector.CONFIG_USER, testUser);
        imapMailboxConfig.put(BasicConnector.CONFIG_PASSWORD, testUser);
        imapMailboxConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_SMTP_HOST);
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_SMTP_PORT));

        Map<String, Collection<String>> deleteAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(deleteAttrs, MailHelper.CONFIG_MAILBOX, testUser);

        Folder folder = null;
        String folderName = MailHelper.FOLDER_INBOX;
        Store store = null;
        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConnectorConfig);

            try {
                // Send message
                MailTestHelper.sendMessage(smtpConfig, testUser, testUser, subject, body);

                // Verify there's at least one message
                Session session = MailHelper.newImapSession(imapMailboxConfig);
                store = MailHelper.getStore(session, imapMailboxConfig);
                folder = store.getFolder(folderName);
                folder.open(Folder.READ_ONLY);
                assertTrue(folder.getMessageCount() > 0);
                MailHelper.close(folder);

                // Delete messages
                imapConnector.execute("DeleteMail", deleteAttrs);

                // Verify there are no messages
                folder = store.getFolder(folderName);
                folder.open(Folder.READ_ONLY);
                assertEquals(folder.getMessageCount(), 0);
            } finally {
                MailHelper.close(folder);
                MailHelper.close(store);
            }
        } finally {
            imapConnector.tearDown();
        }
    }

    // Test what happens when authentication is delayed and a mailbox attriute isn't passed to the DeleteMail operation.
    public void testDeleteLazyAuthNoMailbox() throws MessagingException, IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);
        final String subject = "subject";
        final String body = "body";

        Map<String, String> imapConnectorConfig = new HashMap<String, String>();
        imapConnectorConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConnectorConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> imapMailboxConfig = new HashMap<String, String>();
        imapMailboxConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapMailboxConfig.put(BasicConnector.CONFIG_USER, testUser);
        imapMailboxConfig.put(BasicConnector.CONFIG_PASSWORD, testUser);
        imapMailboxConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_SMTP_HOST);
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_SMTP_PORT));

        Map<String, Collection<String>> deleteAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        Folder folder = null;
        String folderName = MailHelper.FOLDER_INBOX;
        Store store = null;
        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConnectorConfig);

            try {
                // Send message
                MailTestHelper.sendMessage(smtpConfig, testUser, testUser, subject, body);

                // Verify there's at least one message
                Session session = MailHelper.newImapSession(imapMailboxConfig);
                store = MailHelper.getStore(session, imapMailboxConfig);
                folder = store.getFolder(folderName);
                folder.open(Folder.READ_ONLY);
                assertTrue(folder.getMessageCount() > 0);
                MailHelper.close(folder);

                // Delete messages
                imapConnector.execute("DeleteMail", deleteAttrs);
                fail();
            } finally {
                MailHelper.close(folder);
                MailHelper.close(store);
            }
        } catch (IdMUnitException e) {
            assertEquals("Check your IMAP configuration. Either provide a 'user' for the IMAP connector or pass a 'mailbox' attribute to this operation", e.getMessage());
        } finally {
            imapConnector.tearDown();
        }
    }

    // Test what happens when the DeleteMail operation is called when the mailbox doesn't exist yet.
    public void testDeleteMailMailboxDoesntExist() throws MessagingException, IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);

        Map<String, String> imapConnectorConfig = new HashMap<String, String>();
        imapConnectorConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConnectorConfig.put(BasicConnector.CONFIG_USER, GREENMAIL_IMAP_USER);
        imapConnectorConfig.put(BasicConnector.CONFIG_PASSWORD, GREENMAIL_IMAP_PASSWORD);
        imapConnectorConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> imapMailboxConfig = new HashMap<String, String>();
        imapMailboxConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapMailboxConfig.put(BasicConnector.CONFIG_USER, testUser);
        imapMailboxConfig.put(BasicConnector.CONFIG_PASSWORD, testUser);
        imapMailboxConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_SMTP_HOST);
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_SMTP_PORT));

        Map<String, Collection<String>> deleteAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(deleteAttrs, MailHelper.CONFIG_MAILBOX, testUser);

        Folder folder = null;
        Store store = null;
        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConnectorConfig);

            try {
                // Verify there are no messages
                try {
                    Session session = MailHelper.newImapSession(imapMailboxConfig);
                    store = MailHelper.getStore(session, imapMailboxConfig);
                    fail();
                } catch (AuthenticationFailedException e) {
                    //the mailbox shouldn't exist yet
                }

                // Delete messages
                imapConnector.execute("DeleteMail", deleteAttrs);
            } finally {
                MailHelper.close(folder);
                MailHelper.close(store);
            }
        } finally {
            imapConnector.tearDown();
        }
    }


    // Test what happens when authentication is delayed and a mailbox attriute isn't passed to the Validate operation.
    public void testValidteLazyAuthNoMailbox() throws MessagingException, IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);
        final String subject = "subject";
        final String body = "body";

        // Omit user/password
        Map<String, String> imapConnectorConfig = new HashMap<String, String>();
        imapConnectorConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConnectorConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_SMTP_HOST);
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_SMTP_PORT));

        Map<String, Collection<String>> validateAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(validateAttrs, "To", testUser);
        addSingleValue(validateAttrs, "Subject", subject);
        addSingleValue(validateAttrs, "Body", body);
        addSingleValue(validateAttrs, "From", testUser);

        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConnectorConfig);

            // Send message
            MailTestHelper.sendMessage(smtpConfig, testUser, testUser, subject, body);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }

            imapConnector.execute("Validate", validateAttrs);
        } catch (IdMUnitException e) {
            assertEquals("Check your IMAP configuration. Either provide a 'user' for the IMAP connector or pass a 'mailbox' attribute to this operation", e.getMessage());
        } finally {
            imapConnector.tearDown();
        }
    }

    // Test what happens when authentication is delayed and a mailbox attriute is passed to the Validate operation.
    public void testValidteLazyAuthWithMailbox() throws MessagingException, IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);
        final String subject = "subject";
        final String body = "body";

        // Omit user/password
        Map<String, String> imapConnectorConfig = new HashMap<String, String>();
        imapConnectorConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConnectorConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_SMTP_HOST);
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_SMTP_PORT));

        Map<String, Collection<String>> validateAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(validateAttrs, "To", testUser);
        addSingleValue(validateAttrs, "Subject", subject);
        addSingleValue(validateAttrs, "Body", body);
        addSingleValue(validateAttrs, "From", testUser);
        addSingleValue(validateAttrs, "Mailbox", testUser);

        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConnectorConfig);

            // Send message
            MailTestHelper.sendMessage(smtpConfig, testUser, testUser, subject, body);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }

            imapConnector.execute("Validate", validateAttrs);
        } finally {
            imapConnector.tearDown();
        }
    }

    //TODO: add test for the "Folder" attribute
    public void testConnection() throws IdMUnitException {
        final String testUser = String.format("newuser%s@nowhere.com", ++userNUM);

        Map<String, String> imapConnectorConfig = new HashMap<String, String>();
        imapConnectorConfig.put(BasicConnector.CONFIG_SERVER, GREENMAIL_IMAP_HOST);
        imapConnectorConfig.put(MailHelper.CONFIG_PORT, String.valueOf(GREENMAIL_IMAP_PORT));

        Map<String, Collection<String>> testConnAttrs = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(testConnAttrs, "Mailbox", testUser);

        IMAPConnector imapConnector = new IMAPConnector();

        try {
            imapConnector.setup(imapConnectorConfig);
            imapConnector.execute("TestConnection", testConnAttrs);
        } finally {
            imapConnector.tearDown();
        }
    }
}
