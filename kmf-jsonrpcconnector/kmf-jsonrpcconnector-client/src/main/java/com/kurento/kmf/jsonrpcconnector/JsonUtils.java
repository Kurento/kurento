/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.jsonrpcconnector;

import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.DATA_PROPERTY;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.ERROR_PROPERTY;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.ID_PROPERTY;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.JSON_RPC_PROPERTY;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.JSON_RPC_VERSION;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.METHOD_PROPERTY;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.PARAMS_PROPERTY;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.RESULT_PROPERTY;
import static com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants.SESSION_ID_PROPERTY;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.$Gson$Types;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;

/**
 * 
 * Gson/JSON utilities; used to serialize Java object to JSON (as String).
 * 
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @version 1.0.0
 */
public class JsonUtils {

	public static final boolean INJECT_SESSION_ID = true;
	/**
	 * Static instance of Gson object.
	 */
	private static Gson gson;

	/**
	 * Serialize Java object to JSON (as String).
	 * 
	 * @param obj
	 *            Java Object representing a JSON message to be serialized
	 * @return Serialized JSON message (as String)
	 */
	public static String toJson(Object obj) {
		return getGson().toJson(obj);
	}

	public static JsonObject toJsonObject(Object obj) {
		// TODO Optimize this implementation if possible
		return fromJson(getGson().toJson(obj), JsonObject.class);
	}

	public static <T> Request<T> fromJsonRequest(String json,
			Class<T> paramsClass) {

		if (INJECT_SESSION_ID) {

			// TODO Optimize this implementation if possible
			return fromJsonRequestInject(fromJson(json, JsonObject.class),
					paramsClass);

		} else {

			return getGson().fromJson(
					json,
					$Gson$Types.newParameterizedTypeWithOwner(null,
							Request.class, paramsClass));
		}
	}

	public static <T> Response<T> fromJsonResponse(String json,
			Class<T> resultClass) {

		if (INJECT_SESSION_ID) {

			// TODO Optimize this implementation if possible
			return fromJsonResponseInject(fromJson(json, JsonObject.class),
					resultClass);

		} else {
			try {

				return getGson().fromJson(
						json,
						$Gson$Types.newParameterizedTypeWithOwner(null,
								Response.class, resultClass));

			} catch (JsonSyntaxException e) {
				throw new RuntimeException("Exception converting Json '" + json
						+ "' to a JSON-RPC response with params as class "
						+ resultClass.getName());
			}
		}
	}

	public static <T> Request<T> fromJsonRequest(JsonObject json,
			Class<T> paramsClass) {

		if (INJECT_SESSION_ID) {

			// TODO Optimize this implementation if possible
			return fromJsonRequestInject(json, paramsClass);

		} else {

			return getGson().fromJson(
					json,
					$Gson$Types.newParameterizedTypeWithOwner(null,
							Request.class, paramsClass));
		}
	}

	public static <T> Response<T> fromJsonResponse(JsonObject json,
			Class<T> resultClass) {

		if (INJECT_SESSION_ID) {

			// TODO Optimize this implementation if possible
			return fromJsonResponseInject(json, resultClass);
		} else {

			return getGson().fromJson(
					json,
					$Gson$Types.newParameterizedTypeWithOwner(null,
							Response.class, resultClass));
		}
	}

	private static <T> Response<T> fromJsonResponseInject(
			JsonObject jsonObject, Class<T> resultClass) {

		try {

			String sessionId = extractSessionId(jsonObject, RESULT_PROPERTY);

			Response<T> response = JsonUtils.fromJson(jsonObject, $Gson$Types
					.newParameterizedTypeWithOwner(null, Response.class,
							resultClass));

			response.setSessionId(sessionId);
			return response;

		} catch (JsonSyntaxException e) {
			throw new RuntimeException("Exception converting Json '"
					+ jsonObject
					+ "' to a JSON-RPC response with params as class "
					+ resultClass.getName(), e);
		}
	}

	private static <T> Request<T> fromJsonRequestInject(JsonObject jsonObject,
			Class<T> paramsClass) {

		String sessionId = extractSessionId(jsonObject, PARAMS_PROPERTY);

		Request<T> request = getGson().fromJson(
				jsonObject,
				$Gson$Types.newParameterizedTypeWithOwner(null, Request.class,
						paramsClass));

		request.setSessionId(sessionId);
		return request;
	}

	private static String extractSessionId(JsonObject jsonObject,
			String memberName) {
		JsonElement responseJson = jsonObject.get(memberName);

		if (responseJson != null && responseJson.isJsonObject()) {

			JsonObject responseJsonObject = (JsonObject) responseJson;

			JsonElement sessionIdJson = responseJsonObject
					.remove(SESSION_ID_PROPERTY);

			if (sessionIdJson != null && !(sessionIdJson instanceof JsonNull)) {
				return sessionIdJson.getAsString();
			}
		}
		return null;
	}

	public static String toJson(Object obj, Type type) {
		return getGson().toJson(obj, type);
	}

	public static <T> String toJsonRequest(Request<T> request) {
		return getGson().toJson(
				request,
				$Gson$Types.newParameterizedTypeWithOwner(null, Request.class,
						getClassOrNull(request.getParams())));
	}

	public static <T> String toJsonResponse(Response<T> request) {
		return getGson().toJson(
				request,
				$Gson$Types.newParameterizedTypeWithOwner(null, Response.class,
						getClassOrNull(request.getResult())));
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		return getGson().fromJson(json, clazz);
	}

	public static <T> T fromJson(JsonElement json, Class<T> clazz) {
		return getGson().fromJson(json, clazz);
	}

	public static <T> T fromJson(String json, Type type) {
		return getGson().fromJson(json, type);
	}

	public static <T> T fromJson(JsonElement json, Type type) {
		return getGson().fromJson(json, type);
	}

	private static Class<?> getClassOrNull(Object object) {
		if (object == null) {
			return null;
		} else {
			return object.getClass();
		}
	}

	/**
	 * Gson object accessor (getter).
	 * 
	 * @return son object
	 */
	public static Gson getGson() {
		if (gson != null) {
			return gson;
		}

		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Request.class,
				new JsonRpcRequestDeserializer());

		builder.registerTypeAdapter(Response.class,
				new JsonRpcResponseDeserializer());

		builder.registerTypeAdapter(Props.class, new JsonPropsAdapter());

		gson = builder.create();

		return gson;
	}

	static boolean isIn(JsonObject jObject, String[] clues) {
		for (String clue : clues) {
			if (jObject.has(clue)) {
				return true;
			}
		}
		return false;
	}

	public static String toJsonMessage(Message message) {

		if (message.getSessionId() != null && INJECT_SESSION_ID) {

			JsonObject jsonObject = JsonUtils.toJsonObject(message);

			JsonObject objectToInjectSessionId;
			if (message instanceof Request) {

				objectToInjectSessionId = convertToObject(jsonObject,
						PARAMS_PROPERTY);

			} else {

				Response<?> response = (Response<?>) message;
				if (response.getError() == null) {

					objectToInjectSessionId = convertToObject(jsonObject,
							RESULT_PROPERTY);
				} else {

					objectToInjectSessionId = convertToObject(jsonObject,
							ERROR_PROPERTY, DATA_PROPERTY);
				}
			}

			objectToInjectSessionId.addProperty(
					JsonRpcConstants.SESSION_ID_PROPERTY,
					message.getSessionId());

			return jsonObject.toString();
		} else {
			return JsonUtils.toJson(message);
		}
	}

	private static JsonObject convertToObject(JsonObject jsonObject,
			String... properties) {

		String property = properties[0];

		JsonElement paramsJson = jsonObject.get(property);
		JsonObject paramsAsObject = null;

		if (paramsJson == null) {
			paramsAsObject = new JsonObject();
			jsonObject.add(property, paramsAsObject);
			paramsJson = paramsAsObject;
		}

		if (!paramsJson.isJsonObject()) {
			paramsAsObject = new JsonObject();
			paramsAsObject.add("value", paramsJson);
			jsonObject.add(property, paramsAsObject);
		} else {
			paramsAsObject = (JsonObject) paramsJson;
		}

		if (properties.length > 1) {
			convertToObject(jsonObject,
					Arrays.copyOfRange(properties, 1, properties.length));
		}

		return paramsAsObject;
	}

	public static JsonElement toJsonElement(Object object) {
		return getGson().toJsonTree(object);
	}
}

class JsonRpcResponseDeserializer implements JsonDeserializer<Response<?>> {

	@Override
	public Response<?> deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		if (!(json instanceof JsonObject)) {
			throw new JsonParseException("JonObject expected, found "
					+ json.getClass().getSimpleName());
		}

		JsonObject jObject = (JsonObject) json;

		if (!jObject.has(JSON_RPC_PROPERTY)) {
			throw new JsonParseException(
					"Invalid JsonRpc response lacking version '"
							+ JSON_RPC_PROPERTY + "' field");
		}

		if (!jObject.get(JSON_RPC_PROPERTY).getAsString()
				.equals(JsonRpcConstants.JSON_RPC_VERSION)) {
			throw new JsonParseException("Invalid JsonRpc version");
		}

		Integer id;
		try {
			id = jObject.get(ID_PROPERTY).getAsInt();
		} catch (Exception e) {
			throw new JsonParseException(
					"Invalid JsonRpc response. It lacks a valid '"
							+ ID_PROPERTY + "' field");
		}

		if (jObject.has(RESULT_PROPERTY)) {

			ParameterizedType parameterizedType = (ParameterizedType) typeOfT;

			return new Response<Object>(id, context.deserialize(
					jObject.get(RESULT_PROPERTY),
					parameterizedType.getActualTypeArguments()[0]));

		} else if (jObject.has(ERROR_PROPERTY)) {

			return new Response<Object>(id,
					(ResponseError) context.deserialize(
							jObject.get(ERROR_PROPERTY), ResponseError.class));

		} else {
			throw new JsonParseException("Invalid JsonRpc response lacking '"
					+ RESULT_PROPERTY + "' and '" + ERROR_PROPERTY
					+ "' fields. " + json);
		}

	}
}

class JsonRpcRequestDeserializer implements JsonDeserializer<Request<?>> {

	@Override
	public Request<?> deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		if (!(json instanceof JsonObject)) {
			throw new JsonParseException(
					"Invalid JsonRpc request showning JsonElement type "
							+ json.getClass().getSimpleName());
		}

		JsonObject jObject = (JsonObject) json;

		if (!jObject.has(JSON_RPC_PROPERTY)) {
			throw new JsonParseException(
					"Invalid JsonRpc request lacking version '"
							+ JSON_RPC_PROPERTY + "' field");
		}

		if (!jObject.get("jsonrpc").getAsString().equals(JSON_RPC_VERSION)) {
			throw new JsonParseException("Invalid JsonRpc version");
		}

		if (!jObject.has(METHOD_PROPERTY)) {
			throw new JsonParseException("Invalid JsonRpc request lacking '"
					+ METHOD_PROPERTY + "' field");
		}

		Integer id = null;
		if (jObject.has(ID_PROPERTY)) {
			id = jObject.get(ID_PROPERTY).getAsInt();
		}

		ParameterizedType parameterizedType = (ParameterizedType) typeOfT;

		return new Request<Object>(id, jObject.get(METHOD_PROPERTY)
				.getAsString(), context.deserialize(
				jObject.get(PARAMS_PROPERTY),
				parameterizedType.getActualTypeArguments()[0]));

	}

}

class JsonPropsAdapter implements JsonDeserializer<Props>,
		JsonSerializer<Props> {

	@Override
	public Props deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		if (!(json instanceof JsonObject)) {
			throw new JsonParseException("Cannot convert " + json
					+ " to Props object");
		}

		JsonObject jObject = (JsonObject) json;

		Props props = new Props();
		for (Map.Entry<String, JsonElement> e : jObject.entrySet()) {
			Object value = deserialize(e.getValue(), context);
			props.add(e.getKey(), value);
		}
		return props;
	}

	private Object deserialize(JsonElement value,
			JsonDeserializationContext context) {

		if (value instanceof JsonObject) {
			return deserialize(value, null, context);

		} else if (value instanceof JsonPrimitive) {
			return toPrimitiveObject(value);

		} else if (value instanceof JsonArray) {

			JsonArray array = (JsonArray) value;
			List<Object> result = new ArrayList<Object>();
			for (JsonElement element : array) {
				result.add(deserialize(element, context));
			}
			return result;

		} else {
			throw new RuntimeException("Unrecognized Json element: " + value);
		}
	}

	public Object toPrimitiveObject(JsonElement element) {

		JsonPrimitive primitive = (JsonPrimitive) element;
		if (primitive.isBoolean()) {
			return primitive.getAsBoolean();
		} else if (primitive.isNumber()) {
			Number number = primitive.getAsNumber();
			double value = number.doubleValue();
			if (((int) value == value)) {
				return (int) value;
			} else {
				return (float) value;
			}
		} else if (primitive.isString()) {
			return primitive.getAsString();
		} else {
			throw new RuntimeException("Unrecognized JsonPrimitive: "
					+ primitive);
		}
	}

	@Override
	public JsonElement serialize(Props props, Type typeOfSrc,
			JsonSerializationContext context) {

		JsonObject jsonObject = new JsonObject();
		for (Prop prop : props) {
			jsonObject.add(prop.getName(), context.serialize(prop.getValue()));
		}

		return jsonObject;
	}
}