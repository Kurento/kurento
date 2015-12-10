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
package org.kurento.jsonrpc.internal;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

public interface JsonRpcRequestSender {

	<R> R sendRequest(String method, Class<R> resultClass) throws IOException;

	<R> R sendRequest(String method, Object params, Class<R> resultClass)
			throws IOException;

	JsonElement sendRequest(String method) throws IOException;

	JsonElement sendRequest(String method, Object params) throws IOException;

	Response<JsonElement> sendRequest(Request<JsonObject> request)
			throws IOException;
	
	Response<JsonElement> sendRequestHonorId(Request<JsonObject> request)
			throws IOException;

	void sendNotification(String method, Object params) throws IOException;

	void sendNotification(String method) throws IOException;

	void sendRequest(String method, JsonObject params,
			Continuation<JsonElement> continuation);

	void sendRequest(Request<JsonObject> request,
			Continuation<Response<JsonElement>> continuation)
			throws IOException;
	
	void sendRequestHonorId(Request<JsonObject> request, 
			Continuation<Response<JsonElement>> continuation)
			throws IOException;

	void sendNotification(String method, Object params,
			Continuation<JsonElement> continuation) throws IOException;
}
