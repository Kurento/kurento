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
package com.kurento.kmf.content.jsonrpc;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kurento.kmf.content.jsonrpc.param.JsonRpcRequestParams;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcResponseResult;

/**
 * 
 * Gson/JSON utilities; used to serialize Java object to JSON (as String).
 * 
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @version 1.0.0
 */
public class GsonUtils {

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
	public static String toString(Object obj) {
		return GsonUtils.getGson().toJson(obj);
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
		builder.registerTypeAdapter(JsonRpcRequest.class,
				new JsonRpcRequestDeserializer());

		builder.registerTypeAdapter(JsonRpcResponse.class,
				new JsonRpcResponseDeserializer());

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
}

class JsonRpcResponseDeserializer implements JsonDeserializer<JsonRpcResponse> {

	@Override
	public JsonRpcResponse deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		if (!(json instanceof JsonObject)) {
			throw new JsonParseException("JonObject expected, found "
					+ json.getClass().getSimpleName());
		}

		JsonObject jObject = (JsonObject) json;

		if (!jObject.has("jsonrpc")) {
			throw new JsonParseException(
					"Invalid JsonRpc response lacking version 'jsonrpc' field");
		}

		if (!jObject.get("jsonrpc").getAsString()
				.equals(JsonRpcConstants.JSON_RPC_VERSION)) {
			throw new JsonParseException("Invalid JsonRpc version");
		}

		if (!jObject.has("id")) {
			throw new JsonParseException(
					"Invalid JsonRpc response lacking 'id' field");
		}

		if (jObject.has("result")) {
			return new JsonRpcResponse(
					(JsonRpcResponseResult) context.deserialize(
							jObject.get("result").getAsJsonObject(),
							JsonRpcResponseResult.class), jObject.get("id")
							.getAsInt());
		} else if (jObject.has("error")) {
			return new JsonRpcResponse(
					(JsonRpcResponseError) context.deserialize(
							jObject.get("error").getAsJsonObject(),
							JsonRpcResponseError.class), jObject.get("id")
							.getAsInt());
		} else {
			throw new JsonParseException(
					"Invalid JsonRpc response lacking 'result' and 'error' fields");
		}

	}
}

class JsonRpcRequestDeserializer implements JsonDeserializer<JsonRpcRequest> {

	@Override
	public JsonRpcRequest deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		if (!(json instanceof JsonObject)) {
			throw new JsonParseException(
					"Invalid JsonRpc request showning JsonElement type "
							+ json.getClass().getSimpleName());
		}

		JsonObject jObject = (JsonObject) json;

		if (!jObject.has("jsonrpc")) {
			throw new JsonParseException(
					"Invalid JsonRpc request lacking version 'jsonrpc' field");
		}

		if (!jObject.get("jsonrpc").getAsString()
				.equals(JsonRpcConstants.JSON_RPC_VERSION)) {
			throw new JsonParseException("Invalid JsonRpc version");
		}

		if (!jObject.has("method")) {
			throw new JsonParseException(
					"Invalid JsonRpc request lacking 'method' field");
		}

		if (!jObject.has("params")) {
			throw new JsonParseException(
					"Invalid JsonRpc request lacking 'params' field");
		}

		if (!jObject.has("id")) {
			throw new JsonParseException(
					"Invalid JsonRpc request lacking 'id' field");
		}

		return new JsonRpcRequest(jObject.get("method").getAsString(),
				(JsonRpcRequestParams) context.deserialize(jObject
						.get("params").getAsJsonObject(),
						JsonRpcRequestParams.class), jObject.get("id")
						.getAsInt());

	}

}