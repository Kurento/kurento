package com.kurento.kmf.rabbitmq;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

import com.kurento.kmf.common.Address;

public class RabbitMqManager {

	public static final String EVENT_QUEUE_PREFIX = "event_";
	public static final String CLIENT_QUEUE_PREFIX = "client_";
	public static final String CLIENT_REPLY_QUEUE_PREFIX = "client_reply_";
	public static final String MEDIA_PIPELINE_QUEUE_PREFIX = "media_pipeline_";
	public static final String PIPELINE_CREATION_QUEUE = "pipeline_creation";

	private static final Logger log = LoggerFactory
			.getLogger(RabbitMqManager.class);

	private static final long TIMEOUT = 10000;
	private static final String EXPIRATION_TIME = "10000";

	private CachingConnectionFactory cf;
	private RabbitAdmin admin;
	private final List<SimpleMessageListenerContainer> containers = new ArrayList<>();

	private final Address address;

	public interface BrokerMessageReceiverWithResponse {
		public String onMessage(String message);
	}

	public interface BrokerMessageReceiver {
		public void onMessage(String message);
	}

	public RabbitMqManager(Address address) {
		this.address = address;
	}

	public void connect() {

		cf = new CachingConnectionFactory(address.getHost(), address.getPort());
		admin = new RabbitAdmin(cf);

		declarePipelineCreationQueue(admin);
	}

	private void declarePipelineCreationQueue(RabbitAdmin admin) {

		Queue queue = new Queue(PIPELINE_CREATION_QUEUE, true, false, false);
		admin.declareQueue(queue);

		DirectExchange exchange = new DirectExchange(PIPELINE_CREATION_QUEUE,
				true, false);

		admin.declareExchange(exchange);

		admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(""));

		log.debug("Queue '" + PIPELINE_CREATION_QUEUE
				+ "' declared. Exchange '" + PIPELINE_CREATION_QUEUE
				+ "' declared.");
	}

	public Queue declarePipelineQueue() {
		return admin.declareQueue();
	}

	public Queue declareClientQueue() {
		return admin.declareQueue();
	}

	public RabbitTemplate createClientTemplate() {

		Queue queue = admin.declareQueue();

		RabbitTemplate template = new RabbitTemplate(cf);

		template.setReplyTimeout(TIMEOUT);
		template.setReplyQueue(queue);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(
				cf);
		container.setMessageListener(template);
		container.setQueueNames(queue.getName());
		container.start();

		containers.add(container);

		log.debug("Created RabbitMqTemplate receiving messages in queue: {}",
				queue.getName());

		return template;
	}

	public String sendAndReceive(String exchange, String routingKey,
			String message) {
		return sendAndReceive(exchange, routingKey, message, null);
	}

	public String sendAndReceive(String exchange, String routingKey,
			String message, RabbitTemplate template) {

		if (template == null) {
			template = new RabbitTemplate(cf);
			template.setReplyTimeout(TIMEOUT);
		}

		log.debug("Req-> Exchange:'" + exchange + "' RoutingKey:'" + routingKey
				+ "' " + message);

		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setExpiration(EXPIRATION_TIME);

		Message response = template.sendAndReceive(exchange, routingKey,
				new Message(message.getBytes(), messageProperties));

		if (response == null) {
			throw new RabbitMqException("Timeout waiting a reply to message: "
					+ message);
		}

		String responseAsString = new String(response.getBody());
		log.debug("<-Res " + responseAsString.trim());
		return responseAsString;
	}

	public RabbitTemplate createServerTemplate() {

		return new RabbitTemplate(cf);
	}

	public void send(String exchange, String routingKey, String message) {
		send(exchange, routingKey, message, null);
	}

	public void send(String exchange, String routingKey, String message,
			RabbitTemplate template) {

		if (template == null) {
			template = new RabbitTemplate(cf);
		}

		log.debug("Not-> Exchange:'" + exchange + "' RoutingKey:'" + routingKey
				+ "' " + message);

		template.send(exchange, routingKey, new Message(message.getBytes(),
				new MessageProperties()));
	}

	public String declareEventsExchange(String pipeline) {

		String exchangeName = EVENT_QUEUE_PREFIX + pipeline;

		DirectExchange exchange = new DirectExchange(exchangeName, false, true);
		admin.declareExchange(exchange);

		log.debug("Events exchange '" + exchangeName + "' declared.");

		return exchangeName;
	}

	public void addMessageReceiver(final String queue,
			final BrokerMessageReceiver receiver) {

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(
				cf);
		MessageListenerAdapter adapter = new MessageListenerAdapter(
				new Object() {
					@SuppressWarnings("unused")
					protected void onMessage(byte[] message) {
						onMessage(new String(message));
					}

					protected void onMessage(String messageJson) {
						log.debug("<-Not Queue:'" + queue + "' "
								+ messageJson.trim());
						receiver.onMessage(messageJson);
					}
				}, "onMessage");

		container.setMessageListener(adapter);
		container.setQueueNames(queue);
		container.start();

		containers.add(container);

		log.debug("Registered receiver '" + receiver.getClass().getName()
				+ "' for queue '" + queue);
	}

	public void addMessageReceiverWithResponse(final String queue,
			final BrokerMessageReceiverWithResponse receiver) {

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(
				cf);
		MessageListenerAdapter adapter = new MessageListenerAdapter(
				new Object() {
					@SuppressWarnings("unused")
					protected String onMessage(byte[] message) {

						String messageJson = new String(message);

						log.debug("<-Req Queue:'" + queue + "' " + messageJson);

						String responseJson = receiver.onMessage(messageJson);
						log.debug("Res-> " + responseJson);
						return responseJson;
					}
				}, "onMessage");

		container.setMessageListener(adapter);
		container.setQueueNames(queue);
		container.start();

		containers.add(container);

		log.debug("Registered receiver with response '"
				+ receiver.getClass().getName() + "' for queue '" + queue);
	}

	public void bindExchangeToQueue(String exchangeId, String queueId,
			String eventRoutingKey) {

		Queue queue = new Queue(queueId, false, true, true);
		DirectExchange exchange = new DirectExchange(exchangeId);

		admin.declareBinding(BindingBuilder.bind(queue).to(exchange)
				.with(eventRoutingKey));

		log.debug("Exchange '" + exchangeId + "' bind to queue '" + queueId
				+ "' with routingKey '" + eventRoutingKey + "'");

	}

	public String createRoutingKey(String mediaElementId, String eventType) {
		return mediaElementId + "/" + eventType;
	}

	public void destroy() {

		for (SimpleMessageListenerContainer container : containers) {
			// System.out.println("Queues:"
			// + Arrays.toString(container.getQueueNames()));
			container.destroy();
		}

		containers.clear();
		cf.destroy();
	}
}
