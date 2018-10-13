Mail Connector
--------------
The Mail connector provides both an IMAP Connector and SMTP server to receive email to be validated from IdMUnit tests.

The SMTP Server connector is intended to be used for testing in development and testing environments without an email server. It acts as an SMTP server target for processes needing an SMTP server. The SMTP connector keeps a cache of incoming emails and iterates over those emails to find a match. When a match is found it is removed from the incoming queue.

The IMAP Connector acts as an IMAP client. It iterates through all unread messages in an IMAP inbox looking for a match. When a match is found it is marked as read.

Both connectors are capable of matching against the full text of a message or against an IDM email template specified by a DN. When matching against an email template, the values of email template fields are specified in individual email field columns.

The connectors are designed so they may be used interchangeably in different IdMUnit test profiles.

OPERATIONS
----------
Validate: Validates received email messages. Only one of "Body" or "Template" may be specified
* Body - The body text to match against.
* Template - The DN of an IDM email template to match against.
* To - A TO recipient to match against.
* CC - A CC recipient to match against.
* From - A sender to match against.
* [template_properties] - If "Template" is specified, email template properties to match against.
TestConnection: Tests the IMAP or SMTP connection. The default folder is "Inbox".
* Mailbox - The account to authenticate as. (IMAPConnector only)
* Folder - The folder to attempt to read. (IMAPConnector only)
DeleteMail: Deletes all mail in the specified folder. The default folder is "Inbox".
* Mailbox - The account to authenticate as. (IMAPConnector only)
* Folder - The folder to delete mail from. If no value is provided, the default is used. (IMAPConnector only)

CONFIGURATION
-------------
This connector does not have any connector specific configuration parameters.

    <connection>
        <name>Mail</name>
        <description>Mail Connector</description>
        <type>com.trivir.idmunit.connector.SMTPServer</type>
        <port>25</port>
        <!-- recipient: An email recipient this connector should track. Case insensitive. May be left empty to denote a catch-all queue -->
        <recipient>test1@example.com</recipient>
        <!-- An LDAP server must be specified when performing IDM email template validation -->
        <ldapserver>192.168.1.3</ldapserver>
        <ldapuser>cn=admin,o=services</ldapuser>
        <ldappassword>B2vPD2UsfKc=</ldappassword>
        <ldaptrust-all-certs>true</ldaptrust-all-certs>
        <ldapuse-tls>true</ldapuse-tls>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>

    <connection>
        <name>Mail</name>
        <description>Mail Connector</description>
        <type>com.trivir.idmunit.connector.IMAPConnector</type>
        <user>user</user>
        <password>B2vPD2UsfKc=<password>
        <server>example.com</server>
        <port>143</port>
        <ssl>false</ssl>
        <folder>Inbox<folder> <!-- the default folder (mailbox) is "Inbox" -->
        <!-- An LDAP server must be specified when performing IDM email template validation -->
        <ldapserver>192.168.1.3</ldapserver>
        <ldapuser>cn=admin,o=services</ldapuser>
        <ldappassword>B2vPD2UsfKc=</ldappassword>
        <ldaptrust-all-certs>true</ldaptrust-all-certs>
        <ldapuse-tls>true</ldapuse-tls>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>
