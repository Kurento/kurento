/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.client.internal.transport.jsonrpc;

import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.CREATE_TYPE;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OBJECT;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OPERATION_NAME;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.INVOKE_OPERATION_PARAMS;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.RELEASE_METHOD;
import static org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants.RELEASE_OBJECT;

import java.io.IOException;

import org.kurento.client.internal.server.ProtocolException;
import org.kurento.client.internal.server.RomServer;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RomServerJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

  private static Logger LOG = LoggerFactory.getLogger(RomServerJsonRpcHandler.class);

  private final RomServer server;

  public RomServerJsonRpcHandler(String packageName, String classSuffix) {
    server = new RomServer(packageName, classSuffix);
  }

  @Override
  public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

    try {

      JsonObject params = request.getParams();
      String method = request.getMethod();
      switch (method) {
        case INVOKE_METHOD:
          String objectRef = getAsString(params, INVOKE_OBJECT, "object reference");

          String operationName = getAsString(params, INVOKE_OPERATION_NAME, "method to be invoked");

          JsonObject operationParams = params.getAsJsonObject(INVOKE_OPERATION_PARAMS);

          handleInvokeCommand(transaction, objectRef, operationName, operationParams);
          break;
        case RELEASE_METHOD:
          String objectReleaseRef = getAsString(params, RELEASE_OBJECT,
              "object reference to be released");

          handleReleaseCommand(transaction, objectReleaseRef);
          break;
        case CREATE_METHOD:
          String type = getAsString(params, CREATE_TYPE, "RemoteClass of the object to be created");

          handleCreateCommand(transaction, type, params.getAsJsonObject(CREATE_CONSTRUCTOR_PARAMS));
          break;
        default:
          LOG.warn("Unknown request method '{}'", method);

      }
    } catch (ProtocolException e) {
      try {
        transaction.sendError(e);
      } catch (IOException ex) {
        LOG.warn("Exception while sending a response", e);
      }
    } catch (IOException e) {
      LOG.warn("Exception while sending a response", e);
    }
  }

  private String getAsString(JsonObject jsonObject, String propName, String propertyDescription) {

    if (jsonObject == null) {
      throw new ProtocolException("There are no params in the request");
    }

    JsonElement element = jsonObject.get(propName);
    if (element == null) {
      throw new ProtocolException(
          "It is necessary a property '" + propName + "' with " + propertyDescription);
    } else {
      return element.getAsString();
    }
  }

  private void handleCreateCommand(Transaction transaction, String type,
      JsonObject constructorParams) throws IOException {

    Object result = server.create(type, JsonUtils.fromJson(constructorParams, Props.class));

    transaction.sendResponse(result);
  }

  private void handleReleaseCommand(Transaction transaction, String objectRef) {
    server.release(objectRef);
  }

  private void handleInvokeCommand(Transaction transaction, String objectRef, String operationName,
      JsonObject operationParams) throws IOException {

    Object result = server.invoke(objectRef, operationName,
        JsonUtils.fromJson(operationParams, Props.class), Object.class);

    transaction.sendResponse(result);
  }

}