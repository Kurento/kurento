package kmf.broker;

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

public class Broker {

	public static final String EVENT_QUEUE_PREFIX = "event_";
	public static final String CLIENT_QUEUE_PREFIX = "client_";
	public static final String CLIENT_REPLY_QUEUE_PREFIX = "client_reply_";
	public static final String MEDIA_PIPELINE_QUEUE_PREFIX = "media_pipeline_";
	public static final String PIPELINE_CREATION_QUEUE = "pipeline_creation";

	private Logger LOG = LoggerFactory.getLogger(Broker.class);

	private static final long TIMEOUT = 1000000;

	private CachingConnectionFactory cf;
	private RabbitAdmin admin;

	private String logId;
	private RabbitTemplate template;

	public interface BrokerMessageReceiverWithResponse {
		public String onMessage(String message);
	}

	public interface BrokerMessageReceiver {
		public void onMessage(String message);
	}

	public Broker() {
		this("");
	}

	public Broker(String logId) {
		this.logId = logId;
	}

	public void init() {
		cf = new CachingConnectionFactory();
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

		LOG.debug("[" + logId + "] Queue '" + PIPELINE_CREATION_QUEUE
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

		return template;
	}

	public String declareEventsExchange(String pipeline) {

		String exchangeName = EVENT_QUEUE_PREFIX + pipeline;

		DirectExchange exchange = new DirectExchange(exchangeName, false, true);
		admin.declareExchange(exchange);

		LOG.debug("[" + logId + "] Events exchange '" + exchangeName
				+ "' declared.");

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

						String messageJson = new String(message);

						LOG.debug("[" + logId + "] <-- Queue:'" + queue + "' "
								+ messageJson);

						receiver.onMessage(messageJson);
					}
				}, "onMessage");

		container.setMessageListener(adapter);
		container.setQueueNames(queue);
		container.start();

		LOG.debug("[" + logId + "] Registered receiver '"
				+ receiver.getClass().getName() + "' for queue '" + queue);
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

						LOG.debug("[" + logId + "] <-- Queue:'" + queue + "' "
								+ messageJson);

						String responseJson = receiver.onMessage(messageJson);
						LOG.debug("[" + logId + "] <-- " + responseJson);
						return responseJson;
					}
				}, "onMessage");

		container.setMessageListener(adapter);
		container.setQueueNames(queue);
		container.start();

		LOG.debug("[" + logId + "] Registered receiver with response '"
				+ receiver.getClass().getName() + "' for queue '" + queue);
	}

	public void send(String exchange, String routingKey, String message) {

		RabbitTemplate template = new RabbitTemplate(cf);
		LOG.debug("[" + logId + "] --> Exchange:'" + exchange
				+ "' RoutingKey:'" + routingKey + "' " + message);
		template.send(exchange, routingKey, new Message(message.getBytes(),
				new MessageProperties()));
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

		LOG.debug("[" + logId + "]--> Exchange:'" + exchange + "' RoutingKey:'"
				+ routingKey + "' " + message);
		Message response = template.sendAndReceive(exchange, routingKey,
				new Message(message.getBytes(), new MessageProperties()));

		if (response == null) {
			throw new BrokerException("Timeout waiting a reply to message: "
					+ message);
		}

		String responseAsString = new String(response.getBody());
		LOG.debug("[" + logId + "] <-- " + responseAsString);
		return responseAsString;
	}

	public void bindExchangeToQueue(String exchangeId, String queueId,
			String eventRoutingKey) {

		Queue queue = new Queue(queueId, false, true, true);
		DirectExchange exchange = new DirectExchange(exchangeId);

		admin.declareBinding(BindingBuilder.bind(queue).to(exchange)
				.with(eventRoutingKey));

		LOG.debug("[" + logId + "] Exchange '" + exchangeId
				+ "' bind to queue '" + queueId + "' with routingKey '"
				+ eventRoutingKey + "'");

	}

	public String createRoutingKey(String mediaElementId, String eventType) {
		return mediaElementId + "/" + eventType;
	}

	public void destroy() {
		cf.destroy();
	}
}
