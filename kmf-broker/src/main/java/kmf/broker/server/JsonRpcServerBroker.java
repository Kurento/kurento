package kmf.broker.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kmf.broker.Broker;
import kmf.broker.Broker.BrokerMessageReceiverWithResponse;
import kmf.broker.Broker.ExchangeAndQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants;

public class JsonRpcServerBroker {

	private static Logger LOG = LoggerFactory
			.getLogger(JsonRpcServerBroker.class);

	private Map<String, MediaPipelineInfo> pipelinesByBrokerId = new ConcurrentHashMap<>();
	private Map<String, MediaPipelineInfo> pipelinesBySubscription = new ConcurrentHashMap<>();

	private JsonRpcClient client;
	private Broker broker;

	private ObjectIdsConverter converter = new ObjectIdsConverter();

	public JsonRpcServerBroker(JsonRpcClient client) {
		this.client = client;
		this.broker = new Broker(">MS");
	}

	public void start() {
		this.broker.init();

		broker.addMessageReceiverWithResponse(Broker.PIPELINE_CREATION_QUEUE,
				new BrokerMessageReceiverWithResponse() {
			@Override
			public String onMessage(String message) {

				return processRequestFromBroker(message);
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

	private String processRequestFromBroker(String message) {
		LOG.debug("[PCQ] --> {}", message);

		Request<JsonObject> request = JsonUtils.fromJsonRequest(message,
				JsonObject.class);

		String response = createMediaPipeline(request).toString();

		LOG.debug("[PCQ] <-- {}", response);

		return response;
	}

	private void processEventFromServer(Request<JsonObject> request) {

		if (!RomJsonRpcConstants.ONEVENT_METHOD.equals(request.getMethod())) {
			LOG.warn("Unrecognized server message {}", request);
			return;
		}

		JsonObject value = request.getParams().get("value").getAsJsonObject();

		String subscriptionId = value.get(
				RomJsonRpcConstants.ONEVENT_SUBSCRIPTION).getAsString();

		MediaPipelineInfo pipelineInfo = pipelinesBySubscription
				.get(subscriptionId);

		String objectId = value.get(RomJsonRpcConstants.ONEVENT_OBJECT)
				.getAsString();

		String brokerObjectId = converter.createBrokerObjectId(
				pipelineInfo.getBrokerPipelineId(), objectId);

		value.addProperty(RomJsonRpcConstants.ONEVENT_OBJECT, brokerObjectId);

		String type = value.get(RomJsonRpcConstants.ONEVENT_TYPE).getAsString();

		final String eventRoutingKey = broker.createRoutingKey(brokerObjectId,
				type);

		broker.send(pipelineInfo.getEventsExchange(), eventRoutingKey,
				request.toString());

	}

	private Response<JsonElement> createMediaPipeline(
			Request<JsonObject> request) {

		try {

			JsonElement response = client.sendRequest(request.getMethod(),
					request.getParams());

			final String realPipelineId = getValue(response);

			ExchangeAndQueue pipelineEQ = broker.declarePipelineQueue();

			final String brokerPipelineId = pipelineEQ.getExchangeName();

			broker.addMessageReceiverWithResponse(pipelineEQ.getQueueName(),
					new BrokerMessageReceiverWithResponse() {
				@Override
				public String onMessage(String message) {

					LOG.debug("[PQ] --> {}", message);

					String response = onPipelineMessage(
							brokerPipelineId, realPipelineId, message);

					LOG.debug("[PQ] <-- {}", response);

					return response;
				}
			});

			String exchange = broker.declareEventsExchange(brokerPipelineId);

			MediaPipelineInfo pipeline = new MediaPipelineInfo(
					brokerPipelineId, realPipelineId, exchange);

			this.pipelinesByBrokerId.put(brokerPipelineId, pipeline);

			return new Response<JsonElement>(request.getId(),
					new JsonPrimitive(brokerPipelineId));

		} catch (Exception e) {
			return new Response<JsonElement>(request.getId(),
					ResponseError.newFromException(request.getId(), e));
		}
	}

	private String getValue(JsonElement response) {

		if (response instanceof JsonPrimitive) {
			return response.getAsString();
		} else {
			if (response instanceof JsonObject) {
				JsonObject json = (JsonObject) response;
				return getValue(json.entrySet().iterator().next().getValue());
			} else {
				throw new RuntimeException(
						"Can't extract a single value from jsonElement: "
								+ response);
			}
		}

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
				throw new RuntimeException(
						"Invalid message in pipeline queue: " + message);
			}
		} catch (Exception e) {

			LOG.warn("Exception processing request from client. ", e);

			return new Response<JsonElement>(request.getId(),
					ResponseError.newFromException(request.getId(), e))
					.toString();
		}
	}

	private Object release(String realPipelineId, String brokerPipelineId,
			Request<JsonObject> request) {

		String brokerObjectId = request.getParams()
				.get(RomJsonRpcConstants.RELEASE_OBJECT).getAsString();

		boolean isPipeline = pipelinesByBrokerId.containsKey(brokerObjectId);

		Response<JsonElement> response = invokeOperation(request);

		if (!response.isError() && isPipeline) {
			pipelinesByBrokerId.remove(brokerObjectId);
		}

		return response;
	}

	private Response<JsonElement> createMediaElement(String brokerPipelineId,
			Request<JsonObject> request) {

		try {

			converter.convertRefsFromBrokerToReal(request.getMethod(),
					request.getParams(), pipelinesByBrokerId);

			JsonElement response = client.sendRequest(request.getMethod(),
					request.getParams());

			String realObjectId = getValue(response);

			String brokerObjectId = converter.createBrokerObjectId(
					brokerPipelineId, realObjectId);

			return new Response<JsonElement>(request.getId(),
					new JsonPrimitive(brokerObjectId));

		} catch (Exception e) {
			return new Response<JsonElement>(request.getId(),
					ResponseError.newFromException(request.getId(), e));
		}
	}

	private Response<JsonElement> invokeOperation(Request<JsonObject> request) {

		converter.convertRefsFromBrokerToReal(request.getMethod(),
				request.getParams(), pipelinesByBrokerId);

		try {

			JsonElement result = client.sendRequest(request.getMethod(),
					request.getParams());

			converter.convertRefsFromRealToBroker(null, result);

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

	private Response<JsonElement> subscribeMessage(String brokerPipelineId,
			Request<JsonObject> request) {

		Response<JsonElement> response = invokeOperation(request);

		if (!response.isError()) {
			String subscriptionId = ((JsonObject) response.getResult()).get(
					"value").getAsString();
			this.pipelinesBySubscription.put(subscriptionId,
					pipelinesByBrokerId.get(brokerPipelineId));
		}

		return response;
	}

	public void destroy() throws IOException {
		if (client != null) {
			client.close();
		}
	}

}
