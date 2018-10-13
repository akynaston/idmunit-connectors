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

import javax.mail.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

import static org.idmunit.connector.ConnectorUtil.addSingleValue;

public class SMTPServerTests extends MailTestBase {
    private static final int SMTP_PORT = ConfigTests.DEFAULT_SMTP_STANDALONE_PORT;

    protected void setUp() throws Exception {
        // server is for setup and tear down usage, connector is for test usage.
        mailConnector = new SMTPServer();

        Map<String, String> config = new HashMap<String, String>();
        config.put(MailHelper.CONFIG_PORT, Integer.toString(SMTP_PORT));
        config.put(MailHelper.CONFIG_RECIPIENT, defaultRecipient);
        mailConnector.setup(config);
    }

    protected void tearDown() throws Exception {
        mailConnector.tearDown();
    }

    protected void sendMessage(String to, String from, String subject, String body) throws MessagingException {
        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(SMTP_PORT));
        MailTestHelper.sendMessage(smtpConfig, to, from, subject, body);
    }

    @Override
    protected void sendMessage(Collection<String> to, String from, String subject, String body) throws MessagingException {
        Map<String, String> smtpConfig = new HashMap<String, String>();
        smtpConfig.put(MailHelper.CONFIG_PORT, String.valueOf(SMTP_PORT));
        MailTestHelper.sendMessage(smtpConfig, to, from, subject, body);
    }

    private Properties getMailProperties(int port) {
        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.host", MailHelper.HOST_LOCALHOST);
        mailProps.setProperty("mail.smtp.port", "" + port);
        mailProps.setProperty("mail.smtp.sendpartial", "true");
        return mailProps;
    }

    public void testRemoveEmailBodyMimeData() {
        String hasEmailPostAndPrefixgarbage = "--myp<HTML>ost.3463464566<html>mygooddata</html>-----3</HTML>4534";
        String clean = "<html>mygooddata</html>";

        assertEquals(clean, MailCompare.removeEmailBodyMimeData(hasEmailPostAndPrefixgarbage));
    }

    public void testSMTPPortInUse() throws IOException, IdMUnitException {
        mailConnector.tearDown(); // Kill the server setup by the test setup - it will only interfere with this test.
        SMTPServer conn = new SMTPServer();
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(conn.getPort());
        } catch (IOException e) {
            serverSocket = null;
        }

        try {
            conn.setup(new HashMap<String, String>());
            conn.tearDown();
            fail("setup should have failed with port already in use");
        } catch (IdMUnitException e) {
            // Test succeeded - no email messages to validate, port 25 collision was managed.
            // TODO: confirm "port is in use message is correct"
        } finally {
            if (serverSocket != null && serverSocket.isBound()) {
                serverSocket.close();
            }
        }
    }

    @SuppressWarnings("serial")
    public void testDuplicateQueue() throws IdMUnitException {
        SMTPServer conn = new SMTPServer();

        try {
            conn.setup(new HashMap<String, String>() {{
                    put(MailHelper.CONFIG_RECIPIENT, defaultRecipient);
                }});
            fail("setup should have failed with queue already specified");
        } catch (IdMUnitException e) {
            // Test succeeded - no email messages to validate, duplicate queue managed.
            // TODO: confirm "port is in use message is correct"
        } finally {
            conn.tearDown();
        }
    }

    public void testDuplicateDefaultQueue() throws IOException, IdMUnitException {

        SMTPServer conn = new SMTPServer();
        SMTPServer conn2 = new SMTPServer();
        try {
            conn.setup(new HashMap<String, String>());
            conn2.setup(new HashMap<String, String>());
            fail("setup should have failed with queue already specified");
        } catch (IdMUnitException e) {
            // Test succeeded - no email messages to validate, duplicate queue managed.
            // TODO: confirm "port is in use message is correct"
        } finally {
            conn.tearDown();
            conn2.tearDown();
        }
    }

    public void testDefaultQueueSend() throws IdMUnitException {
        SMTPServer conn = new SMTPServer();
        conn.setup(new HashMap<String, String>());
        try {
            sendMessage(
                    "receiver2@there.com",
                    "sender@here.com",
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", "receiver2@there.com");
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");

        conn.execute("Validate", data);
    }

    public void testDeleteMail() throws MessagingException, IdMUnitException {
        // Send message
        sendMessage(defaultRecipient, defaultSender, "subject", "body");

        SMTPServer smtpServer = (SMTPServer)mailConnector;

        // Verify there's at least one message
        assertTrue(smtpServer.getMessageCount() > 0);

        // Delete messages
        mailConnector.execute("DeleteMail", Collections.<String, Collection<String>>emptyMap());

        // Verify there are no messages
        assertEquals(smtpServer.getMessageCount(), 0);
    }

    // Test whether messages remain unread across calls to Validate where there's no match
    public void testUnmatchedValidate() throws MessagingException {
        sendMessage(defaultRecipient, defaultSender, "subject", "body");

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "From", defaultSender);
        addSingleValue(data, "Subject", "mismatch");
        addSingleValue(data, "Body", "mismatch");

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        SMTPServer smtpServer = (SMTPServer)mailConnector;
        int firstNumMessages = smtpServer.getMessageCount();
        assertTrue(firstNumMessages > 0);

        try {
            mailConnector.execute("Validate", data);
            fail();
        } catch (IdMUnitException e) {
            //do nothing
        }

        int midNumMessages = smtpServer.getMessageCount();
        assertEquals(firstNumMessages, midNumMessages);

        try {
            mailConnector.execute("Validate", data);
            fail();
        } catch (IdMUnitException e) {
            //do nothing
        }

        int lastNumMessages = smtpServer.getMessageCount();
        assertEquals(midNumMessages, lastNumMessages);
    }

    public void testDeleteMailUnrecognizedAttrs() throws MessagingException, IdMUnitException {

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "Bogus", "value");
        addSingleValue(data, "Unvalued1", null);
        addSingleValue(data, "Unvalued2", "");

        // Legitimate values

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

        try {
            mailConnector.execute("TestConnection", data);
            fail("Expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("Unsupported attribute(s): [Bogus]", e.getMessage());
        }
    }

    /**
     * Sends email - function from http://www.javacommerce.com/displaypage.jsp?name=javamail.sql&id=18274
     * WANRING: NOT TESTED YE T. .
     * @param recipients
     * @param subject
     * @param message
     * @param from
     * @throws MessagingException
     */
//    public void postMail( String recipients[ ], String subject, String message , String from) throws MessagingException    {
//        boolean debug = false;
//
//         //Set the host smtp address
//         Properties props = new Properties();
//         props.put("mail.smtp.host", "localhost");
//
//        // create some properties and get the default Session
//        Session session = Session.getDefaultInstance(props, null);
//        session.setDebug(debug);
//
//        // create a message
//        Message msg = new MimeMessage(session);
//
//        // set the from and to address
//        InternetAddress addressFrom = new InternetAddress(from);
//        msg.setFrom(addressFrom);
//
//        InternetAddress[] addressTo = new InternetAddress[recipients.length];
//        for (int i = 0; i < recipients.length; i++)
//        {
//            addressTo[i] = new InternetAddress(recipients[i]);
//        }
//        msg.setRecipients(Message.RecipientType.TO, addressTo);
//
//
//        // Optional : You can also set your custom headers in the Email if you Want
//        msg.addHeader("MyHeaderName", "myHeaderValue");
//
//        // Setting the Subject and Content Type
//        msg.setSubject(subject);
//        msg.setContent(message, "text/plain");
//        Transport.send(msg);
//    }

}
