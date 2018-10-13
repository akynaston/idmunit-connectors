VASCO Connector
---------------
The VASCO connector provides methods to allow you to send SOAP requests to a VASCO system from IdMUnit tests.

OPERATIONS
----------
CreateUser:
* - USER_ID - User ID of the user to be created.
* - DOMAIN - Domain in which to create the user.
* - LOCAL_AUTH - Supported values: Default, None, Digipass Only, Digipass/Pass, word.
* - BACKEND_AUTH - Supported values: Default, None, If needed, Always
* - DISABLED - true/false
* - LOCKED  - true/false
* - User attributes (Please see below for the format of user attributes on creates and validates).

DeleteUser:
* - USER_ID - User ID of the user to be created.
* - DOMAIN - Domain in which to create the user.

ValidateUser:
* - USER_ID - User ID of the user to be created.
* - DOMAIN - Domain in which to create the user.
* - Any other user attributes that need to be validated.
* - User attributes (Please see below for the format of user attributes on creates and validates).
* - ASSIGNED_DIGIPASS (Optional) - For example:  0123456789,11234567890

AssignDigipass
* - DIGIPASS_SERIAL
* - USER_ID - User ID of the user to be created.
* - DOMAIN - Domain in which to create the user.
* - GRACE_PERIOD_DAYS

UnAssignDigipass
* - DIGIPASS_SERIAL

ModifyUser
* - USER_ID - User ID of the user to be created.
* - DOMAIN - Domain in which to create the user.
* - This is only to modify account attributes. No user attributes (begin with "ua:") can be modified.
* - DIGIPASS_SERIAL cannot be used on a modify. Intstead use the AssignDigipass and UnAssignDigipass operations.

How to handle user attributes:
* - Column title will be prepended with "ua:".
* - Field values will be in the format "property:value" separated by commas.
* - Radius attributes need to have the OPTIONS property set to 1.
* - EXAMPLE:
Column Title: ua:Callback-id
Field Value: ATTR_GROUP:External_Group, USAGE_QUALIFIER:reply, VALUE:test@test.com,OPTIONS:1
		
		
EXAMPLE CONFIGURATION
---------------------
			<connection> <!-- VASCO -->
				<name>VASCO</name>
				<description>Connector to VASCO.</description>
				<type>com.trivir.idmunit.connector.VascoConnector</type>
				<server>https://10.10.10.01:8888</server>
				<user>external_user</user>
				<password>p@ssw0rd123</password>
				<multiplier/>
				<substitutions/>
				<data-injections/>
			</connection>