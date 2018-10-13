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
import com.trivir.idmunit.connector.mail.TemplateHelper;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.util.LdapConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.mail.*;
import javax.naming.directory.DirContext;
import java.util.*;

import static com.trivir.idmunit.connector.MailCompare.newSanitizedAttrMap;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.idmunit.connector.ConnectorUtil.getSingleValue;

public class IMAPConnector extends AbstractConnector {

    private static final Logger LOG = LoggerFactory.getLogger(IMAPConnector.class);
    private static final Set<String> STNDVALIDATEATTRS;
    private static final Set<String> STNDDELETEATTRS;
    private static final Set<String> STNDTESTATTRS;

    static {
        Set<String> attrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        attrs.add(MailHelper.CONFIG_MAILBOX);
        attrs.add(MailCompare.ATTR_BODY);
        attrs.add(MailCompare.ATTR_CC);
        attrs.add(MailCompare.ATTR_FROM);
        attrs.add(MailCompare.ATTR_SUBJECT);
        attrs.add(MailCompare.ATTR_TEMPLATE);
        attrs.add(MailCompare.ATTR_TO);
        STNDVALIDATEATTRS = attrs;

        attrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        attrs.add(MailHelper.CONFIG_MAILBOX);
        attrs.add(MailHelper.CONFIG_FOLDER);
        STNDDELETEATTRS = attrs;

        attrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        attrs.add(MailHelper.CONFIG_MAILBOX);
        attrs.add(MailHelper.CONFIG_FOLDER);
        STNDTESTATTRS = attrs;
    }

    private Map<String, String> mailConfig = null;
    private Store store = null;
    private DirContext ldapContext = null;

    public void setup(Map<String, String> config) throws IdMUnitException {
        String folderName = config.get(MailHelper.CONFIG_FOLDER);
        if (isBlank(folderName)) {
            folderName = MailHelper.FOLDER_INBOX;
        }

        // Map connector params to mail params
        mailConfig = new HashMap<String, String>();
        mailConfig.put(MailHelper.CONFIG_SERVER, config.get(BasicConnector.CONFIG_SERVER));
        mailConfig.put(MailHelper.CONFIG_USER, config.get(BasicConnector.CONFIG_USER));
        mailConfig.put(MailHelper.CONFIG_PASSWORD, config.get(BasicConnector.CONFIG_PASSWORD));
        mailConfig.put(MailHelper.CONFIG_PORT, config.get(MailHelper.CONFIG_PORT));
        mailConfig.put(MailHelper.CONFIG_SSL, config.get(MailHelper.CONFIG_SSL));
        mailConfig.put(MailHelper.CONFIG_FOLDER, folderName);

        // Allow for on-demand connections
        if (isBlank(mailConfig.get(MailHelper.CONFIG_USER))) {
            LOG.info("The '" + BasicConnector.CONFIG_USER + "' parameter wasn't provided. Delaying IMAP authentication.");
        } else {
            try {
                Session session = MailHelper.newImapSession(mailConfig);
                store = MailHelper.getStore(session, mailConfig);
            } catch (MessagingException e) {
                MailHelper.close(store);
                throw new IdMUnitException(
                        String.format(
                                "An error occurred connecting to %s:%s as user '%s': '%s'",
                                config.get(MailHelper.CONFIG_SERVER),
                                config.get(MailHelper.CONFIG_PORT),
                                config.get(MailHelper.CONFIG_USER),
                                e.getMessage()), e);
            }

            Folder folder = null;

            try {
                folder = store.getFolder(folderName);
                folder.open(Folder.READ_ONLY);
                System.out.println(folderName + ": new message count:    " + folder.getNewMessageCount());
                System.out.println(folderName + ": unread message count: " + folder.getUnreadMessageCount());
                Message[] unreadMessages = folder.search(MailHelper.SEARCH_UNREAD);
                System.out.println(folderName + ": unread messages:      " + unreadMessages.length);
            } catch (MessagingException e) {
                throw new IdMUnitException(
                        String.format(
                                "An error occurred reading folder %s: '%s'",
                                folderName,
                                e.getMessage()), e);
            } finally {
                MailHelper.close(folder, true);
            }
        }

        // Only attempt to connect if a server was specified: aka: are we using the email template validation model, or not? only attempt the ldap connection if an ldapserver was specified.
        if (config.get(LdapConnectionHelper.LDAP_CONFIG_PREFIX + BasicConnector.CONFIG_SERVER) != null) {
            LOG.info("The " + LdapConnectionHelper.LDAP_CONFIG_PREFIX + BasicConnector.CONFIG_SERVER + " setting was specified, attempting to get an LDAP connection to enable template comparisons . .");
            try {
                ldapContext = LdapConnectionHelper.createLdapConnection(LdapConnectionHelper.LDAP_CONFIG_PREFIX, config);
            } catch (IdMUnitException e) {
                throw new IdMUnitException("An LDAP connection was unable to be made. A valid LDAP configuration is required for email template validation.", e);
            }
        } else {
            LOG.info("The '" + LdapConnectionHelper.LDAP_CONFIG_PREFIX + BasicConnector.CONFIG_SERVER + "' setting was not specified, not using the template validation mode.");
        }
    }

    public void tearDown() throws IdMUnitException {
        MailHelper.close(store);
        store = null;

        LdapConnectionHelper.destroyLdapConnection(ldapContext);
        ldapContext = null;
    }

    //Determines which Store to use for an operation: the existing one or a temporary one.
    private Store getOpStore(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {

        Store opStore = newMailboxStore(expectedAttrs);
        if (opStore == null) {
            opStore = store;
        }

        return opStore;
    }

    //Determines which Store to use for an operation: the existing one or a temporary one.
    private Store newMailboxStore(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {

        //Note: This should not be a reference to the "store" member variable as that may be a different Store
        // that's authenticated using different parameters
        Store mailboxStore = null;

        // If the mailbox attribute is populated, reauthenticate as that user for this operation
        if (expectedAttrs.containsKey(MailHelper.CONFIG_MAILBOX)) {
            String mailbox = getSingleValue(expectedAttrs, MailHelper.CONFIG_MAILBOX);
            if (!isBlank(mailbox)) {
                LOG.info(String.format("Authenticating as '%s'...", mailbox));
                try {
                    // Duplicate existing imap config and overrride the authentication parameters
                    Map<String, String> mailboxConfig = new HashMap<String, String>(mailConfig);
                    mailboxConfig.put(MailHelper.CONFIG_USER, mailbox);
                    mailboxConfig.put(MailHelper.CONFIG_PASSWORD, mailbox);
                    Session session = MailHelper.newImapSession(mailboxConfig);
                    mailboxStore = MailHelper.getStore(session, mailboxConfig);

                    LOG.info(String.format("Authenticated as '%s'", mailbox));
                } catch (AuthenticationFailedException e) {
                    MailHelper.close(mailboxStore);
                    String msg = String.format(
                            "Unable to authenticate as '%s': %s. It's possible that the user doesn't exist because they haven't received any mail yet.",
                            mailbox,
                            e);
                    LOG.warn(msg);
                    throw new IdMUnitException(msg, e);
                } catch (MessagingException e) {
                    MailHelper.close(mailboxStore);
                    throw new IdMUnitException(String.format("Unable to authenticate as '%s': %s", mailbox, e.getMessage()), e);
                }
            }
        }

        return mailboxStore;
    }

    @SuppressWarnings("unchecked")
    public void opValidate(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
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

        //Reauthenticate as mailbox?
        Store opStore = getOpStore(validatedAttrs);
        if (opStore == null) {
            throw new IdMUnitException(
                    String.format("Check your IMAP configuration. Either provide a '%s' for the IMAP connector or pass a '%s' attribute to this operation",
                            MailHelper.CONFIG_USER, MailHelper.CONFIG_MAILBOX));
        }

        Message[] unreadMessages;
        Folder folder = null;

        try {
            folder = opStore.getFolder(mailConfig.get(MailHelper.CONFIG_FOLDER));
            folder.open(Folder.READ_WRITE);
            unreadMessages = folder.search(MailHelper.SEARCH_UNREAD);

            // For now, assert that we only have one email in the server . .
            LOG.info("Received [" + unreadMessages.length + "] email messages to validate against the given criteria . .");

            // Check all received email against the single email data row provided - see if any received email match the row data.
            boolean haveSucceeded = false;
            List<String> allEmailResults = new ArrayList<String>();
            for (Message unreadMessage : unreadMessages) {
                List<String> singleEmailResults = MailCompare.validateEmailMessage(validatedAttrs, unreadMessage);

                if (singleEmailResults.size() != 0) {
                    StringBuffer results = new StringBuffer();
                    for (String singleEmailResult : singleEmailResults) {
                        results.append(singleEmailResult).append("\n");
                    }
                    allEmailResults.add(results.toString());
                } else {
                    haveSucceeded = true;
                    try {
                        unreadMessage.setFlag(Flags.Flag.SEEN, true);
                    } catch (MessagingException e) {
                        LOG.warn("Unable to mark validated message as read. This will cause it to be tested in further tests.");
                    }
                    break; // we're done looking!
                }
            }

            // Done validating all email - if we haven't succeeded, dump all failures.
            if (!haveSucceeded) {
                // Iterate all email messages and mark them unread so a retry is possible
                try {
                    MailHelper.applyFlag(unreadMessages, Flags.Flag.SEEN, false);
                } catch (MessagingException e) {
                    LOG.warn("Unable to mark unvalidated message as unread. This will cause it to not be tested in further tests.");
                }

                StringBuilder msg = new StringBuilder("Received a total of [").append(unreadMessages.length).append("] email message(s), and was unable to find any that match your expected values.  Listing messages now:");
                int count = 0;
                for (String allEmailResult : allEmailResults) {
                    msg.append("\n===========================================================\nMessage [").append(++count).append("]:\n");
                    msg.append(allEmailResult);
                }
                throw new IdMUnitFailureException(msg.toString());
            }
        } catch (MessagingException e) {
            throw new IdMUnitException(String.format("Failure opening folder '%s'", mailConfig.get(MailHelper.CONFIG_FOLDER)), e);
        } finally {
            MailHelper.close(folder);
            // If we used a temporary Store for this operation, close it
            if (opStore != store) {
                MailHelper.close(opStore);
            }
        }
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

        // Try to authenticate as "Mailbox" first
        try {
            store = newMailboxStore(expectedAttrs);
        } catch (IdMUnitException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AuthenticationFailedException) {
                //AuthenticationFailedException could mean the user doesn't exist yet. We connected, however.
                LOG.info("Connectivity to the server was tested. Unable to test folder connectivity due to an inability to authenticate.");
                return;
            } else {
                throw e;
            }
        }

        // Authenticate using IMAP configuration
        if (store == null) {
            try {
                Session session = MailHelper.newImapSession(mailConfig);
                store = MailHelper.getStore(session, mailConfig);
            } catch (MessagingException e) {
                MailHelper.close(store);
                throw new IdMUnitException(
                        String.format(
                                "An error occurred connecting to %s:%s as user '%s': '%s'",
                                mailConfig.get(MailHelper.CONFIG_SERVER),
                                mailConfig.get(MailHelper.CONFIG_PORT),
                                mailConfig.get(MailHelper.CONFIG_USER),
                                e.getMessage()), e);
            }
        }

        String folderName = mailConfig.get(MailHelper.CONFIG_FOLDER);
        if (isBlank(folderName)) {
            folderName = getSingleValue(validatedAttrs, MailHelper.CONFIG_FOLDER);
            if (isBlank(folderName)) {
                folderName = MailHelper.FOLDER_INBOX;
            }
        }

        Folder folder = null;

        try {
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
        } catch (MessagingException e) {
            throw new IdMUnitException(
                    String.format(
                            "An error occurred reading folder %s: '%s'",
                            mailConfig.get(MailHelper.CONFIG_FOLDER),
                            e.getMessage()), e);
        } finally {
            MailHelper.close(folder, true);
            MailHelper.close(store);
        }
    }

    // TODO: Enhance to include search parameters to limit the mail being deleted
    // TODO: Add test to delete from a folder other than Inbox
    public void opDeleteMail(Map<String, Collection<String>> expectedAttrs) throws IdMUnitException {
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

        String folderName = getSingleValue(expectedAttrs, MailHelper.CONFIG_FOLDER);
        if (isBlank(folderName)) {
            folderName = mailConfig.get(MailHelper.CONFIG_FOLDER);
        }

        //Reauthenticate as mailbox?
        Store opStore;

        try {
            opStore = getOpStore(expectedAttrs);
        } catch (IdMUnitException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AuthenticationFailedException) {
                //AuthenticationFailedException could mean the user doesn't exist yet
                return;
            } else {
                throw e;
            }
        }

        if (opStore == null) {
            throw new IdMUnitException(
                    String.format("Check your IMAP configuration. Either provide a '%s' for the IMAP connector or pass a '%s' attribute to this operation",
                            MailHelper.CONFIG_USER, MailHelper.CONFIG_MAILBOX));
        }

        try {
            Folder folder = null;

            try {
                folder = opStore.getFolder(folderName);
                folder.open(Folder.READ_WRITE);
            } catch (MessagingException e) {
                MailHelper.close(folder);
                throw new IdMUnitException(
                        String.format(
                                "An error occurred while opening folder %s: '%s'",
                                folderName,
                                e.getMessage()), e);
            }

            try {
                MailHelper.deleteAllMessages(folder);
            } catch (MessagingException e) {
                throw new IdMUnitException(
                        String.format(
                                "An error occurred while deleting messages from folder %s: '%s'",
                                folderName,
                                e.getMessage()), e);
            } finally {
                MailHelper.close(folder, true);
            }
        } finally {
            // If we used a temporary Store for this operation, close it
            if (store != opStore) {
                MailHelper.close(opStore);
            }
        }
    }

}
