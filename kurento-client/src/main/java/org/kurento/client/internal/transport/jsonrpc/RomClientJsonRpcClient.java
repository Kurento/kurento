package org.kurento.client.internal.transport.jsonrpc;

import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_TYPE;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OPERATION_NAME;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OPERATION_PARAMS;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.ONEVENT_DATA;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.ONEVENT_OBJECT;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.ONEVENT_SUBSCRIPTION;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.ONEVENT_TYPE;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.RELEASE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.RELEASE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.SUBSCRIBE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.SUBSCRIBE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.SUBSCRIBE_TYPE;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.Set;

import org.kurento.client.Continuation;
import org.kurento.client.internal.client.RomClient;
import org.kurento.client.internal.client.RomEventHandler;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.client.internal.server.KurentoServerTransportException;
import org.kurento.client.internal.server.ProtocolException;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RomClientJsonRpcClient extends RomClient {

	private static final Logger log = LoggerFactory
			.getLogger(RomClientJsonRpcClient.class);

	private final JsonRpcClient client;

	public RomClientJsonRpcClient(JsonRpcClient client) {
		this.client = client;
	}

	@Override
	public Object invoke(String objectRef, String operationName,
			Props operationParams, Type type) {
		return invoke(objectRef, operationName, operationParams, type, null);
	}

	@Override
	public String subscribe(String objectRef, String type) {
		return subscribe(objectRef, type, null);
	}

	@Override
	public String create(String remoteClassName, Props constructorParams) {
		return create(remoteClassName, constructorParams, null);
	}

	@Override
	public void release(String objectRef) {
		release(objectRef, null);
	}

	@Override
	public String create(String remoteClassName, Props constructorParams,
			Continuation<String> cont) {

		JsonObject params = new JsonObject();
		params.addProperty(CREATE_TYPE, remoteClassName);
		if (constructorParams != null) {

			Props flatParams = ParamsFlattener.getInstance().flattenParams(
					constructorParams);

			params.add(CREATE_CONSTRUCTOR_PARAMS,
					JsonUtils.toJsonObject(flatParams));
		}

		return this.<String, String> sendRequest(CREATE_METHOD, String.class,
				params, null, cont);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E invoke(String objectRef, String operationName,
			Props operationParams, Class<E> clazz) {
		return (E) invoke(objectRef, operationName, operationParams,
				(Type) clazz);
	}

	@Override
	public Object invoke(String objectRef, String operationName,
			Props operationParams, Type type, Continuation<?> cont) {

		JsonObject params = new JsonObject();
		params.addProperty(INVOKE_OBJECT, objectRef);
		params.addProperty(INVOKE_OPERATION_NAME, operationName);

		if (operationParams != null) {

			Props flatParams = ParamsFlattener.getInstance().flattenParams(
					operationParams);

			params.add(INVOKE_OPERATION_PARAMS,
					JsonUtils.toJsonObject(flatParams));
		}

		return sendRequest(INVOKE_METHOD, type, params, null, cont);
	}

	@Override
	public void release(String objectRef, Continuation<Void> cont) {

		JsonObject params = JsonUtils.toJsonObject(new Props(RELEASE_OBJECT,
				objectRef));

		sendRequest(RELEASE_METHOD, Void.class, params, null, cont);
	}

	@Override
	public String subscribe(String objectRef, String eventType,
			Continuation<String> cont) {

		JsonObject params = JsonUtils.toJsonObject(new Props(SUBSCRIBE_OBJECT,
				objectRef).add(SUBSCRIBE_TYPE, eventType));

		Function<JsonElement, String> processor = new Function<JsonElement, String>() {
			@Override
			public String apply(JsonElement subscription) {

				if (subscription instanceof JsonPrimitive) {
					return subscription.getAsString();
				}

				JsonObject subsObject = (JsonObject) subscription;
				Set<Entry<String, JsonElement>> entries = subsObject.entrySet();
				if (entries.size() != 1) {
					throw new ProtocolException(
							"Error format in response to subscription operation."
									+ "The response should have one property and it has "
									+ entries.size() + ". The response is: "
									+ subscription);
				}

				return entries.iterator().next().getValue().getAsString();
			}
		};

		return sendRequest(SUBSCRIBE_METHOD, JsonElement.class, params,
				processor, cont);
	}

	@Override
	public void addRomEventHandler(final RomEventHandler eventHandler) {

		this.client
				.setServerRequestHandler(new DefaultJsonRpcHandler<JsonObject>() {

					@Override
					public void handleRequest(Transaction transaction,
							Request<JsonObject> request) throws Exception {
						processEvent(eventHandler, request);
					}
				});
	}

	private void processEvent(RomEventHandler eventHandler,
			Request<JsonObject> request) {

		JsonObject params = request.getParams();

		try {
			params = (JsonObject) params.get("value");
		} catch (Exception e) {
		}

		String objectRef = params.get(ONEVENT_OBJECT).getAsString();
		String subscription = "";
		if (params.has(ONEVENT_SUBSCRIPTION)) {
			subscription = params.get(ONEVENT_SUBSCRIPTION).getAsString();
		}
		String type = params.get(ONEVENT_TYPE).getAsString();
		JsonObject jsonData = (JsonObject) params.get(ONEVENT_DATA);
		Props data = JsonUtils.fromJson(jsonData, Props.class);

		eventHandler.processEvent(objectRef, subscription, type, data);
	}

	@Override
	public void destroy() {
		try {
			client.close();
		} catch (IOException e) {
			throw new RuntimeException("Exception while closing JsonRpcClient",
					e);
		}
	}

	@SuppressWarnings("unchecked")
	private <P, R> R sendRequest(String method, final Type type,
			JsonObject params, final Function<P, R> processor,
			final Continuation<R> cont) {
		try {

			if (cont == null) {

				return processReqResult(type, processor,
						client.sendRequest(method, params, JsonElement.class));

			}

			client.sendRequest(method, params,
					new org.kurento.jsonrpc.client.Continuation<JsonElement>() {

						@SuppressWarnings({ "rawtypes" })
						@Override
						public void onSuccess(JsonElement reqResult) {

							R methodResult = processReqResult(type, processor,
									reqResult);
							try {
								((Continuation) cont).onSuccess(methodResult);
							} catch (Exception e) {
								log.warn(
										"[Continuation] error invoking OnSuccess implemented by client",
										e);
							}
						}

						@Override
						public void onError(Throwable cause) {
							try {
								cont.onError(cause);
							} catch (Exception e) {
								log.warn(
										"[Continuation] error invoking onError implemented by client",
										e);
							}
						}
					});

			return null;

		} catch (IOException e) {
			throw new KurentoServerTransportException(
					"Error connecting with server", e);
		} catch (JsonRpcErrorException e) {
			throw new KurentoServerException("Exception invoking the ", e);
		}
	}

	@SuppressWarnings("unchecked")
	private <P, R> R processReqResult(final Type type,
			Function<P, R> processor, JsonElement reqResult) {
		P methodResult = JsonUtils.extractJavaValueFromResult(reqResult, type);

		if (processor == null) {
			return (R) methodResult;
		}

		return processor.apply(methodResult);
	}

}
