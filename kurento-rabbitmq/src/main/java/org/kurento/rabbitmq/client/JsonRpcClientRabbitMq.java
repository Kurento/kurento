package org.kurento.rabbitmq.client;

import static org.kurento.jsonrpc.JsonUtils.fromJsonRequest;
import static org.kurento.rabbitmq.RabbitMqManager.PIPELINE_CREATION_QUEUE;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants;
import org.kurento.commons.Address;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.KeepAliveManager;
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.rabbitmq.RabbitMqManager;
import org.kurento.rabbitmq.RabbitMqManager.BrokerMessageReceiver;
import org.kurento.rabbitmq.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonRpcClientRabbitMq extends JsonRpcClient {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcClientRabbitMq.class);

	private final ExecutorService execService = Executors
			.newFixedThreadPool(10);

	private RabbitMqManager rabbitMqManager;

	private String clientId;

	private RabbitTemplate rabbitTemplate;

	private String defaultSessionId = UUID.randomUUID().toString();

	private final ResponseSender dummyResponseSenderForEvents = new ResponseSender() {
		@Override
		public void sendResponse(Message message) throws IOException {
			log.warn(
					"The broker client is trying to send the response '{}' for "
							+ "a request from server. But with broker it is"
							+ " not yet implemented", message);
		}

		@Override
		public void sendPingResponse(Message message) throws IOException {
			log.warn(
					"The broker client is trying to send the response '{}' for "
							+ "a request from server. But with broker it is"
							+ " not yet implemented", message);
		}
	};

	public JsonRpcClientRabbitMq(String host, String port, String username,
			String password, String vhost) throws IOException {
		this(new RabbitMqManager(host, port, username, password, vhost));
	}

	public JsonRpcClientRabbitMq() throws IOException {
		this(new Address("127.0.0.1", 5672));
	}

	public JsonRpcClientRabbitMq(Address rabbitMqAddress) throws IOException {
		this(new RabbitMqManager(rabbitMqAddress));
	}

	public JsonRpcClientRabbitMq(RabbitMqManager rabbitMqManager)
			throws IOException {
		this.rabbitMqManager = rabbitMqManager;
		this.connect();
	}

	@Override
	public void connect() throws IOException {
		connectIfNecessary();
	}

	private synchronized void connectIfNecessary() {

		if (clientId == null) {

			this.rabbitMqManager.connect();

			Queue queue = rabbitMqManager.declareClientQueue();

			clientId = queue.getName();

			rabbitTemplate = rabbitMqManager.createClientTemplate();

			this.rsHelper = new JsonRpcRequestSenderHelper() {
				@Override
				public <P, R> Response<R> internalSendRequest(
						Request<P> request, Class<R> resultClass)
								throws IOException {
					return internalSendRequestBroker(request, resultClass);
				}

				@Override
				protected void internalSendRequest(
						Request<? extends Object> request,
						Class<JsonElement> resultClass,
						Continuation<Response<JsonElement>> continuation) {
					internalSendRequestBroker(request, resultClass,
							continuation);
				}
			};

			keepAliveManager = new KeepAliveManager(this,
					KeepAliveManager.Mode.PER_ID_AS_MEDIAPIPELINE);

			keepAliveManager.start();
		}
	}

	private void handleRequestFromServer(String message) {
		try {
			handlerManager.handleRequest(session,
					fromJsonRequest(message, JsonElement.class),
					dummyResponseSenderForEvents);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public <P, R> Response<R> internalSendRequestBroker(Request<P> request,
			Class<R> resultClass) {

		connectIfNecessary();

		long initTime = System.nanoTime();

		log.debug("Req-> {}", request);

		JsonObject paramsJson = (JsonObject) request.getParams();

		if (request.getSessionId() == null) {
			// RabbitMQ doesn't allow sending requests without sessionId. It is
			// used to filter retried requests / responses
			request.setSessionId(defaultSessionId);
		}

		try {

			Response<R> response;

			if (RomJsonRpcConstants.CREATE_METHOD.equals(request.getMethod())
					&& "MediaPipeline".equals(paramsJson.get("type")
							.getAsString())) {

				String responseStr = rabbitMqManager.sendAndReceive("",
						PIPELINE_CREATION_QUEUE, request, rabbitTemplate);

				log.debug("<-Res {}", responseStr.trim());

				response = JsonUtils.fromJsonResponse(responseStr, resultClass);

				String mediaPipelineId;
				if (response.getResult() instanceof JsonObject) {
					mediaPipelineId = ((JsonObject) response.getResult()).get(
							"value").getAsString();
				} else {
					mediaPipelineId = ((JsonPrimitive) response.getResult())
							.getAsString();
				}

				keepAliveManager.addId(mediaPipelineId);

			} else {

				String method = request.getMethod();

				String pipelineId;

				if (RomJsonRpcConstants.CREATE_METHOD.equals(method)) {

					JsonObject constructorParams = paramsJson.get(
							RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS)
							.getAsJsonObject();

					if (constructorParams.has("mediaPipeline")) {
						pipelineId = constructorParams.get("mediaPipeline")
								.getAsString();
					} else {
						pipelineId = extractPipelineFromObjectId(constructorParams
								.get("hub").getAsString());
					}

				} else {

					// All messages has the same param name for "object"
					String objectId = paramsJson.get(
							RomJsonRpcConstants.INVOKE_OBJECT).getAsString();

					pipelineId = extractPipelineFromObjectId(objectId);

					if (RomJsonRpcConstants.SUBSCRIBE_METHOD.equals(method)) {
						processSubscriptionRequest(paramsJson, pipelineId);
					} else if (RomJsonRpcConstants.RELEASE_METHOD
							.equals(method)) {

						// Remove from keepAliveManager if the released object
						// is a MediaPipeline object
						keepAliveManager.removeId(objectId);
					}
				}

				String responseStr = rabbitMqManager.sendAndReceive("",
						pipelineId, request, rabbitTemplate);

				log.debug("<-Res {}", responseStr.trim());

				response = JsonUtils.fromJsonResponse(responseStr, resultClass);
			}

			double duration = (System.nanoTime() - initTime) / (double) 1000000;

			log.debug("RTT Time: {} millis", duration);

			return response;

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while invoking request to server", e);
		}
	}

	private String extractPipelineFromObjectId(String brokerObjectId) {
		int slashIndex = brokerObjectId.indexOf('/');
		if (slashIndex == -1) {
			// It is a BrokerPipelineId
			return brokerObjectId;
		} else {
			// It is another object
			return brokerObjectId.substring(0, slashIndex);
		}
	}

	private void processSubscriptionRequest(JsonObject paramsJson,
			String pipeline) {

		String eventType = paramsJson.get(RomJsonRpcConstants.SUBSCRIBE_TYPE)
				.getAsString();
		String element = paramsJson.get(RomJsonRpcConstants.SUBSCRIBE_OBJECT)
				.getAsString();

		final String eventRoutingKey = rabbitMqManager.createRoutingKey(
				element, eventType);

		rabbitMqManager.bindExchangeToQueue(RabbitMqManager.EVENT_QUEUE_PREFIX
				+ pipeline, clientId, eventRoutingKey);

		rabbitMqManager.addMessageReceiver(clientId,
				new BrokerMessageReceiver() {
			@Override
			public void onMessage(String message) {
				handleRequestFromServer(message);
			}
		});
	}

	protected void internalSendRequestBroker(
			final Request<? extends Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		connectIfNecessary();

		// FIXME: Poor man async implementation.
		execService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					Response<JsonElement> result = internalSendRequestBroker(
							request, resultClass);
					try {
						continuation.onSuccess(result);
					} catch (Exception e) {
						log.error("Exception while processing response", e);
					}
				} catch (Exception e) {
					continuation.onError(e);
				}
			}
		});
	}

	@Override
	public void close() throws IOException {
		log.debug("Closing connection to broker of the RabbitMqMediaConnector");
		if (rabbitMqManager != null) {
			rabbitMqManager.destroy();
		}
	}

}
