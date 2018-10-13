Soap Client Connector
---------------------
The SOAP Client connector provides methods to submit SOAP requests and validate their responses using XPATH or RegEx.

OPERATIONS
----------
ValidateXpath: Validate the response from a SOAP response using XPATH.
* url - The URL of the SOAP endpoint to submit the request.
* request - A SOAP request document.
* response - An XPATH expression to validate the SOAP response.

ValidateRegex: Validate the response from a SOAP request using Regular Expressions.
* url - The URL of the SOAP endpoint to submit the request.
* request - A SOAP request document.
* response - A Regular Expression to validate the SOAP response.

CONFIGURATION
-------------
To configure this connector you need to specify a server, user, and a password. 

    <connection>
        <name>SOAP</name>
        <description>Soap Client Connector</description>
        <type>com.trivir.idmunit.connector.SoapClientConnector</type>
        <user>user</user>
        <password>B2vPD2UsfKc=</password>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>

Optionally, httpRequestHeaders can be specified to contain a comma delmited list of names of spreadsheet columns that contain header values.  The corresponding cell value would hold the actual value for the header. This particular example allows us to do things like call multiple SOAP services in a single spreadsheet, using the same connector but with different SOAPActions.

		<httpRequestHeaders>SOAPAction</httpRequestHeaders>
