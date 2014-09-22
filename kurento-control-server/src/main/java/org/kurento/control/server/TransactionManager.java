package org.kurento.control.server;

import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OPERATION_PARAMS;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.RELEASE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.RELEASE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.SUBSCRIBE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.SUBSCRIBE_OBJECT;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.kurento.client.internal.transport.jsonrpc.JsonResponseUtils;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class TransactionManager {

	private int numRequest = 0;
	private Map<Integer, String> newObjectRefsByReqNum = new HashMap<Integer, String>();
	private boolean lastRequestIsCreate = false;

	public void updateRequest(Request<JsonObject> request) {

		JsonObject params = request.getParams();

		switch (request.getMethod()) {
		case CREATE_METHOD:
			updateRecursive(params, CREATE_CONSTRUCTOR_PARAMS);
			lastRequestIsCreate = true;
			break;
		case INVOKE_METHOD:
			updateRecursive(params, INVOKE_OPERATION_PARAMS);
			updateSimple(params, INVOKE_OBJECT);
			lastRequestIsCreate = false;
			break;
		case RELEASE_METHOD:
			updateSimple(params, RELEASE_OBJECT);
			lastRequestIsCreate = false;
			break;
		case SUBSCRIBE_METHOD:
			updateSimple(params, SUBSCRIBE_OBJECT);
			lastRequestIsCreate = false;
			break;
		}

	}

	public void updateResponse(Response<JsonElement> response) {
		if (lastRequestIsCreate) {
			newObjectRefsByReqNum.put(
					numRequest,
					JsonResponseUtils.<String> convertFromResult(
							response.getResult(), String.class));
		}
		numRequest++;
	}

	private void updateSimple(JsonObject params, String propertyName) {
		String param = params.get(propertyName).getAsString();
		if (isNewRef(param)) {
			String ref = convertValue(param);
			params.add(propertyName, new JsonPrimitive(ref));
		}
	}

	private boolean isNewRef(String param) {
		return param.startsWith("newref:");
	}

	private String convertValue(String param) {
		int numReq = Integer.parseInt(param.substring(7));
		return newObjectRefsByReqNum.get(numReq);
	}

	private void updateRecursive(JsonObject params, String prop) {

		// TODO This implementation assumes that all values starting with
		// 'newref:0' are referencies to objects created in the same tx. But
		// 'newref' is a valid value for a String param. We need to find a good
		// way to mark all reference params.

		JsonElement propValue = params.get(prop);
		if (propValue instanceof JsonPrimitive) {
			JsonPrimitive value = (JsonPrimitive) propValue;
			if (value.isString()) {
				updateSimple(params, prop);
			}
		} else {
			updateRecursiveComplex(propValue);
		}
	}

	private void updateRecursive(JsonObject params) {
		for (Entry<String, JsonElement> prop : params.entrySet()) {
			updateRecursive(params, prop.getKey());
		}
	}

	private void updateRecursiveComplex(JsonElement propValue) {

		if (propValue instanceof JsonObject) {
			updateRecursive((JsonObject) propValue);

		} else if (propValue instanceof JsonArray) {
			JsonArray array = (JsonArray) propValue;
			for (int i = 0; i < array.size(); i++) {

				JsonElement arrayValue = array.get(i);
				if (arrayValue instanceof JsonPrimitive) {
					JsonPrimitive value = (JsonPrimitive) arrayValue;
					if (value.isString()) {
						String sValue = value.getAsString();
						if (isNewRef(sValue)) {
							String ref = convertValue(sValue);
							array.set(i, new JsonPrimitive(ref));
						}
					}
				} else {
					updateRecursiveComplex(propValue);
				}
			}
		}
	}

}
