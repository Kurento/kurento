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
package com.kurento.kmf.connector;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonObject;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kms.thrift.api.KmsMediaError;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaParam;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 */
public final class AsyncCallbackProxy implements AsyncMethodCallback<Object> {

	private final Transaction tx;

	@Autowired
	private ApplicationContext ctx;

	public AsyncCallbackProxy(Transaction tx) {
		this.tx = tx;
	}

	@Override
	public void onComplete(Object response) {

		// Si es create, registrar el handler
		// Si es release, liberar el handler
		// En la sessión, al finalizar, liberar todos los no liberados hasta
		// ahora.

		try {
			Method getResult;

			try {
				getResult = response.getClass().getMethod("getResult");
			} catch (NoSuchMethodException e) {
				// TODO Error code
				throw new Error(
						"getResult() method not found in Thrift callback", e);
			} catch (SecurityException e) {
				// TODO Error code
				throw new Error("getResult() security exception", e);
			}

			try {
				Object callbackResult = getResult.invoke(response);

				Object jsonRpcResponse = convertToJsonRpcResponse(callbackResult);

				tx.sendResponse(jsonRpcResponse);

			} catch (Exception e) {
				tx.sendResponse(new KmsMediaError("ERROR", "ERROR", 0, null));
			}

		} catch (IOException e) {
			// TODO Error code
			throw new KurentoMediaFrameworkException(
					"Exception while sending response to client");
		}
	}

	private Object deserializeKmsData(KmsMediaParam mParam) {
		return ctx.getBean("mediaParam", mParam);
	}

	public Object convertToJsonRpcResponse(Object callbackResult) {

		if (callbackResult instanceof KmsMediaParam) {
			return JsonUtils
					.toJsonObject(deserializeKmsData((KmsMediaParam) callbackResult));
		} else if (callbackResult instanceof KmsMediaObjectRef) {

			JsonObject result = JsonUtils.toJsonObject(callbackResult);
			result.remove("objectType");
			result.remove("__isset_bitfield");
			return result;

		} else if (callbackResult instanceof List) {

			@SuppressWarnings("unchecked")
			List<Object> data = (List<Object>) callbackResult;
			List<Object> result = new ArrayList<Object>();
			for (Object element : data) {
				result.add(convertToJsonRpcResponse(element));
			}
			return result;
		}

		return callbackResult;
	}

	@Override
	public void onError(Exception exception) {
		try {
			tx.sendError(exception);
		} catch (IOException e) {
			// TODO Error code
			throw new KurentoMediaFrameworkException(
					"Exception while sending response to client");
		}
	}
}
