SCP IdMUnit Connector
--------------------------
This version of the connector generates text content and pushes it to an SCP interface.

This release is currently dependent on WinSCP.  Its configuration requires a WinSCP connection profile to be setup.  The profile should be authenticated (saving the password) at least once to cache the required certificates/keys.

OPERATIONS:
-----------

AddObject:
* [column names] - column values to be added to the output file.
	Note: currently the ordering of the columns is how the file will be written; however, this is only because the ExcelParser uses a predictable iteration order collection: linked hash map; we need to resolve this to enforce ordering through the spreadsheet.

**Note:**: this connector does NOT provide validation of any sort currently.
	
	
CONFIGURATION
-------------
To configure this connector you need to specify the following values. for the scp-profile; run WinSCP and setup a profile saving the user name and password.
			<connection> <!-- SCP -->				
				<name>SCP</name>
				<description>Connector to generate DTF data feed and push to an SCP interface on a UNIX server</description>
				<type>com.trivir.idmunit.connector.SCPConnector</type>
				<!-- Delimited text generation section -->
				<write-path>/output</write-path>
				<delimiter>,</delimiter>
				<local-cache-path>./test/testData/dtf/</local-cache-path>
				<dtf-data-file-extension>csv</dtf-data-file-extension>
				<!-- SCP Interface Configuration Section -->
				<enable-scp>true</enable-scp>
				<win-scp-exe-path>C:/Program Files (x86)/WinSCP/WinSCP.exe</win-scp-exe-path>
				<scp-profile>winscpprofile-dev</scp-profile>
				<multiplier>
					<retry>0</retry>
					<wait>0</wait>
				</multiplier>
				<substitutions>
					<substitution>
					<replace/>
					<new/>
					</substitution>
				</substitutions>
			</connection>
			