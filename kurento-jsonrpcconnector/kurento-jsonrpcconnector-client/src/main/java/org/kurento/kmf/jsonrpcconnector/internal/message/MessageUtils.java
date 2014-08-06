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
package org.kurento.kmf.jsonrpcconnector.internal.message;

import static org.kurento.kmf.jsonrpcconnector.JsonUtils.getGson;

import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageUtils {

	private static Logger log = LoggerFactory.getLogger(MessageUtils.class);

	public static <R> Response<R> convertResponse(
			Response<JsonElement> response, Class<R> resultClass) {

		R resultR = convertJsonTo(response.getResult(), resultClass);

		@SuppressWarnings("unchecked")
		Response<R> responseR = (Response<R>) response;

		responseR.setResult(resultR);

		return responseR;
	}

	@SuppressWarnings("unchecked")
	public static <P> Request<P> convertRequest(
			Request<? extends Object> request, Class<P> paramsClass) {

		P paramsP = null;
		Object params = request.getParams();
		if (params != null) {
			if (paramsClass.isAssignableFrom(params.getClass())) {
				paramsP = (P) params;
			} else if (params instanceof JsonElement) {
				paramsP = convertJsonTo((JsonElement) request.getParams(),
						paramsClass);
			} else {
				throw new ClassCastException();
			}
		}

		Request<P> requestP = (Request<P>) request;

		requestP.setParams(paramsP);

		return requestP;
	}

	private static <R> R convertJsonTo(JsonElement resultJsonObject,
			Class<R> resultClass) {

		if (resultJsonObject == null) {
			return null;
		}

		R resultR = null;
		if (resultClass == String.class || resultClass == Boolean.class
				|| resultClass == Character.class
				|| Number.class.isAssignableFrom(resultClass)
				|| resultClass.isPrimitive()) {

			JsonElement value;
			if (resultJsonObject.isJsonObject()) {

				Set<Entry<String, JsonElement>> properties = ((JsonObject) resultJsonObject)
						.entrySet();

				if (properties.size() > 1) {

					Entry<String, JsonElement> prop = properties.iterator()
							.next();

					log.warn(
							"Converting a result with {} properties in a value"
									+ " of type {}. Selecting propoerty '{}'",
							Integer.valueOf(properties.size()), resultClass,
							prop.getKey());

					value = prop.getValue();

				} else if (properties.size() == 1) {
					value = properties.iterator().next().getValue();
				} else {
					value = null;
				}

			} else if (resultJsonObject.isJsonArray()) {
				JsonArray array = (JsonArray) resultJsonObject;
				if (array.size() > 1) {
					log.warn("Converting an array with {} elements in a value "
							+ "of type {}. Selecting first element",
							Integer.valueOf(array.size()), resultClass);

				}

				value = array.get(0);
			} else {
				value = resultJsonObject;
			}

			resultR = getGson().fromJson(value, resultClass);
		} else {
			resultR = getGson().fromJson(resultJsonObject, resultClass);
		}
		return resultR;
	}

}
