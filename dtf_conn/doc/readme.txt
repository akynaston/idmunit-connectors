DTF Connector
-------------
The DTF connector allows you validate delimited text files from IdMUnit tests. All fields passed to this connector are converted to single values. Multivalue fields are not supported.

Validation operations only happen against new rows written to the files since the beginning of the sheet in your spreadsheet. Any existing data in the files will be ignored. Also, the connector automatically includes files with the extension ".tmp", in addition to those with an extension matching output-file-ext, since those are the temporary files used by the DTF driver.

INSTALLATION
------------
The DTF connector requires jsch-0.1.50.jar (or newer) and opencsv.2.3.jar (or newer) be added to your classpath.

OPERATIONS
----------
Add
  * [field names] - values to write to the row in a file. Each Add operation will create a new file. The name of the file will be based on the current day and time.

Validate
  * [field names] - values to compare with fields from the files

CONFIGURATION
-------------
The connector can be configured to work with local files or remote files via SSH. These two options require different configuration. To configure this connector to work with local files you need to specify a read-path, write-path, output-file-ext, delimiter, field-definitions, and row-key.

"field-definitions" specifies the names of the columns in the IdMUnit tests that correspond to each field in the file. It can also specify the width of each field in the file by including the length in parentheses following the name, e.g. FirstName(30).
"row-key" specifies one of the column/field names from the "field-definitions" parameter that will be used as the key value to select rows from the data when performing validation operations.

    <connection>
        <name>DTF</name>
        <description>Connector to output of DTF driver</description>
        <type>com.trivir.idmunit.connector.DTF2Connector</type>
        <read-path>/var/opt/novell/IdM/input-files</read-path>
        <write-path>/var/opt/novell/IdM/output-files</write-path>
        <output-file-ext>.csv</output-file-ext>
        <delimiter>,</delimiter>
        <field-definitions>UserId, Name, FirstName, LastName, Group, Role</field-definitions>
        <row-key>UserId</row-key>
        <multiplier/> <!-- As needed-->
        <substitutions/>  <!-- As needed-->
        <data-injections/>  <!-- As needed-->
    </connection>

To configure this connector to work with remote files you need to specify a server, user, and password in addition to the parameters used for local files (i.e. read-path, write-path, output-file-ext, delimiter, field-definitions, and row-key).

Note: The password parameter in the example is encrypted as is typical for an IdMUnit connector configuration. Clear-text password can be used by specifying "DecryptPasswords=false" to your idmunit-defaults.properties file.

    <connection>
        <name>DTF</name>
        <description>Connector to output of DTF driver</description>
        <type>com.trivir.idmunit.connector.DTF2Connector</type>
        <read-path>/var/opt/novell/IdM/input-files</read-path>
        <write-path>/var/opt/novell/IdM/output-files</write-path>
        <output-file-ext>.csv</output-file-ext>
        <delimiter>,</delimiter>
        <field-definitions>UserId, Name, FirstName, LastName, Group, Role</field-definitions>
        <row-key>UserId</row-key>
        <server>172.17.2.140</server>
        <user>testuser</server>
        <password>B2vPD2UsfKc=<password>
        <multiplier/> <!-- As needed-->
        <substitutions/>  <!-- As needed-->
        <data-injections/>  <!-- As needed-->
    </connection>

When configuring the connector to work with remote files you can optionally specify port, host-key, and host-key-type.

    <connection>
        <name>DTF</name>
        <description>Connector to output of DTF driver</description>
        <type>com.trivir.idmunit.connector.DTF2Connector</type>
        <read-path>/var/opt/novell/IdM/input-files</read-path>
        <write-path>/var/opt/novell/IdM/output-files</write-path>
        <output-file-ext>.csv</output-file-ext>
        <delimiter>,</delimiter>
        <field-definitions>UserId, Name, FirstName, LastName, Group, Role</field-definitions>
        <row-key>UserId</row-key>
        <server>172.17.2.140</server>
        <port>22</port>
        <host-key>AAAAB3NzaC1yc2EAAAABIwAAAIEAs+KSqwFtIKZCsWryi2kkzwJyP+hSeGNdoqp0K611wRvS2Hd7SMbLu6yrqttdjWKonezVIaYx0clkdzTmu96HId+zDeCShljrIaslPIkN0nXSsSHuTa/k6K78iKKXsD5WSpKb0zcsnUbeRksUtdyAEiDlUC6CpYS1tC+34vl1AJ8=</host-key>
        <host-key-type>ssh-rsa</host-key-type>
        <user>testuser</server>
        <password>B2vPD2UsfKc=<password>
        <multiplier/> <!-- As needed-->
        <substitutions/>  <!-- As needed-->
        <data-injections/>  <!-- As needed-->
    </connection>
