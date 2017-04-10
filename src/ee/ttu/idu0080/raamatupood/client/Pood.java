package ee.ttu.idu0080.raamatupood.client;

import java.math.BigDecimal;
import java.util.Date;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import ee.ttu.idu0080.raamatupood.client.Vabrik.MessageListenerImpl;
import ee.ttu.idu0080.raamatupood.server.EmbeddedBroker;
import ee.ttu.idu0080.raamatupood.types.*;

/**
 * JMS sõnumite tootja. Ühendub brokeri url-ile
 * 
 * @author Allar Tammik
 * @date 08.03.2010
 */
public class Pood {
	private static final Logger log = Logger.getLogger(Pood.class);
	public static final String SUBJECT = "UUSJRK"; // järjekorra nimi

	private String user = ActiveMQConnection.DEFAULT_USER;// brokeri jaoks vaja
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;

	long sleepTime = 1000; // 1000ms

	private int messageCount = 10;
	private long timeToLive = 1000000;
	private String url = EmbeddedBroker.URL;

	public static void main(String[] args) {
		Pood producerTool = new Pood();
		producerTool.run();
	}

	public void run() {
		Connection connection = null;
		try {
			log.info("Connecting to URL: " + url);
			log.debug("Sleeping between publish " + sleepTime + " ms");
			if (timeToLive != 0) {
				log.debug("Messages time to live " + timeToLive + " ms");
			}

			// 1. Loome ühenduse
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, password, url);
			connection = connectionFactory.createConnection();
			// Käivitame yhenduse
			connection.start();

			// 2. Loome sessiooni
			/*
			 * createSession võtab 2 argumenti: 1. kas saame kasutada
			 * transaktsioone 2. automaatne kinnitamine
			 */
			Session session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);

			// Loome teadete sihtkoha (järjekorra). Parameetriks järjekorra nimi
			Destination destination = session.createQueue(SUBJECT);

			// 3. Loome teadete saatja
			MessageProducer producer = session.createProducer(destination);

			// producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(timeToLive);
			
			// Teadete vastuvõtja
			MessageConsumer consumer = session.createConsumer(destination);

			// Kui teade vastu võetakse käivitatakse onMessage()
			consumer.setMessageListener(new GetAnswer());

			// 4. teadete saatmine 
			//sendLoop(session, producer);
			sendTellimus(session, producer);

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	class GetAnswer implements javax.jms.MessageListener {

		public void onMessage(Message message) {
			try {
				if (message instanceof TextMessage) {
					TextMessage txtMsg = (TextMessage) message;
					String msg = txtMsg.getText();
					log.info("Received: " + msg);
				} else if (message instanceof ObjectMessage) {
					ObjectMessage objectMessage = (ObjectMessage) message;
						
					String msg = objectMessage.getObject().toString();
					log.info("Received: " + msg);

				} else {
					log.info("Received: " + message);
				}

			} catch (JMSException e) {
				log.warn("Caught: " + e);
				e.printStackTrace();
			}
		}
	}
	
	private void sendTellimus(Session session, MessageProducer producer) throws JMSException {
		Toode t1 = new Toode(1, "Pasun", new BigDecimal(50.00));
		Toode t2 = new Toode(2, "Kohver", new BigDecimal(460.00));
		
		Tellimuserida rida1 = new Tellimuserida(t1, 2);
		Tellimuserida rida2 = new Tellimuserida(t2, 1);
		
		Tellimus tellimus1 = new Tellimus();
		tellimus1.addTellimuseRida(rida1);
		tellimus1.addTellimuseRida(rida2);
		
		ObjectMessage objectMessage = session.createObjectMessage();
		objectMessage.setObject(tellimus1);
		producer.send(objectMessage);
	}

	
	protected void sendLoop(Session session, MessageProducer producer)
			throws Exception {

		for (int i = 0; i < messageCount || messageCount == 0; i++) {
			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(new Car(i)); // peab olema Serializable
			producer.send(objectMessage);

			TextMessage message = session
					.createTextMessage(createMessageText(i));
			log.debug("Sending message: " + message.getText());
			producer.send(message);
			
			// ootab 1 sekundi
			Thread.sleep(sleepTime);
		}
	}
	
	class ExceptionListenerImpl implements javax.jms.ExceptionListener {

		public synchronized void onException(JMSException ex) {
			log.error("JMS Exception occured. Shutting down client.");
			ex.printStackTrace();
		}
	}

	private String createMessageText(int index) {
		return "Message: " + index + " sent at: " + (new Date()).toString();
	}
}


