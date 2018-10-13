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

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

public class MailHelper {

    public static final FlagTerm SEARCH_UNREAD = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

    public static final int SMTP_PORT_STANDARD = 25;
    public static final int IMAP_PORT_STANDARD = 143;

    public static final String HOST_LOCALHOST = "localhost";

    public static final String FOLDER_INBOX = "Inbox";

    public static final String CONFIG_FOLDER = "folder";
    public static final String CONFIG_MAILBOX = "mailbox";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_PORT = "port";
    public static final String CONFIG_RECIPIENT = "recipient";
    public static final String CONFIG_SERVER = "server";
    public static final String CONFIG_SSL = "ssl";
    public static final String CONFIG_USER = "user";

    public static void close(Store store) {
        if (store != null) {
            try {
                store.close();
            } catch (MessagingException e) {
                // Ignore
            }
        }
    }

    public static void close(Folder folder) {
        close(folder, false);
    }

    public static void close(Folder folder, boolean expunge) {
        if (folder != null) {
            try {
                folder.close(expunge);
            } catch (MessagingException e) {
                // Ignore
            } catch (IllegalStateException e) {
                // Thrown if a folder is closed twice
            }
        }
    }

    public static void applyFlags(Message[] messages, Flags flags, boolean set) throws MessagingException {
        if ((messages == null) || (messages.length == 0)) {
            return;
        }

        if (flags == null) {
            return;
        }

        for (Message message : messages) {
            message.setFlags(flags, set);
        }
    }

    public static void applyFlag(Message[] messages, Flags.Flag flag, boolean set) throws MessagingException {
        if ((messages == null) || (messages.length == 0)) {
            return;
        }

        if (flag == null) {
            return;
        }

        for (Message message : messages) {
            message.setFlag(flag, set);
        }
    }

    public static void deleteAllMessages(Folder folder) throws MessagingException {
        if (folder == null) {
            return;
        }

        applyFlag(folder.getMessages(), Flags.Flag.DELETED, true);
        folder.expunge();
    }

    //TODO: Add CC, BCC
    public static MimeMessage newMessage(
            Session session,
            String to,
            String from,
            String subject,
            String body) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setSentDate(new Date());

        if (!isBlank(to)) {
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        }

        if (!isBlank(from)) {
            msg.setFrom(new InternetAddress(from));
        }

        // Empty subject allowed
        if (null != subject) {
            msg.setSubject(subject);
        }

        // Empty body allowed
        if (null != body) {
            msg.setText(body);
        }

        return msg;
    }

    public static MimeMessage newMessage(
            Session session,
            Collection<String> to,
            String from,
            String subject,
            String body) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setSentDate(new Date());

        if (to != null) {
            for (String t : to) {
                if (!isBlank(t)) {
                    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(t));
                }
            }
        }

        if (!isBlank(from)) {
            msg.setFrom(new InternetAddress(from));
        }

        // Empty subject allowed
        if (null != subject) {
            msg.setSubject(subject);
        }

        // Empty body allowed
        if (null != body) {
            msg.setText(body);
        }

        return msg;
    }

    public static Session newSmtpSession() {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", HOST_LOCALHOST);
        props.setProperty("mail.smtp.port", String.valueOf(SMTP_PORT_STANDARD));

        return Session.getInstance(props);
    }

    public static Session newSmtpSession(Map<String, String> config) {
        if (config == null) {
            config = new HashMap<String, String>();
        }

        final String username = config.get(CONFIG_USER);
        final String password = config.get(CONFIG_PASSWORD);
        String server = config.get(CONFIG_SERVER);
        String port = config.get(CONFIG_PORT);
        String ssl = config.get(CONFIG_SSL);

        Properties props = new Properties();

        props.setProperty("mail.smtp.sendpartial", "true");

        if (isBlank(server)) {
            server = HOST_LOCALHOST;
        }
        props.setProperty("mail.smtp.host", server);

        // As the property key doesn't change depending upon the port #, it's safe to assume the default port #
        if (isBlank(port)) {
            port = String.valueOf(SMTP_PORT_STANDARD);
        }
        props.setProperty("mail.smtp.port", port);

        if (!isBlank(ssl) && "true".equalsIgnoreCase(ssl)) {
            props.setProperty("mail.smtp.starttls.enable", "true");
        }

        Authenticator auth = null;

        if (!isBlank(username)) {
            props.setProperty("mail.smtp.auth", "true");
            auth = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }

        return Session.getInstance(props, auth);
    }

    public static Session newImapSession(Map<String, String> config) {
        if ((config == null) || config.isEmpty()) {
            throw new IllegalArgumentException("Param 'config' is null or empty");
        }

        String username = config.get(CONFIG_USER);
        String server = config.get(CONFIG_SERVER);
        String port = config.get(CONFIG_PORT);
        String ssl = config.get(CONFIG_SSL);

        if (isBlank(ssl)) {
            ssl = "false";
        }
        boolean useSSL = Boolean.valueOf(ssl);

        //TODO: Why use System Properties? Why not create a new Properties object?
        //Properties props = System.getProperties();
        Properties props = new Properties();

        String storeType;

        if (useSSL) {
            props.setProperty("mail.store.protocol", "imaps");
            storeType = "imaps";
        } else {
            props.setProperty("mail.store.protocol", "imap");
            storeType = "imap";
        }

        String protocolProperty = "mail." + storeType;

        if (!isBlank(server)) {
            props.setProperty(protocolProperty + ".host", server);
        } else {
            throw new IllegalArgumentException(String.format("Expected configuration parameter '%s' is empty", CONFIG_SERVER));
        }

        // NOTE: It isn't safe to assume the default port is 143. Gmail doesn't require a port and the default is 993.
        //  We could assume a default of port 143 if we can use mail.imap properties all of the time instead of mail.imaps.
        if (!isBlank(port)) {
            props.setProperty(protocolProperty + ".port", port);
        }

        props.setProperty(protocolProperty + ".port", port);

        if (!isBlank(username)) {
            props.setProperty(protocolProperty + ".user", username);
        }

        return Session.getInstance(props);
    }

    public static Store getStore(Session session, Map<String, String> config) throws MessagingException {
        if (session == null) {
            throw new IllegalArgumentException("Param 'session' is null");
        }


        if ((config == null) || config.isEmpty()) {
            throw new IllegalArgumentException("Param 'config' is null or empty");
        }

        String username = config.get(CONFIG_USER);
        String password = config.get(CONFIG_PASSWORD);

        Store store = session.getStore();
        store.connect(username, password);
        return store;
    }

}
