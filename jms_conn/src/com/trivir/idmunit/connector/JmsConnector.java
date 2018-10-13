/*
 * IdMUnit - Automated Testing Framework for Identity Management Solutions
 * Copyright (c) 2005-2018 TriVir, LLC
 *
 * This program is licensed under the terms of the GNU General Public License
 * Version 2 (the "License") as published by the Free Software Foundation, and
 * the TriVir Licensing Policies (the "License Policies").  A copy of the License
 * and the Policies were distributed with this program.
 *
 * The License is available at:
 * http://www.gnu.org/copyleft/gpl.html
 *
 * The Policies are available at:
 * http://www.idmunit.org/licensing/index.html
 *
 * Unless required by applicable law or agreed to in writing, this program is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied.  See the License and the Policies
 * for specific language governing the use of this program.
 *
 * www.TriVir.com
 * TriVir LLC
 * 13890 Braddock Road
 * Suite 310
 * Centreville, Virginia 20121
 *
 */

package com.trivir.idmunit.connector;

import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

public class JmsConnector extends AbstractConnector implements ExceptionListener {
    private static final String CONFIG_DURABLE = "durable";
    private static final String CONFIG_PASSWORD = "password";
    private static final String CONFIG_SUBJECT = "subject";
    private static final String CONFIG_TOPIC = "topic";
    private static final String CONFIG_URL = "url";
    private static final String CONFIG_USER = "user";
    private static final String CONFIG_PERSISTENT = "persistent";
    private static final String CONFIG_CONNECTION_FACTORY = "connection-factory";
    private static final String CONFIG_CONNECTION_FACTORY_JNDI_NAME = "connection-factory-jndi-name";

    private static final int RECV_TIMEOUT = 1000;

    private boolean durable = false;
    private boolean topic = false;
    private Connection connection;

    private String clientId = "testClientId";
    private Session session;
    private Destination destination;
    private MessageProducer replyProducer;
    private boolean transacted;
    private int ackMode = Session.AUTO_ACKNOWLEDGE;
    private String consumerName = "IdMUnit";
    private boolean persistent = false;

    public void setup(Map<String, String> config) throws IdMUnitException {
        String url = config.get(CONFIG_URL);
        String user = config.get(CONFIG_USER);
        String password = config.get(CONFIG_PASSWORD);
        if (user != null || password != null) {
            if (user == null) {
                throw new IdMUnitException("User and password must be specified in pair.  User was missing.");
            }
            if (password == null) {
                throw new IdMUnitException("User and password must be specified in pair.  Password was missing.");
            }
        }
        String jndiConnectionFactory = config.get(CONFIG_CONNECTION_FACTORY);
        String connectionFactoryName = config.get(CONFIG_CONNECTION_FACTORY_JNDI_NAME);

        String subject = config.get(CONFIG_SUBJECT);
        if (config.get(CONFIG_TOPIC) != null) {
            topic = Boolean.parseBoolean(config.get(CONFIG_TOPIC));
        }
        if (config.get(CONFIG_DURABLE) != null) {
            durable = Boolean.parseBoolean(config.get(CONFIG_DURABLE));
        }
        if (config.get(CONFIG_PERSISTENT) != null) {
            persistent = Boolean.parseBoolean(config.get(CONFIG_PERSISTENT));
        }

        if ("CLIENT_ACKNOWLEDGE".equals(ackMode)) {
            this.ackMode = Session.CLIENT_ACKNOWLEDGE;
        }
        if ("AUTO_ACKNOWLEDGE".equals(ackMode)) {
            this.ackMode = Session.AUTO_ACKNOWLEDGE;
        }
        if ("DUPS_OK_ACKNOWLEDGE".equals(ackMode)) {
            this.ackMode = Session.DUPS_OK_ACKNOWLEDGE;
        }
        if ("SESSION_TRANSACTED".equals(ackMode)) {
            this.ackMode = Session.SESSION_TRANSACTED;
        }

        Context jndiContext = null;
        ConnectionFactory connectionFactory = null;

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, jndiConnectionFactory);
        props.put(Context.PROVIDER_URL, url);
        if (user != null) {
            props.put(Context.SECURITY_PRINCIPAL, user);
            props.put(Context.SECURITY_CREDENTIALS, password);
        }


        try {
            jndiContext = new InitialContext(props);
        } catch (NamingException e) {
            throw new IdMUnitException("Error creating JNDI context", e);
        }

        /*
         * Look up connection factory and destination.
         */
        try {
            connectionFactory = (ConnectionFactory)jndiContext.lookup(connectionFactoryName);
//            destination = (Destination)jndiContext.lookup(subject);
        } catch (NamingException e) {
            throw new IdMUnitException("JNDI lookup failed", e);
        }

        /*
         * Create connection. Create session from connection; false means
         * session is not transacted. Create sender and text message. Send
         * messages, varying text slightly. Send end-of-messages message.
         * Finally, close connection.
         */
        try {
            if (user == null) {
                connection = connectionFactory.createConnection();
            } else {
                connection = connectionFactory.createConnection(user, password);
            }
            if (durable && clientId != null && clientId.length() > 0 && !"null".equals(clientId)) {
                connection.setClientID(clientId);
            }
            connection.setExceptionListener(this);
            connection.start();
            session = connection.createSession(transacted, ackMode);
            if (topic) {
                destination = session.createTopic(subject);
            } else {
                destination = session.createQueue(subject);
            }
        } catch (JMSException e) {
            throw new IdMUnitException(e);
        }

//        try {
//            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
//            connection = connectionFactory.createConnection();
//            if (durable && clientId != null && clientId.length() > 0 && !"null".equals(clientId)) {
//                connection.setClientID(clientId);
//            }
//            connection.setExceptionListener(this);
//            connection.start();
//
//            session = connection.createSession(transacted, ackMode);
//            if (topic) {
//                destination = session.createTopic(subject);
//            } else {
//                destination = session.createQueue(subject);
//            }
//        } catch (JMSException e) {
//            throw new IdMUnitException(e);
//        }
    }

    public void tearDown() throws IdMUnitException {
        try {
            session.close();
            connection.close();
        } catch (JMSException e) {
            throw new IdMUnitException(e);
        }
    }

    public void opValidate(Map<String, Collection<String>> dataRow) throws IdMUnitException {
        String expectedMessageText = ConnectorUtil.getSingleValue(dataRow, "message");

        try {
            replyProducer = session.createProducer(null);
            replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            MessageConsumer consumer = null;
            if (durable && topic) {
                consumer = session.createDurableSubscriber((Topic)destination, consumerName);
            } else {
                consumer = session.createConsumer(destination);
            }

            Message message = consumer.receive(RECV_TIMEOUT);
            String messageText;
            if (message == null) {
                throw new IdMUnitFailureException("Could not recieve any data!");
            }
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage)message;
                messageText = txtMsg.getText();
            } else {
                messageText = message.toString();
            }

            if (message.getJMSReplyTo() != null) {
                replyProducer.send(message.getJMSReplyTo(),
                        session.createTextMessage("Reply: " + message.getJMSMessageID()));
            }

            if (transacted) {
                session.commit();
            } else if (ackMode == Session.CLIENT_ACKNOWLEDGE) {
                message.acknowledge();
            }

            consumer.close();

            if (messageText.matches(expectedMessageText) == false) {
                throw new IdMUnitFailureException("output expected:<" + expectedMessageText + "> but was:<" + messageText + ">");
            }
        } catch (JMSException e) {
            throw new IdMUnitException(e);
        }
    }

    public synchronized void onException(JMSException ex) {
        System.out.println("JMS Exception occured.  Shutting down client.");
    }

    public void opPublish(Map<String, Collection<String>> dataRow) throws IdMUnitException {
        String messageText = ConnectorUtil.getSingleValue(dataRow, "message");

        try {
//            if (timeToLive != 0) {
//                System.out.println("Messages time to live " + timeToLive + " ms");
//            }

//            producer = session.createProducer(destination);
//            TextMessage message = session.createTextMessage();
//            for (int i = 0; i < numMsgs; i++) {
//                message.setText("This is message " + (i + 1));
//                LOG.info("Sending message: " + message.getText());
//                producer.send(message);
//            }
//
//            /*
//             * Send a non-text control message indicating end of messages.
//             */
//            producer.send(session.createMessage());

            // Create the producer.
            MessageProducer producer = session.createProducer(destination);
            if (persistent) {
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            } else {
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            }
//            if (timeToLive != 0) {
//                producer.setTimeToLive(timeToLive);
//            }

            // Start sending messages
            TextMessage message = session.createTextMessage(messageText);

            producer.send(message);
            if (transacted) {
                session.commit();
            }

            // Use the ActiveMQConnection interface to dump the connection
            // stats.
//            ActiveMQConnection c = (ActiveMQConnection)connection;
//            c.getConnectionStats().dump(new IndentPrinter());

        } catch (JMSException e) {
            throw new IdMUnitException(e);
        }
    }
}
