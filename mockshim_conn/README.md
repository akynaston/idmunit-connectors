# MockShim Connector

The MockShim connector provides methods that allow you to add, modify, delete and validate records in MockShim databases from IdMUnit tests.

## Operations

---

### AddObject

Used to add an object to the MockShim database.

#### Params

- **association** - Required attribute used for record identification.
- **attribute list** - Attributes must be defined in the mock_shim.properties file.

---

### DelObject

Used to remove an object from the MockShim database.

#### Params

- **association** - Required attribute used for record identification.

---

### ModifyObject

Used to remove an object from the MockShim database.

#### Params

- **association** - Required attribute used for record identification.
- **attribute list** - Attributes must be defined in the mock_shim.properties file.

---

### ValidateObject

Validates the output from executing the specified query.

#### Params

- **association** - Required attribute used for record identification.
- **attribute list** - Attributes must be defined in the mock_shim.properties file.

---

## Configuration

To configure this connector you need to specify a server, user, password and jdbc-driver-class.

```xml
<connection>
    <name>MockShim</name>
    <description>Connector to a database using a JDBC driver</description>
    <type>com.trivir.idmunit.connector.MockShim</type>
    <server>jdbc:hsqldb:hsql://localhost/mockCache</server>
    <user>sa</user>
    <password></password>
    <jdbc-driver-class>org.hsqldb.jdbcDriver</jdbc-driver-class>
    <multiplier/>
    <substitutions/>
    <data-injections/>
</connection>
```

