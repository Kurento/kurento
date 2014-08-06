package org.kurento.rabbitmq.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;




import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.kurento.common.Address;
import org.kurento.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.jsonrpcconnector.JsonRpcHandler;
import org.kurento.jsonrpcconnector.JsonUtils;
import org.kurento.jsonrpcconnector.Transaction;
import org.kurento.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.jsonrpcconnector.client.JsonRpcClientLocal;
import org.kurento.jsonrpcconnector.internal.message.Request;
import org.kurento.jsonrpcconnector.internal.message.Response;
import org.kurento.jsonrpcconnector.internal.message.ResponseError;
import org.kurento.rabbitmq.RabbitMqManager;
import org.kurento.rabbitmq.RabbitTemplate;
import org.kurento.rabbitmq.RabbitMqManager.BrokerMessageReceiverWithResponse;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants;

public class JsonRpcServerRabbitMq {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcServerRabbitMq.class);

	private Map<String, MediaPipelineInfo> pipelinesById = new ConcurrentHashMap<>();
	private Map<String, MediaPipelineInfo> pipelinesBySubscription = new ConcurrentHashMap<>();

	private JsonRpcClient client;
	private RabbitMqManager rabbitMq;

	private RabbitTemplate template;

	// TODO: Maybe we need to implement a pure JsonRpcServerRabbitMq with
	// handler parameter instead of this client > handler communication
	public JsonRpcServerRabbitMq(JsonRpcHandler<?> handler) {
		this(new JsonRpcClientLocal(handler));
	}

	public JsonRpcServerRabbitMq(JsonRpcClient client) {
		this(client, new Address("127.0.0.1", 5672));
	}

	public JsonRpcServerRabbitMq(JsonRpcClient client, Address rabbitMqAddress) {
		this.client = client;
		this.rabbitMq = new RabbitMqManager(rabbitMqAddress);
	}

	public void start() {
		this.rabbitMq.connect();
		this.template = rabbitMq.createServerTemplate();

		rabbitMq.addMessageReceiverWithResponse(
				RabbitMqManager.PIPELINE_CREATION_QUEUE,
				new BrokerMessageReceiverWithResponse() {
					@Override
					public String onMessage(String message) {
						return pipelineCreationQueueRequest(message);
					}
				});

		this.client
				.setServerRequestHandler(new DefaultJsonRpcHandler<JsonObject>() {

					@Override
					public void handleRequest(Transaction transaction,
							Request<JsonObject> request) throws Exception {

						processEventFromServer(request);
					}
				});
	}

	private String pipelineCreationQueueRequest(String message) {
		log.debug("[PCQ] --> {}", message);

		Request<JsonObject> request = JsonUtils.fromJsonRequest(message,
				JsonObject.class);

		String response = createMediaPipeline(request).toString();

		log.debug("[PCQ] <-- {}", response);

		return response;
	}

	private Response<JsonElement> createMediaPipeline(
			Request<JsonObject> request) {

		try {

			JsonElement response = client.sendRequest(request.getMethod(),
					request.getParams());

			final String pipelineId = getValue(response);

			rabbitMq.declarePipelineQueue(pipelineId);

			rabbitMq.addMessageReceiverWithResponse(pipelineId,
					new BrokerMessageReceiverWithResponse() {
						@Override
						public String onMessage(String message) {

							return pipelineQueueRequest(pipelineId, message);
						}
					});

			String exchange = rabbitMq.declareEventsExchange(pipelineId);

			MediaPipelineInfo pipeline = new MediaPipelineInfo(pipelineId,
					pipelineId, exchange);

			this.pipelinesById.put(pipelineId, pipeline);

			return new Response<JsonElement>(request.getId(),
					new JsonPrimitive(pipelineId));

		} catch (Exception e) {
			return new Response<JsonElement>(request.getId(),
					ResponseError.newFromException(request.getId(), e));
		}
	}

	private void processEventFromServer(Request<JsonObject> request) {

		if (!RomJsonRpcConstants.ONEVENT_METHOD.equals(request.getMethod())) {
			log.warn("Unrecognized server message {}", request);
			return;
		}

		JsonObject value = request.getParams().get("value").getAsJsonObject();

		String subscriptionId = value.get(
				RomJsonRpcConstants.ONEVENT_SUBSCRIPTION).getAsString();

		String objectId = value.get(RomJsonRpcConstants.ONEVENT_OBJECT)
				.getAsString();

		value.addProperty(RomJsonRpcConstants.ONEVENT_OBJECT, objectId);

		String type = value.get(RomJsonRpcConstants.ONEVENT_TYPE).getAsString();

		final String eventRoutingKey = rabbitMq
				.createRoutingKey(objectId, type);

		MediaPipelineInfo pipelineInfo = pipelinesBySubscription
				.get(subscriptionId);

		if (pipelineInfo == null) {
			log.debug("PipelinesBySubscription: " + pipelinesBySubscription);
		}

		rabbitMq.send(pipelineInfo.getEventsExchange(), eventRoutingKey,
				request.toString(), template);

	}

	private String pipelineQueueRequest(final String pipelineId, String message) {
		log.debug("[PQ] --> {}", message);

		String response = onPipelineMessage(pipelineId, pipelineId, message);

		log.debug("[PQ] <-- {}", response);

		return response;
	}

	public String onPipelineMessage(String brokerPipelineId,
			String realPipelineId, String message) {

		Request<JsonObject> request = JsonUtils.fromJsonRequest(message,
				JsonObject.class);

		try {

			switch (request.getMethod()) {
			case RomJsonRpcConstants.CREATE_METHOD:
				return createMediaElement(brokerPipelineId, request).toString();
			case RomJsonRpcConstants.INVOKE_METHOD:
				return invokeOperation(request).toString();
			case RomJsonRpcConstants.SUBSCRIBE_METHOD:
				return subscribeMessage(brokerPipelineId, request).toString();
			case RomJsonRpcConstants.RELEASE_METHOD:
				return release(realPipelineId, brokerPipelineId, request)
						.toString();
			default:
				return invokeOperation(request).toString();
			}

		} catch (Exception e) {

			log.warn("Exception processing request from client. ", e);

			return new Response<JsonElement>(request.getId(),
					ResponseError.newFromException(request.getId(), e))
					.toString();
		}
	}

	private Object release(String realPipelineId, String brokerPipelineId,
			Request<JsonObject> request) {

		String objectId = request.getParams()
				.get(RomJsonRpcConstants.RELEASE_OBJECT).getAsString();

		Response<JsonElement> response = invokeOperation(request);

		if (!response.isError() && pipelinesById.containsKey(objectId)) {
			pipelinesById.remove(objectId);
		}

		return response;
	}

	private Response<JsonElement> createMediaElement(String brokerPipelineId,
			Request<JsonObject> request) {

		try {

			JsonElement response = client.sendRequest(request.getMethod(),
					request.getParams());

			String realObjectId = getValue(response);

			return new Response<JsonElement>(request.getId(),
					new JsonPrimitive(realObjectId));

		} catch (Exception e) {
			return new Response<JsonElement>(request.getId(),
					ResponseError.newFromException(request.getId(), e));
		}
	}

	private Response<JsonElement> invokeOperation(Request<JsonObject> request) {

		try {

			JsonElement result = client.sendRequest(request.getMethod(),
					request.getParams());

			// TODO: Improve this handling of null responses
			if (result == null || result instanceof JsonNull) {
				result = new JsonObject();
			}

			return new Response<JsonElement>(request.getId(), result);

		} catch (Exception e) {
			return new Response<JsonElement>(request.getId(),
					ResponseError.newFromException(request.getId(), e));
		}
	}

	private Response<JsonElement> subscribeMessage(String pipelineId,
			Request<JsonObject> request) {

		Response<JsonElement> response = invokeOperation(request);

		if (!response.isError()) {

			String subscriptionId = ((JsonObject) response.getResult()).get(
					"value").getAsString();

			this.pipelinesBySubscription.put(subscriptionId,
					pipelinesById.get(pipelineId));
		}

		return response;
	}

	public void destroy() throws IOException {
		if (client != null) {
			client.close();
		}

		this.rabbitMq.destroy();
	}

	private String getValue(JsonElement response) {

		if (response == null) {
			return null;
		}

		if (response instanceof JsonPrimitive) {
			return response.getAsString();
		}

		if (response instanceof JsonObject) {
			JsonObject json = (JsonObject) response;
			return getValue(json.entrySet().iterator().next().getValue());
		}

		throw new RuntimeException(
				"Can't extract a single value from jsonElement: " + response);

	}

}
