Iteration Connector
===================
The Iteration connector allows you to enhance existing connectors the following abilities:

* Repeat row operations over a specified range, and replace a token with the current iteration value.
* Populate a large multi-value attribute over a specified range and replace a token with the current iteration value.

These features are particularly useful when you need to:
* Create a large number of users for use in tests.
* Create a group with a large number of group members (like the users created in previous bullet item)

The row and attribute iterations can be used simultaneously to build multiple objects containing multivalued attributes.

OPERATIONS
==========
Operations will be the same as the connector you are wrapping. The following additional attributes will also be available for use in an operation:
* rowIterationStart - Iteration start value (inclusive).
* rowIterationEnd - Iteration end value (inclusive).
* attributeIterationStart - Attribute iteration start value (inclusive).
* attributeIterationEnd - Attribute iteration end value (inclusive).

EXAMPLES
========

Row Iteration
-------------
The row iteration token is "$rowIteration$". This token will be replaced with the current row iteration

------------------------------------------------------------
|someValue              |rowIterationStart|rowIterationEnd |
------------------------------------------------------------
| myValue$rowIteration$ |3                |5               |
------------------------------------------------------------

The above example will result in the operation being repeated with the values 3-5. The resulting value for someValue in the 3 iterations will be:
* myValue3
* myValue4
* myValue5

Attribute Iteration
-------------------
The attribute iteration token is "$attributeIteration$". This token will be replaced with the current attribute iteration.

--------------------------------------------------------------------------------
|someValue                    | attributeIterationStart| attributeIterationEnd |
--------------------------------------------------------------------------------
| myValue$attributeIteration$ | 4                      | 7                     |
--------------------------------------------------------------------------------

The above example will result in the attribute being repeated with the values 4-7. The resulting multiple values for someValue will be:
* myValue4, myValue5, myValue6, myValue7

CONFIGURATION
=============
To configure this connector you need to specify the same configuration you would for the connector you are enhancing with the following exceptions:
* The <type> configuration must point to the Iteration connector (com.trivir.idmunit.connector.IterationConnector)
* An additional configuration of <wrappedConnector> will point to the connector type you would normally place in the <type> configuration. e.g. for the LDAP connector, this would be com.trivir.idmunit.connector.LdapConnector.

For example, an enhanced LDAP connector would have the following configuration:

    <connection>
        <name>IDV</name>
        <description>Connector to an LDAP server</description>
        <type>com.trivir.idmunit.connector.IterationConnector</type>
        <wrappedConnector>org.idmunit.connector.LdapConnector</wrappedConnector>
        <server>192.168.1.3</server>
        <user>cn=admin,o=services</user>
        <password>B2vPD2UsfKc=</password>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>

