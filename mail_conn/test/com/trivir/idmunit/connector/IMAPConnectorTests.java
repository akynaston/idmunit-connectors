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

import javax.mail.*;
import java.io.IOException;
import java.util.*;

import static org.idmunit.connector.ConnectorUtil.addSingleValue;

public class IMAPConnectorTests extends MailTestBase {
    @Override
    protected void setUp() throws Exception {
        mailConnector = new IMAPConnector();

        Map<String, String> config = new HashMap<String, String>();
        config.put(BasicConnector.CONFIG_SERVER, ConfigTests.DEFAULT_IMAP_HOST);
        config.put(BasicConnector.CONFIG_USER, ConfigTests.DEFAULT_IMAP_USER);
        config.put(BasicConnector.CONFIG_PASSWORD, ConfigTests.DEFAULT_IMAP_PASSWORD);
        config.put(MailHelper.CONFIG_PORT, String.valueOf(ConfigTests.DEFAULT_IMAP_PORT));
        config.put(MailHelper.CONFIG_SSL, ConfigTests.DEFAULT_IMAP_SSL);
        mailConnector.setup(config);
        mailConnector.execute("DeleteMail", Collections.<String, Collection<String>>emptyMap());

        deliveryPause = true; // in MailTestBase. Needed for IMAP tests.
    }

    @Override
    protected void tearDown() throws Exception {
        mailConnector.tearDown();
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

    @Override
    protected void sendMessage(Collection<String> to, String from, String subject, String body) throws MessagingException {
        MailTestHelper.sendMessage(
                ConfigTests.newDefaultSmtpConfig(),
                to,
                from,
                subject,
                body);
    }

    public void testSetup() throws IOException, IdMUnitException {

        Folder folder = null;
        String folderName = MailHelper.FOLDER_INBOX;
        Store store = null;

        try {
            Map<String, String> imapConfig = ConfigTests.newDefaultImapConfig();
            Session session = MailHelper.newImapSession(imapConfig);
            store = MailHelper.getStore(session, imapConfig);
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            System.out.println(folderName + ": new message count:    " + folder.getNewMessageCount());
            System.out.println(folderName + ": unread message count: " + folder.getUnreadMessageCount());
            Message[] unreadMessages = folder.search(MailHelper.SEARCH_UNREAD);
            System.out.println(folderName + ": unread messages:      " + unreadMessages.length);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            MailHelper.close(folder);
            MailHelper.close(store);
        }
    }

    public void testDeleteMail() throws MessagingException, IdMUnitException {

        Folder folder = null;
        String folderName = MailHelper.FOLDER_INBOX;
        Store store = null;

        try {
            // Send message
            sendMessage(defaultRecipient, defaultSender, "subject", "body");

            Map<String, String> imapConfig = ConfigTests.newDefaultImapConfig();

            // Verify there's at least one message
            Session session = MailHelper.newImapSession(imapConfig);
            store = MailHelper.getStore(session, imapConfig);
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            assertTrue(folder.getMessageCount() > 0);
            MailHelper.close(folder);

            // Delete messages
            mailConnector.execute("DeleteMail", Collections.<String, Collection<String>>emptyMap());

            // Verify there are no messages
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            assertEquals(folder.getMessageCount(), 0);
        } finally {
            MailHelper.close(folder);
            MailHelper.close(store);
        }
    }

    // Test whether messages remain unread across calls to Validate where there's no match
    public void testUnmatchedValidate() throws MessagingException {
        Folder folder = null;
        String folderName = MailHelper.FOLDER_INBOX;
        Store store = null;

        try {
            sendMessage(defaultRecipient, defaultSender, "subject", "body");

            if (deliveryPause) {
                try {
                    Thread.sleep(pauseDuration);
                } catch (InterruptedException e) {
                    // Oh well, our sleep was interrupted.
                }
            }

            Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
            addSingleValue(data, "To", defaultRecipient);
            addSingleValue(data, "From", defaultSender);
            addSingleValue(data, "Subject", "mismatch");
            addSingleValue(data, "Body", "mismatch");

            Map<String, String> imapConfig = ConfigTests.newDefaultImapConfig();
            Session session = MailHelper.newImapSession(imapConfig);
            store = MailHelper.getStore(session, imapConfig);
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            int firstNumMessages = folder.getMessageCount();
            MailHelper.close(folder);
            assertTrue(firstNumMessages > 0);

            try {
                mailConnector.execute("Validate", data);
                fail();
            } catch (IdMUnitException e) {
                //do nothing
            }

            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            int midNumMessages = folder.getMessageCount();
            MailHelper.close(folder);
            assertEquals(firstNumMessages, midNumMessages);

            try {
                mailConnector.execute("Validate", data);
                fail();
            } catch (IdMUnitException e) {
                //do nothing
            }

            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            int lastNumMessages = folder.getMessageCount();
            MailHelper.close(folder);
            assertEquals(midNumMessages, lastNumMessages);
        } finally {
            MailHelper.close(folder);
            MailHelper.close(store);
        }
    }

    public void testDeleteMailUnrecognizedAttrs() throws MessagingException, IdMUnitException {

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "Bogus", "value");
        addSingleValue(data, "Unvalued1", null);
        addSingleValue(data, "Unvalued2", "");

        // Legitimate values
        addSingleValue(data, "Mailbox", "value");
        addSingleValue(data, "Folder", "value");

        try {
            mailConnector.execute("DeleteMail", data);
            fail("Expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("Unsupported attribute(s): [Bogus]", e.getMessage());
        }
    }

    public void testTestConnectionUnrecognizedAttrs() throws MessagingException, IdMUnitException {

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "Bogus", "value");
        addSingleValue(data, "Unvalued1", null);
        addSingleValue(data, "Unvalued2", "");

        // Legitimate values
        addSingleValue(data, "Mailbox", "value");
        addSingleValue(data, "Folder", "value");

        try {
            mailConnector.execute("TestConnection", data);
            fail("Expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("Unsupported attribute(s): [Bogus]", e.getMessage());
        }
    }

    /**
     * Return the primary text content of the message.
     */
    private String getText(Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null) {
                        text = getText(bp);
                    }
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null) {
                        return s;
                    }
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null) {
                    return s;
                }
            }
        }

        return null;
    }
}
