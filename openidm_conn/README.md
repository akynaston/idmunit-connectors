# OpenIDM Connector

The OpenIDM Connector allows you to manage objects and the sync process for the ForgeRock OpenIDM platform. This connector handles managed users, links to external applications, and the recon process.

Validation of managed users can handle complex and array attributes, as well as customary single and multivalue attribute.

## INSTALLATION

The OpenIDM connector (openidm.jar) requires gson-2.3.jar as well as ojdbc6.jar to be added to your classpath.

## OPERATIONS

> **Note: Attribute Postfix Specifiers**\
> This connector needs to explicitly force attribute types. These postfixes are can be added to the end of an attribute in the the column header.

There are currently three supported attribute type specifiers that can be used in the column header:

- `[]` Open and close brackets can be used to treat the attribute as an array type: 'groups[]' or 'description[]'.

- `::boolean` can be used to force an attribute to be treated as a boolean, as in 'loginDisabled::boolean'.

- `::string` can be used to force an attribute to be treated like a string: surname::string

---

### AddObject

#### Params

- **[field names]** - values to write to the managed user. Any attributes required by the managed object must be included in the row.
- **[objectType]** - requires this field to indicate type of object we are adding. (ex. user)
- Supports the attribute specifiers as shown in the note above.

---

### DeleteObject

#### Params

- **[_id]** OR **[userName]** - requires one of these fields to determine which managed user to remove. Will also remove all links associated with that user account.
- **[objectType]** - requires this field to indicate type of object we are deleting. (ex. user)
- Will not fail if the user does not exist.

---

### DeleteObjectLeaveLinks

#### Params

- **[_id]** OR **[userName]** - same as DeleteUser, but leaves the links behind.
- **[objectType]** - requires this field to indicate type of object we are deleting. (ex. user)
- Will not fail if the user does not exist.

---

### Reconcile

#### Params

- **[mapping]** - the name of the mapping that will be reconciled.
- **[\<search attribute for source system>]** - attribute to use in a query for a single user(e.g. "_id" or "userName" if querying the managedUser). Requires a single search attribute.

---

### LiveSync

#### Params

- **[sourceSystem]** - the source system used in initiating a LiveSync. Should be the same format found in the mapping (e.g. system/ldap/account).

---

### ValidateObject

#### Params

- **[_id]** OR **[userName]** - the user to be validated
- **[field names]** - values to compare with fields from the managed user
- **[objectType]** - requires this field to indicate type of object we are adding. (ex. user)
- Supports the attribute specifiers as shown in the note above.
- You can validate null or missing values with "[EMPTY]"
- You can test complex attibutes (a composite attribute made up of sub attributes (i.e. courseGroups _> (string Name, boolean Active, string Type)))
- Test complex attributes by using a period (.) notation in the column header. Left of the period is the attribute name, right of the period is the sub attribute name (i.e. courseGroups.Type)

---

### ValidateObjectExact

#### Params

- **[_id]** OR **[userName]** - the user to be validated
- **[field names]** - values to compare with fields from the managed user
- **[objectType]** - requires this field to indicate type of object we are adding. (ex. user)
- Supports the attribute specifiers as shown in the note above.
- You can validate null or missing values with "[EMPTY]"
- You can test complex attibutes (a composite attribute made up of sub attributes (i.e. courseGroups _> (string Name, boolean Active, string Type)))
- Test complex attributes by using a period (.) notation in the column header. Left of the period is the attribute name, right of the period is the sub attribute name (i.e. courseGroups.Type)
- Same as above validation method, except this is used to exactly validate complex attributes.

---

### ReplaceAttribute

#### Params

- **[_id]** OR **[userName]** - the user to be modified.
- **[field names]** - values to update on the managed user.
- **[objectType]** - requires this field to indicate type of object we are modifying. (ex. user)
- Supports the attribute specifiers as shown in the note above.

Note that in this update, OpenIDM __replaces__ all values in the attribute with the ones passed in.

---

### RemoveAttribute

#### Params

- **[_id]** OR **[userName]** - the user to be modified.
- **[field names]** - values to remove on the managed user.
- **[objectType]** - requires this field to indicate type of object we are modifying by removing an attribute. (ex. user)
- Supports the attribute specifiers as shown in the note above.

---

### AddAttribute

#### Params

- **[_id]** OR **[userName]** - the user to be modified.
- **[field names]** - values to be added on the managed user.
- **[objectType]** - requires this field to indicate type of object we are adding an attribute to. (ex. user)
- Supports the attribute specifiers as shown in the note above.

---

## CONFIGURATION

The example connector below connects to the REST endpoints over a non SSL port.

To make this use SSL, you must add the trusted cert from OpenIDM to the java cert store used by IdMUnit.

In addition, update the \<port> and the \<sslConnection> values.

```xml
<connection> <!-- OpenIDM -->
    <name>OIDM</name>
    <description>Connector for OIDM</description>                
    <type>com.trivir.idmunit.connector.OpenIdmConnector</type>
    <server>172.17.2.35</server>
    <port>8080</port>
    <sslConnection>false</sslConnection>
    <user>openidm-admin</user>
    <password>xtXVCEVo8ewcwC8ykv3ACw==</password>
    <multiplier>
        <retry>0</retry>
        <wait>0</wait>
    </multiplier>
    <substitutions>
        <substitution>
            <replace>%mapperToLdap%</replace>
            <new>managedUser_sourceLdapAccount</new>
        </substitution>
    </substitutions>
    <data-injections/>
</connection>
```

## DATA INJECTOR - ID FROM QUERY FILTER

In addition to the data injectors that come as part of the base IdMUnit product, an additional custom data injector has been packaged with the OpenIDM IdMUnit connector.

This data injector allows for the dynamic substitution of a system object _id based on a custom _queryFilter.

A request will be made to the OpenIDM system specified in configuration with the intention to target an individual object.

The system will respond with that system object's _id, and that value can be substituted into an IdMUnit spreadsheet based on the substitution key provided in config.

The following is a sample configuration of this data injector:

```xml
<data-injection>
    <type>com.trivir.idmunit.injector.IdFromQueryFilterInjection</type>
    <key>%MANAGED_USER_ID%</key>
    <mutator>{ 'host' : 'my-openidm.example.com', 'port' : '8443', 'systemObject' : 'managed/user', 'queryFilter' : 'userName+eq+"testuser"', 'useSSL' : true, 'trustAllCerts' : true, 'username' : 'openidm-admin', 'password' : 'openidm-admin' }</mutator>
</data-injection>
```

The mutator expects a JSON object with the following required properties:

- 'host' - The hostname of your OpenIDM server.
- 'port' - The port used by your OpenIDM server.
- 'systemObject' - The system & objectType combination used to locate your targeted object's _id (e.g. system/ldap/account).
- 'queryFilter' - The query used to determine which object _id to return. This must be in OpenIDM queryFilter format, and must only match one object (e.g. sn+eq+"costello").

The mutator can also be configured with the following optional properties:

- 'useSSL' - Will attempt to use HTTPS protocol when querying OpenIDM. [DEFAULT: false]
- 'trustAllCerts' - Used in combination with 'useSSL', will accept any cert presented by the server. [DEFAULT: false]
- 'username' - The username of the account used to authenticate to OpenIDM. [DEFAULT: 'openidm-admin']
- 'password' - The password of the account used to authenticate to OpenIDM. [DEFAULT: 'openidm-admin']

The 'password' value can also be passed in as an encrypted password. The password must be encrypted using the IdMUnit EncTool.

After the password has been encrypted, the property value for 'password' can be prepended with 'ENC::' to inform the injector that

it must decrypt before using the password in the query. The following is an example config using an encrypted password:

```xml
<data-injection>
    <type>com.trivir.idmunit.injector.IdFromQueryFilterInjection</type>
    <key>%MANAGED_USER_ID%</key>
    <mutator>{ 'host' : 'my-openidm.example.com', 'port' : '8443', 'systemObject' : 'managed/user', 'queryFilter' : 'userName+eq+"testuser"', 'useSSL' : true, 'trustAllCerts' : true, 'username' : 'openidm-admin', 'password' : 'ENC::abABCDEFc1defgG2hij3HIk==' }</mutator>
</data-injection>
```

After the injector has been configured, the _id would be returned (e.g. '6c588d3e-7c29-476d-b6d8-5db02906ac9e') and can be substituted using the string set inside the <key> tags (e.g. %MANAGED_USER_ID%)

DATA INJECTOR - URI FROM QUERY FILTER
-------------------------------------

Also included is an injector that will substitute a URI that points to the object ID with systemObject context. Configuration is identical, except the <type> should be 'com.trivir.idmunit.injector.URIFromQueryFilterInjection'.

After the injector has been configured, a URI to the object would be returned (e.g. 'managed/user/6c588d3e-7c29-476d-b6d8-5db02906ac9e') and can be substituted using the string set inside the <key> tags (e.g. %MANAGED_USER_URI%)

