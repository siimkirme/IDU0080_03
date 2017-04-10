package ee.ttu.idu0080.raamatupood.client;

import java.math.BigDecimal;

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

import ee.ttu.idu0080.raamatupood.server.EmbeddedBroker;
import ee.ttu.idu0080.raamatupood.types.Car;
import ee.ttu.idu0080.raamatupood.types.Tellimus;
import ee.ttu.idu0080.raamatupood.types.Tellimuserida;

/**
 * JMS sõnumite tarbija. Ühendub broker-i urlile
 * 
 * @author Allar Tammik
 * @date 08.03.2010
 */
public class Vabrik {
	private static final Logger log = Logger.getLogger(Vabrik.class);
	private String SUBJECT = "UUSJRK";
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;
	private String url = EmbeddedBroker.URL;
	private long timeToLive = 1000000;
	
	private Session session;
	private MessageProducer producer;

	public static void main(String[] args) {
		Vabrik consumerTool = new Vabrik();
		consumerTool.run();
	}

	public void run() {
		Connection connection = null;
		try {
			log.info("Connecting to URL: " + url);
			log.info("Consuming queue : " + SUBJECT);

			// 1. Loome ühenduse
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, password, url);
			connection = connectionFactory.createConnection();

			// Kui ühendus kaob, lõpetatakse Consumeri töö veateatega.
			connection.setExceptionListener(new ExceptionListenerImpl());

			// Käivitame ühenduse
			connection.start();

			// 2. Loome sessiooni
			/*
			 * createSession võtab 2 argumenti: 1. kas saame kasutada
			 * transaktsioone 2. automaatne kinnitamine
			 */
			session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);

			// Loome teadete sihtkoha (järjekorra). Parameetriks järjekorra nimi
			Destination destination = session.createQueue(SUBJECT);

			// 3. Teadete vastuvõtja
			MessageConsumer consumer = session.createConsumer(destination);

			// Kui teade vastu võetakse käivitatakse onMessage()
			consumer.setMessageListener(new MessageListenerImpl());
			
			producer = session.createProducer(destination);

			// producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(timeToLive);

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Käivitatakse, kui tuleb sõnum
	 */
	class MessageListenerImpl implements javax.jms.MessageListener {

		public void onMessage(Message message) {
			try {
				if (message instanceof TextMessage) {
					TextMessage txtMsg = (TextMessage) message;
					String msg = txtMsg.getText();
					log.info("Received: " + msg);
				} else if (message instanceof ObjectMessage) {
					ObjectMessage objectMessage = (ObjectMessage) message;
					if(objectMessage.getObject() instanceof Tellimus) 
					{
						Tellimus tellimus = (Tellimus) objectMessage.getObject();
						log.info("Saadeti tellimus:");
						for (Tellimuserida rida : tellimus.getTellimuseRead()) {
							log.info("Toode: " + rida.getToode().getNimetus());
							log.info("Kogus: " + rida.getKogus());
							log.info("Hind: " + rida.getToode().getHind());
							log.info("------------------------------------");
						}
						sendAnswer(CalculateOrder(tellimus));
					} else if(objectMessage.getObject() instanceof Car)
					{
					    Car auto=(Car)objectMessage.getObject();
						log.info("Auto!!! Autol on "+auto.getDoors()+" uks/ust");
					}
					
						
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
	
	private void sendAnswer(String vastus) throws JMSException {
		TextMessage message = session
				.createTextMessage(vastus);
		producer.send(message);
	}

	public String CalculateOrder(Tellimus tellimus) {
		BigDecimal sum = BigDecimal.ZERO;
		
		for (Tellimuserida rida : tellimus.getTellimuseRead()) {
			sum = sum.add(rida.getToode().getHind().multiply(new BigDecimal(rida.getKogus())));
		}
		
		return String.format("Tooteid tellitud: %1d, Tellimuse summa kokku: %2s", 
				tellimus.getTellimuseRead().size(), sum.toString());
	}

	/**
	 * Käivitatakse, kui tuleb viga.
	 */
	class ExceptionListenerImpl implements javax.jms.ExceptionListener {

		public synchronized void onException(JMSException ex) {
			log.error("JMS Exception occured. Shutting down client.");
			ex.printStackTrace();
		}
	}

}