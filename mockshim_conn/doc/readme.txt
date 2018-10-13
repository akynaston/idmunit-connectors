MockShim Connector
---------------
The MockShim connector provides methods that allow you to add, modify, delete, and validate records in MockShim databases from IdMUnit tests.

OPERATIONS
----------
AddObject: Used to add an object to the MockShim database
* association - Required attribute used for record identification.
* attribute list - Attributes must be defined in the mock_shim.properties file. 


DelObject: Used to remove an object from the MockShim database 
* association - required attribute used for record identification.


ModifyObject: Used to modify an object in the the MockShim database
* association - Required attribute used for record identification. 
* attribute list - Attributes must be defined in the mock_shim.properties file. 


ValidateObject: Validates the output from executing the specifed query
* association - Required attribute used for record identification.
* attribute list - Attributes must be defined in the mock_shim.properties file. 



CONFIGURATION
-------------
To configure this connector you need to specify a server, user, password, and jdbc-driver-class.

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
