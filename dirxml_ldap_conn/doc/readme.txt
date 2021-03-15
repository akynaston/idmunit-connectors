DXCMD Connector
---------------
The DXCMD connector allows you execute DXCMD commands from IdMUnit tests.

OPERATIONS
----------
MigrateApp: migrate objects from the application into eDirectory
* dn - driver object DN
* xmlFileData - XDS document with an XML query to select objects to be migrated (see http://developer.novell.com/documentation/dirxml/dirxmlbk/ref/ndsdtd/query.html for query documentation)
* xmlFile (deprecated) - name of file containing XML query to select objects to be migrated

MigrateApp query examples
--------------------------------------------------------
<nds dtdversion="3.5" ndsversion="8.x">
	<source>
		<product version="3.5.1.20070411 ">DirXML</product>
		<contact>Novell, Inc.</contact>
	</source>
	<input>
		<query event-id="testmigration" class-name="User" scope="entry" dest-dn="CN=Jones\, Sue,OU=Staff,DC=example,DC=com">
			<read-attr/>
		</query>
	</input>
</nds>

<nds dtdversion="3.5" ndsversion="8.x">
	<source>
		<product version="3.5.1.20070411 ">DirXML</product>
		<contact>Novell, Inc.</contact>
	</source>
	<input>
		<query event-id="testmigration" class-name="User" scope="subtree">
			<search-class class-name="User"/>
			<search-attr attr-name="Surname">
				<value type="string">Jones</value>
			</search-attr>
			<read-attr/>
			<read-parent/>
		</query>
	</input>
</nds>
--------------------------------------------------------

SetDriverStatusAuto: set driver to auto start and noresync
* dn - driver object DN

SetDriverStatusDisabled: set driver to disabled
* dn - driver object DN

SetDriverStatusManual: set driver to manual start and noresync
* dn - driver object DN

StartDriver:
* dn - driver object DN

StartJob:
* dn - job DN

StopDriver:
* dn - driver object DN

CONFIGURATION
-------------
To configure the DxcmdConnector you need to specify a server, user, and password and optionally a port.

    <connection>
        <name>DXCMD</name>
        <description>Connector to an IDM server</description>
        <type>com.trivir.idmunit.connector.DxcmdConnector</type>
        <server>192.168.1.3</server>
        <!--
        <port>524</port>
        -->
        <user>admin.services</user>
        <password>B2vPD2UsfKc=</password>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>


JAR FILES
-------------
For the DXCMD connector to operate properly, the following jar files must be in your class path.  Copy them from the IDM Engine server to your Designer workstation, then add them to your class path. Pay special attention to jclient.jar, as you must use the jar provided by the Deisgner installation:

dirxml.jar
nxsl.jar
dirxml_misc.jar
ldap.jar
js.jar
xp.jar
jclient.jar (note: use the jar file in the designer installation, not from your current IDM installation, or you'll get mismatched jclient errors)
	Note: Designer 3.0, this file is located at:
		C:\Program Files\Novell\Designer\plugins\com.novell.core.jars_4.0.0.201104051747\lib
	In Designer 4.0.1, this jar file is at:
		C:\Program Files\Novell\Designer\eclipse\plugins\com.novell.core.jars_3.0.0.2008
07211539\lib\jclient.jar

ERROR CODES AND THEIR SOLUTIONS
-------------------------------
1: This is a generic error, and means there is a stack trace in the output.  Review the stack trace to resolve this issue.
32: This typically means that the DN is invalid: did you specify an LDAP DN instead of a standard dotted dn?   Example: admin.services vs cn=admin,o=services