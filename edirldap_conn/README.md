# eDirectory LDAP Connector

The eDirectory LDAP connector extends the LdapConnector. It adds additional methods to validate whether a password exists on an object in eDirectory.

This connector does not validate the connected system is eDirectory. The connector does not currently validate that the object referenced by "dn" is a valid user object. It is the responsibility of the develop to specify a valid user object dn.

## Operations

---

### PasswordExists

Validates that the object's password exists.

#### Params

- **dn** - LDAP DN of the object to validate.

---

### PasswordDoesNotExist

Validates that the object's password does not exist.

#### Params

- **dn** - LDAP DN of the object to validate.

---

## Configuration

To configure this connector you need to specify a server, user and password and optionally a keystore-path.

```xml
<connection>  
    <name>EDIR</name>  
    <description>Connector to an eDirectory LDAP server</description>  
    <type>com.trivir.idmunit.connector.EdirLdapConnector</type>  
    <server>192.168.1.3</server>  
    <user>cn=admin,o=services</user>  
    <password>B2vPD2UsfKc=</password>  
    <multiplier/>  
    <substitutions/>  
    <data-injections/>  
</connection>
```

