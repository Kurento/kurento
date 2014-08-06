package org.kurento.client.internal.transport.jsonrpcconnector;

import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_METHOD;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_TYPE;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_METHOD;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OPERATION_NAME;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OPERATION_PARAMS;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_DATA;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_OBJECT;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_SUBSCRIPTION;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_TYPE;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.RELEASE_METHOD;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.RELEASE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.SUBSCRIBE_METHOD;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.SUBSCRIBE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpcconnector.RomJsonRpcConstants.SUBSCRIBE_TYPE;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.kurento.client.Continuation;
import org.kurento.client.internal.client.RomClient;
import org.kurento.client.internal.client.RomEventHandler;
import org.kurento.client.internal.server.MediaServerException;
import org.kurento.client.internal.server.MediaServerTransportException;
import org.kurento.client.internal.server.ProtocolException;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;

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
			// TODO: Print error?
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

			client.sendRequest(
					method,
					params,
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
			throw new MediaServerTransportException(
					"Error connecting with server", e);
		} catch (JsonRpcErrorException e) {
			throw new MediaServerException("Exception invoking the ", e);
		}
	}

	@SuppressWarnings("unchecked")
	private <P, R> R processReqResult(final Type type,
			Function<P, R> processor, JsonElement reqResult) {
		P methodResult = convertFromResult(reqResult, type);

		if (processor == null) {
			return (R) methodResult;
		}

		return processor.apply(methodResult);
	}

	private <E> E convertFromResult(JsonElement result, Type type) {

		if (type == Void.class || type == void.class) {
			return null;
		}

		JsonElement extractResult = extractValueFromResponse(result, type);

		return JsonUtils.fromJson(extractResult, type);
	}

	private JsonElement extractValueFromResponse(JsonElement result, Type type) {

		if (result == null) {
			return null;
		}

		if (isPrimitiveClass(type) || isEnum(type)) {

			if (result instanceof JsonPrimitive) {
				return result;

			} else if (result instanceof JsonArray) {
				throw new ProtocolException("Json array '" + result
						+ " cannot be converted to " + getTypeName(type));
			} else if (result instanceof JsonObject) {
				return extractSimpleValueFromJsonObject((JsonObject) result,
						type);
			} else {
				throw new ProtocolException("Unrecognized json element: "
						+ result);
			}

		} else if (isList(type)) {

			if (result instanceof JsonArray) {
				return result;
			}

			return extractSimpleValueFromJsonObject((JsonObject) result, type);
		} else {
			return result;
		}
	}

	private JsonElement extractSimpleValueFromJsonObject(JsonObject result,
			Type type) {

		if (!result.has("value")) {
			throw new ProtocolException("Json object " + result
					+ " cannot be converted to " + getTypeName(type)
					+ " without a 'value' property");
		}

		return result.get("value");
	}

	private boolean isEnum(Type type) {

		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			return clazz.isEnum();
		}

		return false;
	}

	private boolean isPrimitiveClass(Type type) {
		return type == String.class || type == Integer.class
				|| type == Float.class || type == Boolean.class
				|| type == int.class || type == float.class
				|| type == boolean.class;
	}

	private boolean isList(Type type) {

		if (type == List.class) {
			return true;
		}

		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			if (pType.getRawType() instanceof Class) {
				return ((Class<?>) pType.getRawType())
						.isAssignableFrom(List.class);
			}
		}

		return false;
	}

	private String getTypeName(Type type) {

		if (type instanceof Class) {

			Class<?> clazz = (Class<?>) type;
			return clazz.getSimpleName();

		} else if (type instanceof ParameterizedType) {

			StringBuilder sb = new StringBuilder();

			ParameterizedType pType = (ParameterizedType) type;
			Class<?> rawClass = (Class<?>) pType.getRawType();

			sb.append(rawClass.getSimpleName());

			Type[] arguments = pType.getActualTypeArguments();
			if (arguments.length > 0) {
				sb.append('<');
				for (Type aType : arguments) {
					sb.append(getTypeName(aType));
					sb.append(',');
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append('>');
			}

			return sb.toString();
		}

		return type.toString();
	}

}
