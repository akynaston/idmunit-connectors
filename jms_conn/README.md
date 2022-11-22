# JMS Connector

The JMS connector provides methods to allow you to publish messages to a JMS message bus and receive and validate messages from a JMS message bus using IdMUnit tests.

## Operations

---

### Publish

Publishes a message to a queue or topic.

#### Params

- **message** - Message text to publish

---

### Validate

Validates the first message received on the queue or topic.

#### Params

- **message** - Expected message text to compare with the message text received

---

## Configuration

To configure this connector you need to specify the following settings:

```xml
<connection>
    <name>JMS</name>
    <description>Connector to a JMS message bus</description>
    <type>com.trivir.idmunit.connector.JmsConnector</type>
    <url>192.168.1.3</url>
    <topic>false</topic>
    <subject>TEST.FOO</subject>
    <connection-factory>org.apache.activemq.jndi.ActiveMQInitialContextFactory</connection-factory>
    <connection-factory-jndi-name>ConnectionFactory</connection-factory-jndi-name>
    <multiplier/>
    <substitutions/>
    <data-injections/>
</connection>
```

User and password are optional and are used in the JNDI lookup, and connection factory authentication if provided:

```xml
<user>jmsuser</user>
<password>B2vPD2UsfKc=</password>
```

