Command Line Connector
---------------
This connector runs console commands in Windows.

OPERATIONS
----------
RunCmd: Runs the given command and validates the results with the given regular expression.
* command - The command that you would like to run on the command line.
* response - A regular expression to match against the standard out of the command.

--------------------------------------------
|command              |            response|
--------------------------------------------
| dir c:\temp         |.*file\d*\.txt.*    |
--------------------------------------------

CONFIGURATION
-------------
To configure this connector you need to specify a server, user, and a password. 

		    <connection>
				<name>CmdLine</name>
				<disabled>false</disabled>
				<description>Connector to run console commands in Windows</description>
				<type>com.trivir.idmunit.connector.CmdLineConnector</type>
				<multiplier/>
				<substitutions/>
				<data-injections/>
			</connection>