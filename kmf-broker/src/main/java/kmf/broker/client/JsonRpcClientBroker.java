package kmf.broker.client;

import static com.kurento.kmf.jsonrpcconnector.JsonUtils.fromJsonRequest;
import static kmf.broker.Broker.PIPELINE_CREATION_QUEUE;

import java.io.IOException;

import kmf.broker.Broker;
import kmf.broker.Broker.BrokerMessageReceiver;
import kmf.broker.Broker.ExchangeAndQueue;
import kmf.broker.server.ObjectIdsConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants;

public class JsonRpcClientBroker extends JsonRpcClient {

	private static final Logger LOG = LoggerFactory
			.getLogger(JsonRpcClientBroker.class);

	private ObjectIdsConverter converter = new ObjectIdsConverter();

	private Broker broker;

	private String clientId;

	private RabbitTemplate rabbitTemplate;

	private final ResponseSender dummyResponseSenderForEvents = new ResponseSender() {
		@Override
		public void sendResponse(Message message) throws IOException {
			LOG.warn(
					"The broker client is trying to send the response '{}' for "
							+ "a request from server. But with broker it is"
							+ " not yet implemented", message);
		}
	};

	public JsonRpcClientBroker(Broker broker) {

		this.broker = broker;

		ExchangeAndQueue eq = broker.declareClientQueue();
		clientId = eq.getQueueName();

		rabbitTemplate = broker.createClientTemplate();

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

		JsonObject paramsJson = (JsonObject) request.getParams();

		try {

			String responseStr = null;

			if (RomJsonRpcConstants.CREATE_METHOD.equals(request.getMethod())
					&& "MediaPipeline".equals(paramsJson.get("type")
							.getAsString())) {

				responseStr = broker.sendAndReceive(PIPELINE_CREATION_QUEUE,
						"", request.toString(), rabbitTemplate);

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

				responseStr = broker.sendAndReceive(brokerPipelineId, "",
						request.toString(), rabbitTemplate);
			}

			LOG.debug("[Client] Response received: {}", responseStr);

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

		final String eventRoutingKey = broker.createRoutingKey(element,
				eventType);

		broker.bindExchangeToQueue(Broker.EVENT_QUEUE_PREFIX + pipeline,
				clientId, eventRoutingKey);

		broker.addMessageReceiver(clientId, new BrokerMessageReceiver() {
			@Override
			public void onMessage(String message) {
				handleRequestFromServer(message);
			}
		});
	}

	protected void internalSendRequestBroker(Request<Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		throw new UnsupportedOperationException("Async API are not yet allowed");
	}

	@Override
	public void close() throws IOException {
		if (broker != null) {
			broker.destroy();
		}
	}

}
