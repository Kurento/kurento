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

package org.kurento.jsonrpc;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public abstract class TypeDefaultJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

  private final Logger log = LoggerFactory.getLogger(TypeDefaultJsonRpcHandler.class);
  private final Map<String, Method> methods = new ConcurrentHashMap<>();

  private static final JsonRpcAndJavaMethodManager methodManager = new JsonRpcAndJavaMethodManager();

  public TypeDefaultJsonRpcHandler() {
    Method[] methodsArray = this.getClass().getMethods();
    for (Method method : methodsArray) {
      if (method.isAnnotationPresent(JsonRpcMethod.class)) {
        methods.put(method.getName(), method);
      }
    }
  }

  @Override
  public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

    try {
      Method method = methods.get(request.getMethod());
      if (method == null) {
        log.error("Requesting unrecognized method '{}'", request.getMethod());
        transaction.sendError(1, "UNRECOGNIZED_METHOD",
            "Unrecognized method '" + request.getMethod() + "'", null);
      } else {
        methodManager.executeMethod(method, this, transaction, request);
      }

    } catch (Exception e) {
      log.error("Exception processing request {}", request, e);
      transaction.sendError(e);
    }
  }
}
