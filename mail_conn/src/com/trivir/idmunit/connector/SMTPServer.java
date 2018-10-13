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

import com.dumbster.smtp.SmtpActionType;
import com.dumbster.smtp.SmtpRequest;
import com.dumbster.smtp.SmtpResponse;
import com.dumbster.smtp.SmtpState;
import com.trivir.idmunit.connector.mail.MailHelper;
import com.trivir.idmunit.connector.mail.TemplateHelper;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.util.LdapConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.directory.DirContext;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import static com.trivir.idmunit.connector.MailCompare.newSanitizedAttrMap;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.idmunit.connector.ConnectorUtil.getSingleValue;

// TODO: Refactor thread safety. The current implementation is inconsistent and somewhat
//  overcomplicated
public class SMTPServer extends AbstractConnector implements Runnable {

    private static final String HOST = MailHelper.HOST_LOCALHOST;
    private static final int DEFAULT_SMTP_PORT = 25;
    private static final int TIMEOUT = 500;
    private static Logger log = LoggerFactory.getLogger(SMTPServer.class);
    private static final Set<String> STNDVALIDATEATTRS;
    private static final Set<String> STNDDELETEATTRS;
    private static final Set<String> STNDTESTATTRS;
    private static final Object SERVERSTATUSLOCK = new Object();
    private static volatile ServerSocket serverSocket;
    private static volatile boolean stopped = true;
    private static volatile boolean running = false;
    private static boolean serverStartedAndConfigured = false;
    private static List<Message> unqueuedMail = new ArrayList<Message>();
    private static Map<String, List<Message>> mailQueues = new HashMap<String, List<Message>>();

    static {
        Set<String> attrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        attrs.add(MailCompare.ATTR_BODY);
        attrs.add(MailCompare.ATTR_CC);
        attrs.add(MailCompare.ATTR_FROM);
        attrs.add(MailCompare.ATTR_SUBJECT);
        attrs.add(MailCompare.ATTR_TEMPLATE);
        attrs.add(MailCompare.ATTR_TO);
        STNDVALIDATEATTRS = attrs;

        attrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        STNDDELETEATTRS = attrs;

        attrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        STNDTESTATTRS = attrs;
    }

    private List<Message> messages;
    private int port = DEFAULT_SMTP_PORT;
    private DirContext ldapContext;

    /**
     * Send response to client.
     *
     * @param out          socket output stream
     * @param smtpResponse response object
     */
    private static void sendResponse(PrintWriter out, SmtpResponse smtpResponse) {
        if (smtpResponse.getCode() > 0) {
            int code = smtpResponse.getCode();
            String message = smtpResponse.getMessage();
            out.print(code + " " + message + "\r\n");
            out.flush();
        }
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        String portString = config.get(MailHelper.CONFIG_PORT);
        if (portString != null) {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                throw new IdMUnitException(String.format("Configuration option '%s' must be a number 0-65535", MailHelper.CONFIG_PORT));
            }
        }

        String recipient = config.get(MailHelper.CONFIG_RECIPIENT);
        // Normalize
        if (recipient == null) {
            recipient = "";
        }

        String queueName = getQueueKey(recipient);

        if (mailQueues.containsKey(queueName)) {
            throw new IdMUnitException(String.format("A queue has already been configured in another connector for '%s'", recipient));
        } else {
            if (queueName.length() == 0) {
                messages = unqueuedMail;
            } else {
                messages = new ArrayList<Message>();
            }
            mailQueues.put(queueName, messages);
        }

        startServer(port);

        // Only attempt to connect if a server was specified: aka: are we using the email template validation model, or not? only attempt the ldap connection if an ldapserver was specified.
        if (config.get(LdapConnectionHelper.LDAP_CONFIG_PREFIX + BasicConnector.CONFIG_SERVER) != null) {
            log.info("The " + LdapConnectionHelper.LDAP_CONFIG_PREFIX + BasicConnector.CONFIG_SERVER + " setting was specified, attempting to get an LDAP connection to enable template comparisons . .");
            try {
                ldapContext = LdapConnectionHelper.createLdapConnection(LdapConnectionHelper.LDAP_CONFIG_PREFIX, config);
            } catch (IdMUnitException e) {
                throw new IdMUnitException("An LDAP connection was unable to be made. A valid LDAP configuration is required for email template validation.", e);
            }
        } else {
            log.info("The '" + LdapConnectionHelper.LDAP_CONFIG_PREFIX + BasicConnector.CONFIG_SERVER + "' setting was not specified, not using the template validation mode.");
        }
    }

    public void tearDown() throws IdMUnitException {
        // TODO: BUG: unqueuedMail, an ArrayList, is not thread-safe
        if (unqueuedMail.size() > 0) {
            log.warn(String.format("The unqueued mail list contains '%d' mails.", unqueuedMail.size()));
        }

        stopServer();

        LdapConnectionHelper.destroyLdapConnection(ldapContext);
    }

    public int getPort() {
        return port;
    }

    private void startServer(int serverPort) throws IdMUnitException {
        synchronized (SERVERSTATUSLOCK) {
            if (!serverStartedAndConfigured) {
                stopped = false;
                try {
                    serverSocket = new ServerSocket(serverPort);
                } catch (IOException e) {
                    throw new IdMUnitException("Unable to create server socket", e);
                }

                try {
                    serverSocket.setSoTimeout(TIMEOUT);
                } catch (SocketException e) {
                    throw new IdMUnitException("Unable to set socket timeout", e);
                }

                Thread t = new Thread(this);
                t.start();

                // wait for proper startup. not elegant, but should work for what we need.
                while (!running) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        log.warn("Sleep interrupted. Nothing to worry about unless you see hundreds of this message.");
                    }
                }

                serverStartedAndConfigured = true;
            }
        }
    }

    private void stopServer() {
        synchronized (SERVERSTATUSLOCK) {
            if (serverStartedAndConfigured) {
                stopped = true;

                try {
                    // Kick the server accept loop
                    serverSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
                serverStartedAndConfigured = false;
            }

            // Wait for proper shutdown. not elegant, but should work for what we need.
            while (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.warn("Sleep interrupted. Nothing to worry about unless you see hundreds of this message.");
                }
            }

            // Reset the queues.
            mailQueues = new HashMap<String, List<Message>>();
            unqueuedMail = new ArrayList<Message>();
        }
    }

    public void opValidateObject(Map<String, Collection<String>> data) throws IdMUnitException {
        log.warn("Operation 'ValidateObject' is deprecated. Use the 'Validate' operation instead.");
        opValidate(data);
    }

    // NOTE: Added synchronized because access to the messages data structure must be thread-safe
    public synchronized void opValidate(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        // Create defensive shallow copy with trimmed keys
        // TODO: Move to core
        Map<String, Collection<String>> validatedAttrs = newSanitizedAttrMap(expectedAttrs);

        // Validate attributes to check for end user typos
        Set<String> validAttrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        validAttrs.addAll(STNDVALIDATEATTRS);

        String templateDn = getSingleValue(validatedAttrs, MailCompare.ATTR_TEMPLATE);
        Document template;
        if (!isBlank(templateDn)) {
            if (ldapContext == null) {
                throw new IdMUnitException("Check your LDAP configuration. Unable to perform template validation without a LDAP connection");
            }

            String templateData = TemplateHelper.getTemplateData(ldapContext, templateDn);
            if (isBlank(templateData)) {
                throw new IdMUnitException(String.format("Missing template data for DN '%s'", templateDn));
            }

            template = TemplateHelper.toDocument(templateData);
            Map<String, String> tokens = TemplateHelper.getTokensAsMap(TemplateHelper.getNodeList(template, TemplateHelper.XPATH_TOKEN_DESCRIPTION));
            validAttrs.addAll(tokens.keySet());
            // Overwrite DN with actual template data
            validatedAttrs.put(MailCompare.ATTR_TEMPLATE, Arrays.asList(templateData));
        }

        Collection<String> unrecognizedAttrs = MailCompare.getUnrecognizedAttributes(validAttrs, validatedAttrs);
        if (unrecognizedAttrs.size() > 0) {
            throw new IdMUnitException(
                    String.format("Unsupported attribute(s): %s",
                            Arrays.toString(unrecognizedAttrs.toArray())));
        }


        // For now, assert that we only have one email in the server . .
        log.info("Received [" + getMessageCount() + "] email messages to validate against the given criteria . .");

        // Check all received email against the single email data row provided - see if any received email match the row data.
        boolean haveSucceeded = false;
        Iterator<Message> emailIter = getMessages();
        List<String> allEmailResults = new ArrayList<String>();
        while (!haveSucceeded && emailIter.hasNext()) {
            List<String> singleEmailResults;

            try {
                singleEmailResults = MailCompare.validateEmailMessage(validatedAttrs, emailIter.next());
            } catch (IdMUnitException e) {
                throw new IdMUnitException("Error creating message: " + e.getMessage(), e);
            }

            if (singleEmailResults.size() != 0) {
                StringBuilder results = new StringBuilder();
                for (String singleEmailResult : singleEmailResults) {
                    results.append(singleEmailResult).append("\n");
                }
                allEmailResults.add(results.toString());
            } else {
                haveSucceeded = true;
                emailIter.remove();
            }
        }

        // Done validating all email - if we haven't succeeded, dump all failures.
        if (!haveSucceeded) {
            StringBuilder msg = new StringBuilder("Received a total of [").append(getMessageCount()).append("] email message(s), and was unable to find any that match your expected values.  Listing messages now:");
            int count = 0;
            for (String allEmailResult : allEmailResults) {
                msg.append("\n===========================================================\nMessage [").append(++count).append("]:\n");
                msg.append(allEmailResult);
            }
            throw new IdMUnitFailureException(msg.toString());
        }
    }

    /**
     * Main loop of the SMTP server.
     */
    public void run() {
        stopped = false;
        running = true;
        try {
            // Server: loop until stopped
            while (!isStopped()) {
                // Start server socket and listen for client connections
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    if (socket != null) {
                        socket.close();
                    }
                    continue; // Non-blocking socket timeout occurred: try accept() again
                }

                // Get the input and output streams
                synchronized (this) {
                    /*
                     * We synchronize over the handle method and the list update because the client call completes inside
                     * the handle method and we have to prevent the client from reading the list until we've updated it.
                     * For higher concurrency, we could just change handle to return void and update the list inside the method
                     * to limit the duration that we hold the lock.
                     */
                    List<Message> msgs = handleTransaction(socket);

                    for (Message msg : msgs) {
                        boolean addedToUnqueued = false;
                        for (Address address : msg.getAllRecipients()) {
                            InternetAddress addressParts = new InternetAddress(address.toString());
                            String queueKey = getQueueKey(addressParts.getAddress());
                            if (mailQueues.containsKey(queueKey)) {
                                mailQueues.get(queueKey).add(msg);
                            } else {
                                // only add it if it hasn't been added to the unqueuedMail list.
                                if (!addedToUnqueued) {
                                    unqueuedMail.add(msg);
                                }

                                // this message has been added to the unqueuedMail list, don't
                                // add it again for other recipients
                                addedToUnqueued = true;
                            }
                        }
                    }
                }
                socket.close();
            }
        } catch (IOException e) {
            /* @todo Should throw an appropriate exception here. */
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            running = false;
        }
    }

    /**
     * Check if the server has been placed in a stopped state. Allows another thread to
     * stop the server safely.
     *
     * @return true if the server has been sent a stop signal, false otherwise
     */
    private synchronized boolean isStopped() {
        return stopped;
    }

    /**
     * Handle an SMTP transaction, i.e. all activity between initial connect and QUIT command.
     *
     * @return List of SmtpMessage
     * @throws IOException
     */
    private List<Message> handleTransaction(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        PrintWriter out = new PrintWriter(socket.getOutputStream());

        // Initialize the state machine
        SmtpState smtpState = SmtpState.CONNECT;
        SmtpRequest smtpRequest = new SmtpRequest(SmtpActionType.CONNECT, "", smtpState);

        // Execute the connection request
        SmtpResponse smtpResponse = smtpRequest.execute();

        // Send initial response
        sendResponse(out, smtpResponse);
        smtpState = smtpResponse.getNextState();

        List<Message> msgList = new ArrayList<Message>();
        Message message = null;

        while (smtpState != SmtpState.CONNECT) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            SmtpRequest request;
            if (SmtpState.DATA.equals(smtpState)) {
                boolean beginning = true;
                while (true) {
                    int b = input.read();
                    if (beginning) {
                        if (b == '.') {
                            b = input.read();
                            if (b == '\r') {
                                input.read(); // read '\n'
                                break;
                            } else if (b != '.') {
                                os.write('.');
                            }
                        }
                        beginning = false;
                    }
                    if (b == '\r') {
                        os.write(b);
                        b = input.read(); // read '\n'
                        beginning = true;
                    }
                    os.write(b);
                }

                // Store input in message
                try {
                    message = getMessage(os.toByteArray());
                } catch (IOException e) {
                    // TODO In the future, what do we want to do other than skipping this message?
                } catch (MessagingException e) {
                    // TODO In the future, what do we want to do other than skipping this message?
                }
                request = new SmtpRequest(SmtpActionType.DATA_END, "", smtpState);
            } else {
//              String line = input.readLine();
                //
//              if (line == null) {
//              break;
//              }
                int prevByte = input.read();
                while (true) {
                    int b = input.read();
                    if (prevByte == 0x0D && b == 0x0A) {
                        break;
                    }
                    os.write(prevByte);
                    prevByte = b;
                }
                String line = os.toString("UTF-8");
                // Create request from client input and current state
                request = SmtpRequest.createRequest(line, smtpState);
            }

            // Execute request and create response object
            SmtpResponse response = request.execute();

            // Move to next internal state
            smtpState = response.getNextState();
            // Send response to client
            sendResponse(out, response);

            // If message reception is complete save it
            if (smtpState == SmtpState.QUIT) {
                if (message != null) {
                    msgList.add(message);
                    message = null;
                }
            }
        }

        return msgList;
    }

    public MimeMessage getMessage(byte[] data) throws MessagingException, IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        Session session = Session.getDefaultInstance(System.getProperties());
        return new MimeMessage(session, is);
    }

    /**
     * Get email received by this instance since start up.
     *
     * @return List of String
     */
    // NOTE: Made private because Iterators are not thread safe
    private Iterator<Message> getMessages() {
        return messages.iterator();
    }

    /**
     * Get the number of messages received.
     *
     * @return size of received email list
     */
    public synchronized int getMessageCount() {
        return messages.size();
    }

    private String getQueueKey(String recipient) {
        return recipient.trim().toUpperCase();
    }

    public void opTestConnection(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        // Create defensive shallow copy with trimmed keys
        // TODO: Move to core
        Map<String, Collection<String>> validatedAttrs = newSanitizedAttrMap(expectedAttrs);

        // Validate attributes to check for end user typos
        Set<String> validAttrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        validAttrs.addAll(STNDTESTATTRS);

        Collection<String> unrecognizedAttrs = MailCompare.getUnrecognizedAttributes(validAttrs, validatedAttrs);
        if (unrecognizedAttrs.size() > 0) {
            throw new IdMUnitException(
                    String.format("Unsupported attribute(s): %s",
                            Arrays.toString(unrecognizedAttrs.toArray())));
        }

        Socket s = null;

        try {
            s = new Socket(HOST, port);
        } catch (IOException e) {
            throw new IdMUnitException(
                    String.format(
                            "An error occurred connecting to %s:%s: '%s'",
                            HOST,
                            port,
                            e.getMessage()), e);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    //TODO: enhance to include search parameters to limit the mail being deleted
    public synchronized void opDeleteMail(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
        // Create defensive shallow copy with trimmed keys
        // TODO: Move to core
        Map<String, Collection<String>> validatedAttrs = newSanitizedAttrMap(expectedAttrs);

        // Validate attributes to check for end user typos
        Set<String> validAttrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        validAttrs.addAll(STNDDELETEATTRS);

        Collection<String> unrecognizedAttrs = MailCompare.getUnrecognizedAttributes(validAttrs, validatedAttrs);
        if (unrecognizedAttrs.size() > 0) {
            throw new IdMUnitException(
                    String.format("Unsupported attribute(s): %s",
                            Arrays.toString(unrecognizedAttrs.toArray())));
        }

        messages.clear();
    }
}

