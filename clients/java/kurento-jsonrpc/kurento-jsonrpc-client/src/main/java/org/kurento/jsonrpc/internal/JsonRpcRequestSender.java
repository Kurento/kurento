/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.jsonrpc.internal;

import java.io.IOException;

import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface JsonRpcRequestSender {

  <R> R sendRequest(String method, Class<R> resultClass) throws IOException;

  <R> R sendRequest(String method, Object params, Class<R> resultClass) throws IOException;

  JsonElement sendRequest(String method) throws IOException;

  JsonElement sendRequest(String method, Object params) throws IOException;

  Response<JsonElement> sendRequest(Request<JsonObject> request) throws IOException;

  Response<JsonElement> sendRequestHonorId(Request<JsonObject> request) throws IOException;

  void sendNotification(String method, Object params) throws IOException;

  void sendNotification(String method) throws IOException;

  void sendRequest(String method, JsonObject params, Continuation<JsonElement> continuation);

  void sendRequest(Request<JsonObject> request, Continuation<Response<JsonElement>> continuation)
      throws IOException;

  void sendRequestHonorId(Request<JsonObject> request,
      Continuation<Response<JsonElement>> continuation) throws IOException;

  void sendNotification(String method, Object params, Continuation<JsonElement> continuation)
      throws IOException;
}
