# UserApp Role Connector

The UserApp Role connector provides methods to allow you to assign and revoke IDM roles.It also allows you to check a user's role assignments.

## Operations

---

### TestConnection

Test the connection to the configured UserApp Roles connection.

---

### AssignRole

Sends a role assignment request to the User Application.

#### Params

- **dn** - The DN of the user to receive the role.
- **RoleDn** - The DN of the role to assign.
- **<*>** - All other fields are passed as data items to the workflow.

---

### RevokeRole

Sends a role revokation request to the User Application.

#### Params

- **dn** - The DN of the user to receive the role.
- **RoleDn** - The DN of the role to assign.
- **<*>** - All other fields are passed as data items to the workflow.

---

### UserInRoles

Verifies that the user is in the role(s).

#### Params

- **dn** - The DN of the user to check.
- **RoleDn** - The DN(s) of the roles to check.
- **<*>** - All other fields are passed as data items to the workflow.

---

### UserNotInRoles

Verifies that the user is not in the role(s).

#### Params

- **dn** - The DN of the user to check.
- **RoleDn** - The DN(s) of the roles to check.
- **<*>** - All other fields are passed as data items to the workflow.

---

## Configuration

To configure this connector you need to specify a server, user, and a password.

```xml
<connection>
	<name>UserAppRoles</name>
	<description>Connector to UserApp Roles Service</description>
	<type>com.trivir.idmunit.connector.UserAppRoles</type>
	<server>http://127.0.0.1:8080/IDMProv</server>
	<user>cn=admin,o=services</user>
	<password>B2vPD2UsfKc=</password>
	<multiplier/>
	<substitutions/>
	<data-injections/>
</connection>
```

