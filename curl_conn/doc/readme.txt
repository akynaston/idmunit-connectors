'cURL' Connector
=====================
The cURL Connector is a connector which mimics the cURL command. It can be used to issue HTTP calls and validate
the response.

OPERATIONS
==========
Action: Perform a single HTTP request
* url - The URL of the request
* method - The HTTP method of the request (supported methods: PUT, POST, DELETE)
* [headers] - Headers for the request (multivalue, format is headerKey=headerValue)
* [body] - Body of the request.

Validate: Performs a single HTTP
* url - The URL of the request
* method - The HTTP method of the request (supported methods: GET, PUT, POST, DELETE)
    - NOTE: PATCH is also supported, but will send a X-HTTP-Method-Override header instead of using the PATCH verb.
* statusCode - Expected status code of the response (e.g 404).
* responseBody - Expected response body as a JSON string.
    - NOTE: Either one of statusCode or responseBody is required for Validate to work.
* [headers] - Headers for the request (multivalue, format is headerKey=headerValue)
* [body] - Body of the request as a JSON string.

CONFIGURATION
=============
The connector can be configured to accept all certificates if using HTTPS:

<trust-all-certs>true</trust-all-certs>
