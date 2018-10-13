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

import javax.mail.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IMAPConnectorExternalServerTests extends MailTestBase {
    @Override
    protected void setUp() throws Exception {

        // server is for setup and tear down usage, connector is for test usage.
        mailConnector = new IMAPConnector();

        Map<String, String> config = new HashMap<String, String>();
        config.put(MailHelper.CONFIG_SERVER, ConfigTests.DEFAULT_IMAP_HOST);
        config.put(MailHelper.CONFIG_USER, ConfigTests.DEFAULT_IMAP_USER);
        config.put(MailHelper.CONFIG_PASSWORD, ConfigTests.DEFAULT_IMAP_PASSWORD);
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

    public void testSetup() {

        Map<String, String> imapConfig = new HashMap<String, String>();
        imapConfig.put(MailHelper.CONFIG_SERVER, ConfigTests.DEFAULT_IMAP_HOST);
        imapConfig.put(MailHelper.CONFIG_USER, ConfigTests.DEFAULT_IMAP_USER);
        imapConfig.put(MailHelper.CONFIG_PASSWORD, ConfigTests.DEFAULT_IMAP_PASSWORD);
        imapConfig.put(MailHelper.CONFIG_PORT, String.valueOf(ConfigTests.DEFAULT_IMAP_PORT));
        imapConfig.put(MailHelper.CONFIG_SSL, ConfigTests.DEFAULT_IMAP_SSL);

        Folder folder = null;
        String folderName = MailHelper.FOLDER_INBOX;
        Store store = null;

        try {
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
            System.exit(2);
        } finally {
            MailHelper.close(folder);
            MailHelper.close(store);
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
}
