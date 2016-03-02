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
