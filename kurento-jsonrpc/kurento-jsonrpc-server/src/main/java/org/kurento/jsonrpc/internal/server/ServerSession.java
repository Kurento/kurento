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

package org.kurento.jsonrpc.internal.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.ThreadFactoryCreator;
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.AbstractSession;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class ServerSession extends AbstractSession {

  public static final String SESSION_RECONNECTION_TIME_PROP = "ws.sessionReconnectionTime";
  private static final int SESSION_RECONNECTION_TIME_DEFAULT = 10;

  private final SessionsManager sessionsManager;
  private JsonRpcRequestSenderHelper rsHelper;
  private String transportId;
  private ScheduledFuture<?> closeTimerTask;
  private ExecutorService sessionExecutor;

  private volatile ConcurrentMap<String, Object> attributes;

  private long reconnectionTimeoutInMillis = PropertiesManager.getProperty(
      SESSION_RECONNECTION_TIME_PROP, SESSION_RECONNECTION_TIME_DEFAULT) * 1000;
  private boolean gracefullyClosed;

  public ServerSession(String sessionId, Object registerInfo, SessionsManager sessionsManager,
      String transportId) {

    super(sessionId, registerInfo);

    this.transportId = transportId;
    this.sessionsManager = sessionsManager;

    this.sessionExecutor = Executors.newSingleThreadExecutor(ThreadFactoryCreator
        .create("SessionHandler-" + sessionId));
  }

  public abstract void handleResponse(Response<JsonElement> response);

  public String getTransportId() {
    return transportId;
  }

  public void setTransportId(String transportId) {
    this.transportId = transportId;
  }

  @Override
  public void close() throws IOException {
    this.sessionsManager.remove(this.getSessionId());
    this.sessionExecutor.shutdownNow();
  }

  protected void setRsHelper(JsonRpcRequestSenderHelper rsHelper) {
    this.rsHelper = rsHelper;
  }

  @Override
  public <R> R sendRequest(String method, Class<R> resultClass) throws IOException {
    return rsHelper.sendRequest(method, resultClass);
  }

  @Override
  public <R> R sendRequest(String method, Object params, Class<R> resultClass) throws IOException {
    return rsHelper.sendRequest(method, params, resultClass);
  }

  @Override
  public JsonElement sendRequest(String method) throws IOException {
    return rsHelper.sendRequest(method);
  }

  @Override
  public JsonElement sendRequest(String method, Object params) throws IOException {
    return rsHelper.sendRequest(method, params);
  }

  @Override
  public void sendRequest(String method, JsonObject params, Continuation<JsonElement> continuation) {
    rsHelper.sendRequest(method, params, continuation);
  }

  @Override
  public void sendNotification(String method, Object params, Continuation<JsonElement> continuation)
      throws IOException {
    rsHelper.sendNotification(method, params, continuation);
  }

  @Override
  public void sendNotification(String method, Object params) throws IOException {
    rsHelper.sendNotification(method, params);
  }

  @Override
  public void sendNotification(String method) throws IOException {
    rsHelper.sendNotification(method);
  }

  @Override
  public Response<JsonElement> sendRequest(Request<JsonObject> request) throws IOException {
    return rsHelper.sendRequest(request);
  }

  @Override
  public void sendRequest(Request<JsonObject> request,
      Continuation<Response<JsonElement>> continuation) throws IOException {
    rsHelper.sendRequest(request, continuation);
  }

  @Override
  public void sendRequestHonorId(Request<JsonObject> request,
      Continuation<Response<JsonElement>> continuation) throws IOException {
    rsHelper.sendRequestHonorId(request, continuation);
  }

  @Override
  public Response<JsonElement> sendRequestHonorId(Request<JsonObject> request) throws IOException {
    return rsHelper.sendRequestHonorId(request);
  }

  public void setCloseTimerTask(ScheduledFuture<?> closeTimerTask) {
    this.closeTimerTask = closeTimerTask;
  }

  public void setGracefullyClosed() {
    this.gracefullyClosed = true;
  }

  public boolean isGracefullyClosed() {
    return gracefullyClosed;
  }

  public ScheduledFuture<?> getCloseTimerTask() {
    return closeTimerTask;
  }

  @Override
  public void setReconnectionTimeout(long reconnectionTimeoutInMillis) {
    this.reconnectionTimeoutInMillis = reconnectionTimeoutInMillis;
  }

  public long getReconnectionTimeoutInMillis() {
    return reconnectionTimeoutInMillis;
  }

  @Override
  public Map<String, Object> getAttributes() {
    if (attributes == null) {
      synchronized (this) {
        if (attributes == null) {
          attributes = new ConcurrentHashMap<>();
        }
      }
    }

    return attributes;
  }

  public abstract void closeNativeSession(String reason);

  public void processRequest(Runnable task) {
    sessionExecutor.execute(task);
  }
}
