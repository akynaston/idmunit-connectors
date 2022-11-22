# cURL Connector

The cURL Connector is a connector which mimics the cURL command. It can be used to issue HTTP calls and validate the response.

## Operations

---

### Action

Perform a single HTTP request.

#### Params

- **url** - The URL of the request.
- **method** - The HTTP method of the request (supported methods: PUT, POST, DELETE).
- **[headers]** - Headers for the request (multi-value format is `headerKey=headerValue`).
- **[body]** - Body of the request.

---

### Validate

Performs a single HTTP

#### Params

- **url** - The URL of the request.
- **method** - The HTTP method of the request (supported methods: PUT, POST, DELETE).
- **statusCode** - Expected status code of the response (e.g. 404).
- **responseBody** - Expected response body as a JSON string.
- **[headers]** - Headers for the request (multi-value format is headerKey=headerValue).
- **[body]** - Body of the request.

> **Note**\
> PATCH is also supported, but will send a X-HTTP-Method-Override header instead of using the PATCH verb.

> **Note**\
> Either one of statusCode or responseBody is required for Validate to work.

---

## Configuration

The connector can be configured to accept all certificates if using HTTPS:

```xml
<trust-all-certs>true</trust-all-certs>
```

