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
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;

import javax.mail.MessagingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.idmunit.connector.ConnectorUtil.addSingleValue;

@SuppressWarnings("checkstyle:AbstractClassName")
public abstract class MailTestBase extends TestCase {
    AbstractConnector mailConnector;
    String defaultSender = ConfigTests.DEFAULT_SENDER;
    String defaultRecipient = ConfigTests.DEFAULT_RECIPIENT;

    boolean deliveryPause = false; // used for the imap tests where we deliver email and need to wait for it to appear.
    long pauseDuration = TimeUnit.SECONDS.toMillis(10);

    public void testSend() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testSendNullFrom() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    null,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        try {
            mailConnector.execute("Validate", data);
        } catch (IdMUnitFailureException e) {
            assertEquals(
                    "Received a total of [1] email message(s), and was unable to find any that match your expected values.  Listing messages now:\n" +
                            "===========================================================\n" +
                            "Message [1]:\n" +
                            "Failed while checking field: [From] \n" +
                            "expected each regex in:\n" +
                            " [test@franklin.ro],\n" +
                            " to match at least one value in:\n" +
                            " []\n",
                    e.getMessage());
        }
    }

    public void testSendSubjectWithComma() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character, if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character, if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testSendWithTemplate() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testSendEOM() throws IdMUnitException {
        try {
            sendMessage(defaultRecipient, defaultSender, "Test", "Test Body\r\n.");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Test");
        addSingleValue(data, "Body", "Test Body\n.");

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testSendEmptyBody() throws IdMUnitException {
        try {
            sendMessage(defaultRecipient, defaultSender, "Test", "");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Test");
        addSingleValue(data, "Body", "");

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testExpectedButWasStringOneChar() {
        String[] expectedAndActual = MailCompare.getExpectedButWasString("the1String", "the2String");

        assertEquals(3, expectedAndActual.length);
        assertEquals("...1......", expectedAndActual[0]);
        assertEquals("...2......", expectedAndActual[1]);
        assertEquals("Expected: [1] \nbut was:  [2]", expectedAndActual[2]);
    }

    public void testExpectedButWasStringActualLonger() {
        String[] expectedAndActual = MailCompare.getExpectedButWasString("the1String", "the1SomethingiswrongString");

        assertEquals(3, expectedAndActual.length);
        assertEquals(".....t....", expectedAndActual[0]);
        assertEquals(".....omethingiswrongSt....", expectedAndActual[1]);
        assertEquals("Expected: [t] \nbut was:  [omethingiswrongSt]", expectedAndActual[2]);
    }

    public void testExpectedButWasStringExpectedLonger() {
        String[] expectedAndActual = MailCompare.getExpectedButWasString("the1SomethingisRightHereButthisisalongstringString", "the1SomethingiswrongString");

        assertEquals(3, expectedAndActual.length);
        assertEquals("...............RightHereButthisisalongstri........", expectedAndActual[0]);
        assertEquals("...............wro........", expectedAndActual[1]);
        assertEquals("Expected: [RightHereButthisisalongstri] \nbut was:  [wro]", expectedAndActual[2]);
    }

    public void testExpectedButWasStringExpectedBefore() {
        String[] expectedAndActual = MailCompare.getExpectedButWasString("correctString", "actualCorrectString");

        assertEquals(3, expectedAndActual.length);
        assertEquals("c............", expectedAndActual[0]);
        assertEquals("actualC............", expectedAndActual[1]);
        assertEquals("Expected: [c] \nbut was:  [actualC]", expectedAndActual[2]);
    }

    public void testExpectedButWasStringActualBefore() {
        String[] expectedAndActual = MailCompare.getExpectedButWasString("expectedcorrectString", "CorrectString");

        assertEquals(3, expectedAndActual.length);
        assertEquals("expectedc............", expectedAndActual[0]);
        assertEquals("C............", expectedAndActual[1]);
        assertEquals("Expected: [expectedc] \nbut was:  [C]", expectedAndActual[2]);
    }

    public void testConnection() {
        try {
            mailConnector.execute("TestConnection", Collections.<String, Collection<String>>emptyMap());
        } catch (IdMUnitException e) {
            fail();
        }
    }

    public void testSendUntrimmedAttrs() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, " To ", defaultRecipient);
        addSingleValue(data, "Subject ", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, " Body", "Test Body");
        addSingleValue(data, "          From          ", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testToRegexSingle() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", Pattern.quote(defaultRecipient));
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testFromRegex() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", Pattern.quote(defaultSender));

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testToRegexSingleMismatch() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", "somebody@trivir\\.com");
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        try {
            mailConnector.execute("Validate", data);
            fail();
        } catch (IdMUnitFailureException e) {
            assertEquals(
                    "Received a total of [1] email message(s), and was unable to find any that match your expected values.  Listing messages now:\n" +
                            "===========================================================\n" +
                            "Message [1]:\n" +
                            "Failed while checking field: [To] \n" +
                            "expected at least one regex in:\n" +
                            " [somebody@trivir\\.com],\n" +
                            " to match value:\n" +
                            " [test@franklin.ro]\n" +
                            "Failed while checking field: [To] \n" +
                            "expected each regex in:\n" +
                            " [somebody@trivir\\.com],\n" +
                            " to match at least one value in:\n" +
                            " [test@franklin.ro]\n",
                    e.getMessage());
        }
    }

    public void testToRegexMultiple() throws IdMUnitException {
        final String recipient2 = "somebody@trivir.com";

        Collection<String> to = new ArrayList<String>();
        to.add(defaultRecipient);
        to.add(recipient2);

        try {
            sendMessage(
                    to,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        to = new ArrayList<String>();
        to.add(Pattern.quote(defaultRecipient));
        to.add(Pattern.quote(recipient2));

        data.put("To", to);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        mailConnector.execute("Validate", data);
    }

    public void testToRegexMultipleMismatch() throws IdMUnitException {
        //TODO: we need to create a second valid account to avoid mail delivery errors w/ the IMAPConnector
        final String recipient2 = "somebody@trivir.com";

        Collection<String> to = new ArrayList<String>();
        to.add(defaultRecipient);
        to.add(recipient2);

        try {
            sendMessage(
                    to,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        to = new ArrayList<String>();
        to.add("blah@blah\\.com");
        to.add(Pattern.quote(recipient2));

        data.put("To", to);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        try {
            mailConnector.execute("Validate", data);
            fail();
        } catch (IdMUnitFailureException e) {
            //Note: This is a hack to deal with mail delivery subsystem errors until we have a second valid recipient account
            String msg = (e.getMessage() == null) ? "" : e.getMessage();
            String expected =
                    "\n" +
                            "Failed while checking field: [To] \n" +
                            "expected at least one regex in:\n" +
                            " [blah@blah\\.com, \\Qsomebody@trivir.com\\E],\n" +
                            " to match value:\n" +
                            " [test@franklin.ro]\n" +
                            "Failed while checking field: [To] \n" +
                            "expected each regex in:\n" +
                            " [blah@blah\\.com],\n" +
                            " to match at least one value in:\n" +
                            " [test@franklin.ro, somebody@trivir.com]\n";
            assertTrue(msg.contains(expected));
        }
    }

    public void testToRegexAndMismatch() throws IdMUnitException {
        //matches one regex, but not the other

        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);
        Collection<String> to = new ArrayList<String>();
        to.add("blah@blah\\.com");
        to.add(Pattern.quote(defaultRecipient));

        data.put("To", to);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", defaultSender);

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        try {
            mailConnector.execute("Validate", data);
            fail();
        } catch (IdMUnitFailureException e) {
            assertEquals(
                    "Received a total of [1] email message(s), and was unable to find any that match your expected values.  Listing messages now:\n" +
                            "===========================================================\n" +
                            "Message [1]:\n" +
                            "Failed while checking field: [To] \n" +
                            "expected each regex in:\n" +
                            " [blah@blah\\.com],\n" +
                            " to match at least one value in:\n" +
                            " [test@franklin.ro]\n",
                    e.getMessage());
        }
    }

    public void testFromRegexMismatch() throws IdMUnitException {
        try {
            sendMessage(
                    defaultRecipient,
                    defaultSender,
                    "Long subject lines can be folded at a white space character if the line is longer than 78 characters",
                    "Test Body");
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "To", defaultRecipient);
        addSingleValue(data, "Subject", "Long subject lines can be folded at a white space character if the line is longer than 78 characters");
        addSingleValue(data, "Body", "Test Body");
        addSingleValue(data, "From", "somebody@trivir\\.com");

        if (deliveryPause) {
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                // Oh well, our sleep was interrupted.
            }
        }

        try {
            mailConnector.execute("Validate", data);
            fail();
        } catch (IdMUnitFailureException e) {
            //Note: Sometimes this test yields two mail messages. I'm not sure why.
            assertEquals(
                    "Received a total of [1] email message(s), and was unable to find any that match your expected values.  Listing messages now:\n" +
                            "===========================================================\n" +
                            "Message [1]:\n" +
                            "Failed while checking field: [From] \n" +
                            "expected at least one regex in:\n" +
                            " [somebody@trivir\\.com],\n" +
                            " to match value:\n" +
                            " [test@franklin.ro]\n" +
                            "Failed while checking field: [From] \n" +
                            "expected each regex in:\n" +
                            " [somebody@trivir\\.com],\n" +
                            " to match at least one value in:\n" +
                            " [test@franklin.ro]\n",
                    e.getMessage());
        }
    }

    protected abstract void setUp() throws Exception;

    protected abstract void tearDown() throws Exception;

    protected abstract void sendMessage(String to, String from, String subject, String body) throws MessagingException;

    protected abstract void sendMessage(Collection<String> to, String from, String subject, String body) throws MessagingException;

}
