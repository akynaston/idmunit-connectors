Powershell Connector
---------------------
The Powershell connector provides methods to allow you to execute Powershell scripts and commandlets from IdMUnit tests.

OPERATIONS
----------
Exec: Executes the specified command
* exec - Command to execute

Validate: Validates the output from executing the specifed command
* exec - Command to execute
* [field names] - values to compare with output from the command

CONFIGURATION
-------------
To configure this connector you only need to provide name, description, and type.

    <connection>
        <name>PS</name>
        <description>Connector for Powershell</description>
        <type>com.trivir.idmunit.connector.PowershellConnector</type>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>
