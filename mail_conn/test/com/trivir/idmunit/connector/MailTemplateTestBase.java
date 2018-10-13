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

import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.LdapConnector;

import javax.mail.MessagingException;
import java.util.*;

import static org.idmunit.connector.ConnectorUtil.addSingleValue;

@SuppressWarnings("checkstyle:AbstractClassName")
public abstract class MailTemplateTestBase extends TestCase {

    AbstractConnector mailConnector;
    LdapConnector ldapConnector;
    String defaultSender = ConfigTests.DEFAULT_SENDER;
    String defaultRecipient = ConfigTests.DEFAULT_RECIPIENT;

// This is the standard Password Reset Failure template. Included for reference.
//    String emailTemplate =    "<head>\r\n" +
//                            "  <title>Notice of Password Reset Failure</title>\r\n" +
//                            "  <style> <!-- body { font-family: Trebuchet MS } --> </style>\r\n" +
//                            "</head>\r\n" +
//                            "<body BGCOLOR=\"#FFFFFF\">\r\n" +
//                            "  <p>Dear $UserFullName$,</p>\r\n" +
//                            "  <p>This is a notice that your password could not be reset in the $ConnectedSystemName$ system..  The reason for failure is indicated below:</p>\r\n" +
//                            "  <p>Reason: $FailureReason$</p>\r\n" +
//                            "  <p>If you have any further questions,\r\n" +
//                            "     please contact the help desk at (012) 345-6789 or email\r\n" +
//                            "     at <a href=\"mailto:help.desk@mycompany.com\">\r\n" +
//                            "     help.desk@mycompany.com </a></p>\r\n" +
//                            "  <p> - Automated Security</p>\r\n" +
//                            "  <p><img SRC=\"cid:powered_by_novell.gif\" ALT=\"Powered by Novell\" width=\"80\" height=\"29\"/></p>\r\n" +
//                            "</body>";

    boolean deliveryPause = false; // used for the imap tests where we deliver email and need to wait for it to appear.
    long pauseDuration = 15 * 1000;

    public String getBody(String userFullName,
                          String userGivenName,
                          String userLastName,
                          String connectedSystemName,
                          String failureReason) {
        return "<html>\r\n" +
                "<head>\r\n" +
                "  <title>Notice of Password Reset Failure</title>\r\n" +
                "  <style> <!-- body { font-family: Trebuchet MS } --> </style>\r\n" +
                "</head>\r\n" +
                "<body BGCOLOR=\"#FFFFFF\">\r\n" +
                "  <p>Dear " + userFullName + ",</p>\r\n" +
                "  <p>This is a notice that your password could not be reset in the " + connectedSystemName + " system..  The reason for failure is indicated below:</p>\r\n" +
                "  <p>Reason: " + failureReason + "</p>\r\n" +
                "  <p>If you have any further questions,\r\n" +
                "     please contact the help desk at (012) 345-6789 or email\r\n" +
                "     at <a href=\"mailto:help.desk@mycompany.com\">\r\n" +
                "     help.desk@mycompany.com </a></p>\r\n" +
                "  <p> - Automated Security</p>\r\n" +
                "  <p><img ALT=\"Powered by Novell\" SRC=\"cid:powered_by_novell.gif\" height=\"29\" width=\"80\"/></p>\r\n" +
                "</body>\r\n" +
                "</html>";
    }

    public void testSend() throws IdMUnitException {
        String subject = "Notice of Password Reset Failure";
        String userFullName = "Fred Flintstone";
        String userGivenName = "Fred";
        String userLastName = "Flintstone";
        String connectedSystemName = "Trivir";
        String failureReason = "Personal";
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    subject,
                    getBody(userFullName, userGivenName, userLastName, connectedSystemName, failureReason));
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", subject);
        addSingleValue(data, "Template", ConfigTests.DEFAULT_LDAP_TEMPLATE_DN);
        addSingleValue(data, "UserFullName", userFullName);
        addSingleValue(data, "UserGivenName", userGivenName);
        addSingleValue(data, "UserLastName", userLastName);
        addSingleValue(data, "ConnectedSystemName", connectedSystemName);
        addSingleValue(data, "FailureReason", failureReason);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testSendSubjectWithComma() throws IdMUnitException {
        String subject = "Notice of Password, Reset Failure";
        String userFullName = "Fred Flintstone";
        String userGivenName = "Fred";
        String userLastName = "Flintstone";
        String connectedSystemName = "Trivir";
        String failureReason = "Personal";
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    subject,
                    getBody(userFullName, userGivenName, userLastName, connectedSystemName, failureReason));
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", subject);
        addSingleValue(data, "Template", ConfigTests.DEFAULT_LDAP_TEMPLATE_DN);
        addSingleValue(data, "UserFullName", userFullName);
        addSingleValue(data, "UserGivenName", userGivenName);
        addSingleValue(data, "UserLastName", userLastName);
        addSingleValue(data, "ConnectedSystemName", connectedSystemName);
        addSingleValue(data, "FailureReason", failureReason);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testValidateUnrecognizedAttrs() throws MessagingException, IdMUnitException {

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "Bogus", "value");
        addSingleValue(data, "Unvalued1", null);
        addSingleValue(data, "Unvalued2", "");

        // Legitimate values
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

    public void testSendFailure() throws IdMUnitException {
        // Leftover messagess from prior test runs can cause the error message to change
        mailConnector.execute("DeleteMail", Collections.<String, Collection<String>>emptyMap());

        String subject = "Notice of Password Reset Failure";
        String userFullName = "Fred Flintstone";
        String userGivenName = "Fred";
        String userLastName = "Flintstone";
        String connectedSystemName = "Trivir";
        String failureReason = "Personal";
        String badSubject = subject + "Bad";
        String badUserFullName = userFullName + "Bad";
        String badUserGivenName = userGivenName + "Bad";
        String badUserLastName = userLastName + "Bad";
        String badConnectedSystemName = connectedSystemName + "Bad";
        String badFailureReason = failureReason + "Bad";
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    subject,
                    getBody(userFullName, userGivenName, userLastName, connectedSystemName, failureReason));
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", badSubject);
        addSingleValue(data, "Template", ConfigTests.DEFAULT_LDAP_TEMPLATE_DN);
        addSingleValue(data, "UserFullName", badUserFullName);
        addSingleValue(data, "UserGivenName", badUserGivenName);
        addSingleValue(data, "UserLastName", badUserLastName);
        addSingleValue(data, "ConnectedSystemName", badConnectedSystemName);
        addSingleValue(data, "FailureReason", badFailureReason);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        try {
            mailConnector.execute("Validate", data);
            fail("Validation succeeded when it should have failed");
        } catch (IdMUnitException e) {
            String expectedError =
                    "Received a total of [1] email message(s), and was unable to find any that match your expected values.  Listing messages now:\n" +
                            "===========================================================\n" +
                            "Message [1]:\n" +
                            "Email pieces were not equal for: [Subject]\n" +
                            "Expected: [Notice of Password Reset FailureBad]\n" +
                            "Actual:   [Notice of Password Reset Failure]\n" +
                            "\n" +
                            "Deltas Only: Delta String:\n" +
                            "Expected: [Bad] \n" +
                            "but was:  []\n" +
                            "\n" +
                            "Email pieces were not equal for: [Template]\n" +
                            "Key:      [ConnectedSystemName]\n" +
                            "Value:    [TrivirBad]\n" +
                            "Template: [This is a notice that your password could not be reset in the $ConnectedSystemName$ system..  The reason for failure is indicated below:]\n" +
                            "Expected: [This is a notice that your password could not be reset in the TrivirBad system..  The reason for failure is indicated below:]\n" +
                            "Actual:   [This is a notice that your password could not be reset in the Trivir system..  The reason for failure is indicated below:]\n" +
                            "\n" +
                            "Email pieces were not equal for: [Template]\n" +
                            "Key:      [UserFullName]\n" +
                            "Value:    [Fred FlintstoneBad]\n" +
                            "Template: [Dear $UserFullName$,]\n" +
                            "Expected: [Dear Fred FlintstoneBad,]\n" +
                            "Actual:   [Dear Fred Flintstone,]\n" +
                            "\n" +
                            "Email pieces were not equal for: [Template]\n" +
                            "Key:      [FailureReason]\n" +
                            "Value:    [PersonalBad]\n" +
                            "Template: [Reason: $FailureReason$]\n" +
                            "Expected: [Reason: PersonalBad]\n" +
                            "Actual:   [Reason: Personal]\n" +
                            "\n";

            assertEquals(expectedError, e.getMessage());
        }
    }

    protected abstract void setUp() throws Exception;

    protected abstract void tearDown() throws Exception;

    protected abstract void sendMessage(String to, String from, String subject, String body) throws MessagingException;

}
