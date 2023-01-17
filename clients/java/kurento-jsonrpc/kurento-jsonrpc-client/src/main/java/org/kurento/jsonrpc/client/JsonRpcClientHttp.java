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

import static org.kurento.jsonrpc.JsonUtils.fromJsonResponse;
import static org.kurento.jsonrpc.JsonUtils.toJson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.HttpResponseSender;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class JsonRpcClientHttp extends JsonRpcClient {

  private final Logger log = LoggerFactory.getLogger(JsonRpcClient.class);

  private Thread longPoolingThread;
  private String url;

  private HttpResponseSender rs;

  public JsonRpcClientHttp(String url) {
    this.url = url;
    this.rs = new HttpResponseSender();
    this.rsHelper = new JsonRpcRequestSenderHelper() {
      @Override
      public <P, R> Response<R> internalSendRequest(Request<P> request, Class<R> resultClass)
          throws IOException {
        return internalSendRequestHttp(request, resultClass);
      }

      @Override
      protected void internalSendRequest(Request<? extends Object> request,
          Class<JsonElement> class1, Continuation<Response<JsonElement>> continuation) {
        throw new UnsupportedOperationException("Async client int local is unavailable");
      }
    };
  }

  private void updateSession(Response<?> response) {

    String sessionId = response.getSessionId();

    rsHelper.setSessionId(sessionId);

    if (session == null) {
      session = new ClientSession(sessionId, registerInfo, this);

      handlerManager.afterConnectionEstablished(session);

      startPooling();
    }
  }

  private void startPooling() {
    this.longPoolingThread = new Thread() {
      @Override
      public void run() {
        longPooling();
      }
    };

    this.longPoolingThread.start();
  }

  private void longPooling() {

    while (true) {

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log.debug("Long polling thread interrupted", e);
      }

      if (Thread.interrupted()) {
        break;
      }

      try {

        JsonElement requestsListJsonObject = this.sendRequest(Request.POLL_METHOD_NAME,
            rs.getResponseListToSend(), JsonElement.class);

        log.debug("Response from pool: {}", requestsListJsonObject);

        Type collectionType = new TypeToken<List<Request<JsonElement>>>() {
        }.getType();

        List<Request<JsonElement>> requestList = JsonUtils.fromJson(requestsListJsonObject,
            collectionType);

        processServerRequests(requestList);

      } catch (IOException e) {
        // TODO Decide what to do in this case. If the net connection is
        // lost, this will retry indefinitely
        log.error("Exception when waiting for events (long-polling). Retry", e);
      }
    }
  }

  private void processServerRequests(List<Request<JsonElement>> requestList) {
    for (Request<JsonElement> request : requestList) {
      handlerManager.handleRequest(session, request, rs);
    }
  }

  private <P, R> Response<R> internalSendRequestHttp(Request<P> request, Class<R> resultClass)
      throws IOException {

    String resultJson = org.apache.http.client.fluent.Request.Post(url)
        .bodyString(toJson(request), ContentType.APPLICATION_JSON).execute().returnContent()
        .asString();

    if (resultJson == null || resultJson.trim().isEmpty()) {
      return new Response<>(request.getId(), new ResponseError(3,
          "The server send an empty response"));
    }

    Response<R> response = fromJsonResponse(resultJson, resultClass);

    updateSession(response);

    return response;
  }

  @Override
  public void close() {
    if (this.longPoolingThread != null) {
      log.debug("Interrupted!!!");
      this.longPoolingThread.interrupt();
    }
    handlerManager.afterConnectionClosed(session, "Client closed connection");
    session = null;
    try {
      super.close();
    } catch (IOException e) {
      log.error("Exception while executing close from base class JsonRpcClient", e);
    }
  }

  @Override
  public void connect() throws IOException {

    try {

      org.apache.http.client.fluent.Request.Post(url).bodyString("", ContentType.APPLICATION_JSON)
      .execute();

    } catch (ClientProtocolException e) {
      // Silence http connection exception. This indicate that server is
      // reachable and running
    }
  }

  @Override
  public void setRequestTimeout(long requesTimeout) {
    log.warn("setRequestTimeout(...) method will be ignored");
  }

}
