Google Apps Connector
=====================
The Google Apps Connector is used to validate and manage User, Group, Alias, and SendAs objects in GoogleApps.
NOTE: That group operations do NOT work, and are in the process of being developed to work with the new Google API.

OPERATIONS
==========
Refer to the Schema section for objectClass and objectClass specific field information.

AddGroupMember: Add a user to a group
* groupId - Group ID
* username = User's username
		
CreateObject: Create an object (User,Group,Alias,SendAs)
* objectClass - A supported objectClass
* [fields] - objectClass specific fields
Required User fields:
* givenName - First name for account
* familyName - Last name for account
* password - Password for account
Required Group fields:
* groupEmail - Group email address
Required Alias fields:
* userKey - Primary email address for account
* alias - New Alias email address to add
Required SendAs fields:
* userId - Primary gmail email address for account
* sendAsEmail - New SendAs email address to add

DeleteObject: Delete the specified object (User,Group,Alias,SendAS)
* objectClass - A supported objectClass
* [objectId] - A field named for the objectClass appropriate naming attribute.

ModifyObject: Modify the specified object (User,Group,SendAs)
* objectClass - A supported objectClass
* [objectId] - A field named for the objectClass appropriate naming attribute.
* [fields] - objectClass specific fields

ClearAttr (User)
* objectClass - a supported objectClass (Note: As of now, only User is supported)
* username - Username of user who you would like to clear attributes from.
* [column names] - Names of attributes you would like to clear with '*' as the value. Supported attributes are: familyName, givenName, phones, organizations. When 'phones' is present, all phones will be removed (e.g. workPhone, homePhone, mobilePhone). When 'organizations' is present, all organization-related attributes will be cleared (e.g. orgName, orgTitle, orgDepartment, orgDescription, orgOffice).

RemoveGroupMember: Remove a user from a group
* groupId - Group ID
* username = User's username

RenameObject: Rename an object (User,Group)
* [groupId/username] - current object ID
* [newGroupId/newUsername] - new object ID
	
ValidateObject: Validate the specified object (User,Group,Alias,SendAs)
* objectClass - A supported objectClass
* [objectId] - A field named for the objectClass appropriate naming attribute.
* [fields] - objectClass specific fields
	
ValidatePassword: Validate a user's password.
NOTE: it is best to use a delay and single validation. Repeated validation can lead to account suspension, leading to
difficulty in deleting the account.
* username - User's username
* password - User's password


User
----
* objectClass - User
* username (naming, single-valued, string) - User ID
* givenName (single-valued, string) - The user's first name. Required when creating a user account.
* familyName (single-valued, string) - The user's last name. Required when creating a user account.
* password (single-valued, string) - Stores the password for the user account. The user's password value is required
when creating a user account. It is optional when updating a user and should only be provided if the user is updating
their account password.
* suspended (single-valued, boolean) - Indicates if the user is suspended.
* mustChangePassword (single-valued, boolean) - Require user to change password at next login
* ou (single-valued, string) - Organizational unit
* orgName (single-valued, string) - Organization name
* orgTitle (single-valued, string) - The user's title within the organization, for example 'member' or 'engineer'.
* orgDepartment (single-valued, string)- Specifies the department within the organization, such as 'sales' or
'engineering'.
* orgDescription (single-valued, string) - The description of the organization.
* orgOffice (single-valued, string) - The physical location of the organization. This does not need to be a fully
qualified address.
* workPhone (single-valued, string) - The user's work phone number.
* homePhone (single-valued, string) - The user's home phone number.
* mobilePhone (single-valued, string) - The user's mobile phone number.
* employeeId (single-valued, string) - The user's Employee ID.
* aliases (multi-valued, string) - The user's email aliases.
* includeInGlobalAddressList (single-valued, boolean) - Indicates if the user's profile is visible in the G Suite global
address list when the contact sharing feature is enabled for the domain.


Group
-----
* objectClass - Group
* groupId (naming, single-valued, string) - Group ID
* groupName (single-valued, string) - Group name. During creation, defaults to groupId value if not specified
* groupDescription  (single-valued, string) - Group description
* emailPermission (single-valued, string) - Email permission for the group
* groupMembers (multi-valued, string) - Usernames of group members


Alias
-----
REFERENCE: https://developers.google.com/admin-sdk/directory/v1/reference/users/aliases#resource
* objectClass - Alias
* userKey (naming, single-valued, string) - Identifies the user in the API request. The value can be the user's primary
email address, alias email address, or unique user ID.
* alias - The alias email address.
* primaryEmail - The user's primary email address.


SendAs
-----
REFERENCE: https://developers.google.com/gmail/api/v1/reference/users/settings/sendAs#resource
* objectClass - SendAs
* userId (naming, single-valued, string) - User's email address. The special value "me" can be used to indicate the
authenticated user.
* displayName (string) - A name that appears in the "From:" header for mail sent using this alias. For custom "from"
addresses, when this is empty, Gmail will populate the "From:" header with the name that is used for the primary address
associated with the account.
* sendAsEmail (string) - The email address that appears in the "From:" header for mail sent using this alias. This is
read-only for all operations except create.
* replyToAddress (string) - An optional email address that is included in a "Reply-To:" header for mail sent using this
alias. If this is empty, Gmail will not generate a "Reply-To:" header.
* signature (string) - An optional HTML signature that is included in messages composed with this alias in the Gmail web
 UI.
* isPrimary (boolean) - Whether this address is the primary address used to login to the account. Every Gmail account
has exactly one primary address, and it cannot be deleted from the collection of send-as aliases. This field is
read-only.
* isDefault (boolean) - Whether this address is selected as the default "From:" address in situations such as composing
a new message or sending a vacation auto-reply. Every Gmail account has exactly one default send-as address, so the only
legal value that clients may write to this field is true. Changing this from false to true for an address will result in
this field becoming false for the other previous default address.
* treatAsAlias (boolean) - Whether Gmail should treat this address as an alias for the user's primary email address.
This setting only applies to custom "from" aliases.


CONFIGURATION
=============
You can either configure this with a p12KeyFile OR the <privateKey> from the json document. NOTE: The list of scopes is
the recommended list. Depending upon your use case, you may need to reduce the scopes list. For the scopes required
to execute various API calls, consult
https://developers.google.com/admin-sdk/directory/v1/reference/,
https://developers.google.com/gmail/api/v1/reference/ and
https://developers.google.com/admin-sdk/licensing/v1/reference/.

p12KeyFile configuration:
    <connection>
        <name>GApps</name>
        <description>Connector to Google Apps</description>
        <type>com.trivir.idmunit.connector.GoogleAppsConnector</type>
        <superUserEmail>adminUser@domain.com</superUserEmail>
        <serviceEmail>54444444455555-orqbc7gpi2c5k3tm2ci94eq3mmpdenlr@developer.gserviceaccount.com</serviceEmail>
        <p12keyFile>Google-IdMUnit-Dev-1ee22e8066f0.p12</p12keyFile> 		<!-- You can also specifi the privateKey from the json instead. See below. -->        <multiplier/>
        <scopes>https://mail.google.com/,https://www.googleapis.com/auth/admin.directory.group,https://www.googleapis.com/auth/admin.directory.group.member,https://www.googleapis.com/auth/admin.directory.group.member.readonly,https://www.googleapis.com/auth/admin.directory.group.readonly,https://www.googleapis.com/auth/admin.directory.orgunit,https://www.googleapis.com/auth/admin.directory.orgunit.readonly,https://www.googleapis.com/auth/admin.directory.user,https://www.googleapis.com/auth/admin.directory.user.alias,https://www.googleapis.com/auth/admin.directory.user.alias.readonly,https://www.googleapis.com/auth/admin.directory.user.readonly,https://www.googleapis.com/auth/admin.directory.user.security,https://www.googleapis.com/auth/admin.directory.userschema,https://www.googleapis.com/auth/admin.directory.userschema.readonly,https://www.googleapis.com/auth/apps.licensing,https://www.googleapis.com/auth/gmail.modify,https://www.googleapis.com/auth/gmail.readonly,https://www.googleapis.com/auth/gmail.settings.basic,https://www.googleapis.com/auth/gmail.settings.sharing</scopes>
        <substitutions/>
        <data-injections/>
    </connection>

privateKey configuration:
    <connection>
        <name>GApps</name>
        <description>Connector to Google Apps</description>
        <type>com.trivir.idmunit.connector.GoogleAppsConnector</type>
        <superUserEmail>adminUser@domain.com</superUserEmail>
        <serviceEmail>54444444455555-orqbc7gpi2c5k3tm2ci94eq3mmpdenlr@developer.gserviceaccount.com</serviceEmail>
		<privateKey>-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCo+mMnlrND2ED8\nrGunysbNI7ezDB2ABf08WN4qKcYECllReOD6QTWiLa6HxgO+zr/zgNas1GZsLY6/\n6anyVPyMNohifLQIrCjSLIZEZ/3i3PxfbRKAwiexXyvVSwU4KQkVxbcVrcHO/aBX\neP5bY+DRrQzYhaywNF2UY9xMwkcYoqZ8TZotC1SRh0R3ycYKBFbusa0GfQHgZC4A\nsJOPF2re4ligO+IUEy6Xycd/HErepmktYF4gVAU9UO+CP1DJWZa0XCwGpVJgDc3n\nEodmqM+WhJXEs81Tgt57chtRyHaTyHS478djIg5uzBgCt+Qvld0KpAk7qd+CUzOY\nB4DVNtO7AgMBAAECggEABdO8n5zeybQnaY84lyyy9lgI+a+tRMDwHqih4KpYHcON\nmPWmLHsbMmCI6D2321id3fPPc15PueNITO3t4mFz5BHtWhgazUHClpzadHlxk+Uy\n/sdwH+iSHde+VzijEXC403th+OlpCY5NfTa67sPe+NwS2VSiw+DKaVeYPT1l3j+P\nPUWfdynFVM28PWp7T6QP36RDbe37Sprwvdh3zUGeim1RbtSzlCiAgPUX3sDzw/m7\ncBcVD7hqDLJN5tDa6S0Lvg47dpUV1A2+trZDiWco6Q7zyIx3atyrCmOykiE7Wn8P\nFsotsQUr26abI0F8ibSj3i8HQlBl6MLQFqNCrwd2YQKBgQDkw/q05Tdqa7MrAIil\n+StZ+4W9B1vRknozVKC0XmA0gD4f9A2Js8tGCbDHw5y7nggkLSco1WbyI7qSkrkG\nwrC9+2GA8LTD1GWE8vL+rWC+bhwOhiBbkvSfdSqzr5NLZKM+rqsXpyQ2i+c587qL\ng7es7C+6AE8Gk5p6KqNzwjbupQKBgQC9GEhkDwzy6wgWY+qAVcoXQ1q2NG9vq+fJ\npgjjhqSoi4OdPkoqEBLSMCAnn1Am71gbHAewq7jElpolh+f2bRCAeGppS1fRBZbA\n+P8cBKSwn5amHJC6xuOqg+4JLrCyPOsI+VwxaTQdkfas6cPTjWCAc079+bHRGTse\nwb2dWliK3wKBgQDcwvNRVlHf9Qb13JIB+S6Iq10oSc2ltIEQv/5FkT2t7IQHOfFG\njacAc6GRZYf+1kZtAtXjZezuhuQpRkYf7Am7jLdPQvpucK13Wyx78LyHiKKysWTQ\nekRvDRakSC8QdpHCj7S0Iqujp+b/2eCPehDqYxK7oEiixBYzBqqtryJrsQKBgEsM\nCh78Ai2erfUE7prEcZJN/wB1SwzM8w8KwFhcpZjm1fNgN1r+Shn1U62mAs+RCiyo\nMT0iyg/rnX3rtFoO6w+xurnTVyTzzOMFb2oVRa97hvSiPaaDLPQNrDkkuKfkmXtU\nTAyoLjDWL0ps9HVPlM8l9GHD6ZiNJPON/M0ogB+NAoGAFiu9S3+nkCPhVNBUWAzZ\nGnkR+EOmVxzRaHFhwAI9jl9GbYGWO1B2MjQQUXmElGgLXOOOy1+y9dn+xr7o+Ewy\nBubftKT9aFcXns3pcglhDsf+M4izizshAoEQ4D15EyLkdjYZVAPc8hrLqrcBZ42K\nztLIe2zHj5gpxecvyn2JIzs=\n-----END PRIVATE KEY-----</privateKey>
        <scopes>https://mail.google.com/,https://www.googleapis.com/auth/admin.directory.group,https://www.googleapis.com/auth/admin.directory.group.member,https://www.googleapis.com/auth/admin.directory.group.member.readonly,https://www.googleapis.com/auth/admin.directory.group.readonly,https://www.googleapis.com/auth/admin.directory.orgunit,https://www.googleapis.com/auth/admin.directory.orgunit.readonly,https://www.googleapis.com/auth/admin.directory.user,https://www.googleapis.com/auth/admin.directory.user.alias,https://www.googleapis.com/auth/admin.directory.user.alias.readonly,https://www.googleapis.com/auth/admin.directory.user.readonly,https://www.googleapis.com/auth/admin.directory.user.security,https://www.googleapis.com/auth/admin.directory.userschema,https://www.googleapis.com/auth/admin.directory.userschema.readonly,https://www.googleapis.com/auth/apps.licensing,https://www.googleapis.com/auth/gmail.modify,https://www.googleapis.com/auth/gmail.readonly,https://www.googleapis.com/auth/gmail.settings.basic,https://www.googleapis.com/auth/gmail.settings.sharing</scopes>
		<substitutions/>
        <data-injections/>
    </connection>
	
	You can provide encrypted values in configuration by setting the "encrypted" attribute on a configuration element to "true". For example:
	<privateKey encrypted="true">A1bCD2EfgHi=</privateKey>
