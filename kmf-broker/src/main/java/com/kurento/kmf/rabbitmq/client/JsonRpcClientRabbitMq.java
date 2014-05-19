package com.kurento.kmf.rabbitmq.client;

import static com.kurento.kmf.jsonrpcconnector.JsonUtils.fromJsonRequest;
import static com.kurento.kmf.rabbitmq.RabbitMqManager.PIPELINE_CREATION_QUEUE;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.common.Address;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.rabbitmq.RabbitMqManager;
import com.kurento.kmf.rabbitmq.RabbitMqManager.BrokerMessageReceiver;
import com.kurento.kmf.rabbitmq.server.ObjectIdsConverter;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants;

public class JsonRpcClientRabbitMq extends JsonRpcClient {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcClientRabbitMq.class);

	private final ExecutorService execService = Executors
			.newFixedThreadPool(10);

	private final ObjectIdsConverter converter = new ObjectIdsConverter();

	private RabbitMqManager rabbitMqManager;

	private String clientId;

	private RabbitTemplate rabbitTemplate;

	private final ResponseSender dummyResponseSenderForEvents = new ResponseSender() {
		@Override
		public void sendResponse(Message message) throws IOException {
			log.warn(
					"The broker client is trying to send the response '{}' for "
							+ "a request from server. But with broker it is"
							+ " not yet implemented", message);
		}
	};

	public JsonRpcClientRabbitMq(Address rabbitMqAddress) {
		this(new RabbitMqManager(rabbitMqAddress));
	}

	public JsonRpcClientRabbitMq(RabbitMqManager rabbitMqManager) {

		this.rabbitMqManager = rabbitMqManager;
		this.rabbitMqManager.connect();

		Queue queue = rabbitMqManager.declareClientQueue();

		clientId = queue.getName();

		rabbitTemplate = rabbitMqManager.createClientTemplate();

		this.rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				return internalSendRequestBroker(request, resultClass);
			}

			@Override
			protected void internalSendRequest(Request<Object> request,
					Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {
				internalSendRequestBroker(request, resultClass, continuation);
			}
		};
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

		log.trace("Req-> {}", request);

		JsonObject paramsJson = (JsonObject) request.getParams();

		try {

			String responseStr = null;

			if (RomJsonRpcConstants.CREATE_METHOD.equals(request.getMethod())
					&& "MediaPipeline".equals(paramsJson.get("type")
							.getAsString())) {

				responseStr = rabbitMqManager.sendAndReceive("",
						PIPELINE_CREATION_QUEUE, request.toString(),
						rabbitTemplate);

			} else {

				String method = request.getMethod();

				String brokerPipelineId;

				if (RomJsonRpcConstants.CREATE_METHOD.equals(method)) {

					JsonObject constructorParams = paramsJson.get(
							RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS)
							.getAsJsonObject();

					if (constructorParams.has("mediaPipeline")) {
						brokerPipelineId = constructorParams.get(
								"mediaPipeline").getAsString();
					} else {
						brokerPipelineId = converter
								.extractBrokerPipelineFromBrokerObjectId(constructorParams
										.get("hub").getAsString());
					}

				} else {

					// All messages has the same param name for "object"
					String brokerObjectId = paramsJson.get(
							RomJsonRpcConstants.INVOKE_OBJECT).getAsString();

					brokerPipelineId = converter
							.extractBrokerPipelineFromBrokerObjectId(brokerObjectId);

					if (RomJsonRpcConstants.SUBSCRIBE_METHOD.equals(method)) {
						processSubscriptionRequest(paramsJson, brokerPipelineId);
					}
				}

				responseStr = rabbitMqManager.sendAndReceive("",
						brokerPipelineId, request.toString(), rabbitTemplate);
			}

			log.debug("<-Res {}", responseStr.trim());

			Response<R> response = JsonUtils.fromJsonResponse(responseStr,
					resultClass);

			return response;

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while invoking request to server", e);
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

	protected void internalSendRequestBroker(final Request<Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

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
