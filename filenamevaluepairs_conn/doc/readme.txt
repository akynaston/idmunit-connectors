Name Value Pairs (Properties) File Connector
---------------
The Name Value Pairs File connector extends the DTFConnector adding a method to validate values of name-value pairs in a file from IdMUnit tests.

OPERATIONS
----------
ValidateNameValuePairsFile: Validates values from the most recently modified file.
* FileContents - The format of FileContents should be as follows:
-----------------------------------------
| FileContents                          |
-----------------------------------------
| Name1=Value1                          |
| Name2=Value2                          |
| Name3=Value3                          |
| Name3=Value3.1                        |
-----------------------------------------

CONFIGURATION
-------------
To configure this connector you need to specify a read-path, write-path, delimiter, and field-names.

    <connection>
        <name>NVPF</name>
        <description>Connector to output of DTF driver in properties file format</description>
        <type>com.trivir.idmunit.connector.FileNameValuePairsConnector</type>
        <read-path>/var/opt/novell/IdM/input-files</read-path>
        <write-path>/var/opt/novell/IdM/output-files</write-path>
        <delimiter>,</delimiter>
        <field-names>USER ID, Name, FirstName, LastName, Group, Role</field-names>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>
