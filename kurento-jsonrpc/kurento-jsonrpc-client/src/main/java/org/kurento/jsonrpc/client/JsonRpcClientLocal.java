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

package org.kurento.jsonrpc.client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.kurento.jsonrpc.internal.client.TransactionImpl;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonRpcClientLocal extends JsonRpcClient {

  private static Logger log = LoggerFactory.getLogger(JsonRpcClientLocal.class);

  private JsonRpcHandler<? extends Object> remoteHandler;
  private final JsonRpcHandlerManager remoteHandlerManager = new JsonRpcHandlerManager();

  public <F> JsonRpcClientLocal(JsonRpcHandler<? extends Object> handler) {

    this.remoteHandler = handler;
    this.remoteHandlerManager.setJsonRpcHandler(remoteHandler);

    session = new ClientSession("XXX", null, this);

    rsHelper = new JsonRpcRequestSenderHelper() {
      @Override
      public <P, R> Response<R> internalSendRequest(Request<P> request, Class<R> resultClass)
          throws IOException {
        return localSendRequest(request, resultClass);
      }

      @Override
      protected void internalSendRequest(Request<? extends Object> request,
          Class<JsonElement> resultClass, Continuation<Response<JsonElement>> continuation) {
        Response<JsonElement> result = localSendRequest(request, resultClass);
        if (result != null) {
          continuation.onSuccess(result);
        }
      }
    };
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <R, P> Response<R> localSendRequest(Request<P> request, Class<R> resultClass) {
    // Simulate sending json string for net
    String jsonRequest = request.toString();

    log.debug("--> {}", jsonRequest);

    Request<JsonObject> newRequest = JsonUtils.fromJsonRequest(jsonRequest, JsonObject.class);

    final Response<JsonObject>[] response = new Response[1];

    ClientSession clientSession = new ClientSession(session.getSessionId(), null,
        new JsonRpcRequestSenderHelper() {

      @Override
      protected void internalSendRequest(Request<? extends Object> request,
          Class<JsonElement> clazz, final Continuation<Response<JsonElement>> continuation) {
            handlerManager.handleRequest(session, (Request<JsonElement>) request,
                new ResponseSender() {
                  @Override
                  public void sendResponse(Message message) throws IOException {
                    continuation.onSuccess((Response<JsonElement>) message);
                  }

                  @Override
                  public void sendPingResponse(Message message) throws IOException {
                    sendResponse(message);
                  }
                });
      }

      @Override
      protected <P2, R2> Response<R2> internalSendRequest(Request<P2> request,
          Class<R2> resultClass) throws IOException {

        final Object[] response = new Object[1];

            final CountDownLatch responseLatch = new CountDownLatch(1);

            handlerManager.handleRequest(session, (Request<JsonElement>) request,
                new ResponseSender() {
                  @Override
                  public void sendResponse(Message message) throws IOException {
                    response[0] = message;
                    responseLatch.countDown();
                  }

                  @Override
                  public void sendPingResponse(Message message) throws IOException {
                    sendResponse(message);
                  }
                });

            Response<R2> response2 = (Response<R2>) response[0];

            try {
              responseLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }

            log.debug("<-- {}", response2);

            Object result = response2.getResult();

            if (result == null || resultClass.isAssignableFrom(result.getClass())) {
              return response2;
            } else if (resultClass == JsonElement.class) {
              response2.setResult((R2) JsonUtils.toJsonElement(result));
              return response2;
            } else {
              throw new ClassCastException("Class " + result + " cannot be converted to "
                  + resultClass);
            }
      }
    });

    TransactionImpl t = new TransactionImpl(clientSession, newRequest, new ResponseSender() {

      @Override
      public void sendResponse(Message message) throws IOException {
        response[0] = (Response<JsonObject>) message;
      }

      @Override
      public void sendPingResponse(Message message) throws IOException {
        sendResponse(message);
      }
    });

    try {
      remoteHandler.handleRequest(t, (Request) request);
    } catch (Exception e) {
      ResponseError error = ResponseError.newFromException(e);
      return new Response<>(request.getId(), error);
    }

    if (response[0] != null) {
      // Simulate receiving json string from net
      Response<R> responseObj = (Response<R>) response[0];
      if (responseObj.getId() == null) {
        responseObj.setId(request.getId());
      }
      String jsonResponse = responseObj.toString();

      // log.debug("< {}", jsonResponse);

      Response<R> newResponse = JsonUtils.fromJsonResponse(jsonResponse, resultClass);

      newResponse.setId(request.getId());

      return newResponse;

    }

    return new Response<>(request.getId());

  }

  @Override
  public void close() throws IOException {
    handlerManager.afterConnectionClosed(session, "Client close");

    super.close();
  }

  @Override
  public void setServerRequestHandler(org.kurento.jsonrpc.JsonRpcHandler<?> handler) {
    super.setServerRequestHandler(handler);
    handlerManager.afterConnectionEstablished(session);
    remoteHandlerManager.afterConnectionEstablished(session);
  }

  @Override
  public void connect() throws IOException {

  }

  @Override
  public void setRequestTimeout(long requesTimeout) {
    log.warn("setRequestTimeout(...) method will be ignored");
  }

}
