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

package org.kurento.jsonrpc.client;

import static org.kurento.jsonrpc.JsonUtils.fromJson;
import static org.kurento.jsonrpc.JsonUtils.fromJsonRequest;
import static org.kurento.jsonrpc.JsonUtils.fromJsonResponse;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_CONNECT;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_PING;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.ThreadFactoryCreator;
import org.kurento.commons.TimeoutReentrantLock;
import org.kurento.commons.TimeoutRuntimeException;
import org.kurento.jsonrpc.JsonRpcClientClosedException;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonRpcException;
import org.kurento.jsonrpc.internal.JsonRpcConstants;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.internal.ws.PendingRequests;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.MessageUtils;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class AbstractJsonRpcClientWebSocket extends JsonRpcClient {

  private static final int CONNECTION_LOCK_TIMEOUT = 25000;

  private static Logger log = LoggerFactory.getLogger(AbstractJsonRpcClientWebSocket.class);

  protected static final long RECONNECT_DELAY_TIME_MILLIS =
      PropertiesManager.getProperty("jsonRpcClientWebSocket.reconnectionDelay", 2000);

  private long requestTimeout =
      PropertiesManager.getProperty("jsonRpcClientWebSocket.timeout", 60000);

  protected static final int maxPacketSize =
      PropertiesManager.getProperty("jsonRpcClientWebSocket.packetSize", 1000000);

  private volatile ExecutorService reqResEventExec;
  private volatile ScheduledExecutorService disconnectExec;

  protected URI uri;

  private final PendingRequests pendingRequests = new PendingRequests();
  private ResponseSender rs;

  private JsonRpcWSConnectionListener connectionListener;
  private Handler connectedHandler;
  private Handler connectionFailedHandler;
  private Handler disconnectedHandler;
  private Handler reconnectingHandler;
  private ReconnectedHandler reconnectedHandler;

  private volatile boolean reconnecting;

  private TimeoutReentrantLock lock;

  private boolean sendCloseMessage;

  private boolean concurrentServerRequest = true;

  private boolean tryReconnectingForever;
  private long tryReconnectingMaxTime;

  private boolean retryingIfTimeoutToConnect;

  private boolean startSessionWhenConnected;

  private long maxTimeReconnecting;

  private Object executorsLock = new Object();

  public AbstractJsonRpcClientWebSocket(String url,
      JsonRpcWSConnectionListener connectionListener) {

    this.lock = new TimeoutReentrantLock(CONNECTION_LOCK_TIMEOUT, "Server " + url);

    try {
      this.uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("The URL received as argument is not a valid URL", e);
    }
    final String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();

    if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
      throw new IllegalArgumentException("Only WS(S) is supported.");
    }

    this.connectionListener = connectionListener;

    rsHelper = new JsonRpcRequestSenderHelper() {
      @Override
      protected void internalSendRequest(Request<? extends Object> request,
          Class<JsonElement> resultClass, Continuation<Response<JsonElement>> continuation) {

        internalSendRequestWebSocket(request, resultClass, continuation);
      }

      @Override
      public <P, R> Response<R> internalSendRequest(Request<P> request, Class<R> resultClass)
          throws IOException {

        return internalSendRequestWebSocket(request, resultClass);
      }
    };

  }

  /**
   * Configures the request timeout in this client. If a request doesn't receive a response before
   * this time (in millis), a TransportException will be thrown.
   */
  @Override
  public void setRequestTimeout(long timeout) {
    this.requestTimeout = timeout;
  }

  public long getRequestTimeout() {
    return requestTimeout;
  }

  /**
   * Configures if this client should send a close message to server when close() method is invoked.
   * This close message is used to inform the server that client explicitly closed the connection.
   *
   * By default sendCloseMessage is false.
   *
   * @param sendCloseMessage
   */
  public void setSendCloseMessage(boolean sendCloseMessage) {
    this.sendCloseMessage = sendCloseMessage;
  }

  public boolean isSendCloseMessage() {
    return sendCloseMessage;
  }

  public void setTryReconnectingForever(boolean tryReconnectingForever) {
    this.tryReconnectingForever = tryReconnectingForever;
  }

  public boolean isTryReconnectingForever() {
    return tryReconnectingForever;
  }

  /**
   * Configures how requests from server have to be processed. If concurrentServerRequest is true,
   * then a executor service with several threads is used to execute the handler of the request. If
   * concurrentServerRequest is false, the websocket library thread is used to execute the handler.
   * In the current implementation (using Jetty as websocket client), this means that handler is
   * executed sequentially. That is problematic if a synchronous request in sent to server in a
   * handler because a deadlock is produced.
   *
   * By default, concurrentServerRequest is true.
   *
   * @param concurrentServerRequest
   */
  public void setConcurrentServerRequest(boolean concurrentServerRequest) {
    this.concurrentServerRequest = concurrentServerRequest;
  }

  public boolean isConcurrentServerRequest() {
    return concurrentServerRequest;
  }

  private void fireEvent(Runnable r) {
    createExecServiceIfNecessary();
    reqResEventExec.submit(r);
  }

  protected void fireReconnectedNewServer() {
    if (connectionListener != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectionListener.reconnected(false);
        }
      });
    }

    if (reconnectedHandler != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          reconnectedHandler.run(false);
        }
      });
    }
  }

  protected void fireReconnectedSameServer() {
    if (connectionListener != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectionListener.reconnected(true);
        }
      });
    }

    if (reconnectedHandler != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          reconnectedHandler.run(true);
        }
      });
    }
  }

  protected void fireConnectionFailed() {
    if (connectionListener != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectionListener.connectionFailed();
        }
      });
    }

    if (connectionFailedHandler != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectionFailedHandler.run();
        }
      });
    }
  }

  protected void fireConnected() {
    if (connectionListener != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectionListener.connected();
        }
      });
    }
    if (connectedHandler != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectedHandler.run();
        }
      });
    }
  }

  protected void fireReconnecting() {
    if (connectionListener != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectionListener.reconnecting();
        }
      });
    }
    if (reconnectingHandler != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          reconnectingHandler.run();
        }
      });
    }
  }

  protected void fireDisconnected() {
    if (connectionListener != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          connectionListener.disconnected();
        }
      });
    }
    if (disconnectedHandler != null) {
      fireEvent(new Runnable() {
        @Override
        public void run() {
          disconnectedHandler.run();
        }
      });
    }
  }

  protected void createExecServiceIfNecessary() {

    if (reqResEventExec == null || disconnectExec == null || reqResEventExec.isShutdown()
        || reqResEventExec.isTerminated() || disconnectExec.isShutdown()
        || disconnectExec.isTerminated()) {

      synchronized (executorsLock) {

        if (reqResEventExec == null || reqResEventExec.isShutdown()
            || reqResEventExec.isTerminated()) {
          reqResEventExec = Executors.newCachedThreadPool(
              ThreadFactoryCreator.create("AbstractJsonRpcClientWebSocket-reqResEventExec"));
        }

        if (disconnectExec == null || disconnectExec.isShutdown()
            || disconnectExec.isTerminated()) {
          disconnectExec = Executors.newScheduledThreadPool(1,
              ThreadFactoryCreator.create("AbstractJsonRpcClientWebSocket-disconnectExec"));
        }
      }
    }
  }

  protected <P, R> Response<R> internalSendRequestWebSocket(Request<P> request,
      Class<R> resultClass) throws IOException {

    connectIfNecessary(false);

    Future<Response<JsonElement>> responseFuture = null;

    if (request.getId() != null) {
      responseFuture = pendingRequests.prepareResponse(request.getId());
    }

    boolean isPing = false;
    String jsonMessage = request.toString();
    if (METHOD_PING.equals(request.getMethod())) {
      isPing = true;
      log.trace("{} Req-> {}", label, jsonMessage.trim());
    } else {
      log.debug("{} Req-> {}", label, jsonMessage.trim());
    }

    sendTextMessage(jsonMessage);

    if (responseFuture == null) {
      return null;
    }

    Response<JsonElement> responseJson;
    try {
      responseJson = responseFuture.get(requestTimeout, TimeUnit.MILLISECONDS);

      if (isPing) {
        log.trace("{} <-Res {}", label, responseJson.toString());
      } else {
        log.debug("{} <-Res {}", label, responseJson.toString());
      }

      Response<R> response = MessageUtils.convertResponse(responseJson, resultClass);

      if (response.getSessionId() != null) {
        session.setSessionId(response.getSessionId());
      }

      return response;

    } catch (InterruptedException e) {
      throw new JsonRpcException(label + " Interrupted while waiting for a response", e);
    } catch (ExecutionException e) {
      throw new JsonRpcException(label + " This exception shouldn't be thrown", e);
    } catch (TimeoutException e) {
      throw new JsonRpcException(label + " Timeout of " + requestTimeout
          + " milliseconds waiting from response to request " + jsonMessage.trim(), e);
    }
  }

  protected <P> void internalSendRequestWebSocket(final Request<P> request,
      final Class<JsonElement> resultClass,
      final Continuation<Response<JsonElement>> continuation) {

    try {

      connectIfNecessary(false);

      ListenableFuture<Response<JsonElement>> responseFuture = null;

      if (request.getId() != null) {
        responseFuture = pendingRequests.prepareResponse(request.getId());
      }

      final boolean isPing;
      String jsonMessage = request.toString();
      if (METHOD_PING.equals(request.getMethod())) {
        isPing = true;
        log.trace("{} Req-> {}", label, jsonMessage.trim());
      } else {
        isPing = false;
        log.debug("{} Req-> {}", label, jsonMessage.trim());
      }

      sendTextMessage(jsonMessage);

      if (responseFuture != null) {

        createExecServiceIfNecessary();

        Futures.addCallback(responseFuture, new FutureCallback<Response<JsonElement>>() {
          @Override
          public void onSuccess(Response<JsonElement> responseJson) {

            if (isPing) {
              log.trace("{} <-Res {}", label, responseJson.toString());
            } else {
              log.debug("{} <-Res {}", label, responseJson.toString());
            }

            try {

              Response<JsonElement> response =
                  MessageUtils.convertResponse(responseJson, resultClass);

              if (response.getSessionId() != null) {
                session.setSessionId(response.getSessionId());
              }

              continuation.onSuccess(response);

            } catch (Exception e) {
              continuation.onError(e);
            }
          }

          @Override
          public void onFailure(Throwable thrown) {
            continuation.onError(thrown);
          }
        }, reqResEventExec);

      }

    } catch (Exception e) {
      continuation.onError(e);
    }
  }

  @Override
  public void close() throws IOException {

    super.close();

    String sessionId = this.session != null ? this.session.getSessionId() : "";
    log.debug("{} Explicit close of JsonRpcClientWebsocket with sessionId={}", label, sessionId);

    if (sendCloseMessage) {
      try {
        sendRequest(JsonRpcConstants.METHOD_CLOSE);
      } catch (Exception e) {
        log.warn("{} Exception sending close message. {}:{}", label, e.getClass().getName(),
            e.getMessage());
      }
    }

    reconnecting = false;

    this.closeClient("Session closed by JsonRpcClientWebsocket user", true);

  }

  protected synchronized void closeClient(String reason, boolean shutdownReconnectThread) {

    if (!reconnecting) {
      notifyDisconnection(reason, false);
    }

    closeNativeClient();

    if (reqResEventExec != null) {
      try {
        reqResEventExec.shutdown();
      } catch (Exception e) {
        log.debug("{} Could not properly shut down executor service. Reason: {}", label,
            e.getMessage());
      }
      reqResEventExec = null;
    }

    if (disconnectExec != null && shutdownReconnectThread) {
      try {
        disconnectExec.shutdownNow();
      } catch (Exception e) {
        log.debug("{} Could not properly shut down disconnect executor service. Reason: {}", label,
            e.getMessage());
      }
      disconnectExec = null;
    }

    if (heartbeating) {
      disableHeartbeat();
    }
  }

  private void notifyDisconnection(String reason, boolean connectedBefore) {
    if (isClosedByUser() || connectedBefore) {
      fireDisconnected();
    } else {
      fireConnectionFailed();
    }

    pendingRequests.closeAllPendingRequests();

    if (session != null) {
      handlerManager.afterConnectionClosed(session, reason);
    }
  }

  protected void handleResponseFromServer(JsonObject message) {

    Response<JsonElement> response = fromJsonResponse(message, JsonElement.class);

    setSessionId(response.getSessionId());

    pendingRequests.handleResponse(response);
  }

  protected void receivedTextMessage(String message) {

    try {

      JsonObject jsonMessage = fromJson(message, JsonObject.class);

      if (jsonMessage.has(JsonRpcConstants.METHOD_PROPERTY)) {
        handleRequestFromServer(jsonMessage);
      } else {
        handleResponseFromServer(jsonMessage);
      }

    } catch (Exception e) {
      log.error("{} Exception processing jsonRpc message {}", label, message, e);
    }
  }

  void handleRequestFromServer(final JsonObject message) {

    if (concurrentServerRequest) {

      createExecServiceIfNecessary();

      reqResEventExec.submit(new Runnable() {
        @Override
        public void run() {
          handlerManager.handleRequest(session, fromJsonRequest(message, JsonElement.class), rs);
        }
      });

    } else {

      try {
        handlerManager.handleRequest(session, fromJsonRequest(message, JsonElement.class), rs);
      } catch (Exception e) {
        log.warn("{} Exception processing request {}", label, message, e);
      }
    }
  }

  protected void handleReconnectDisconnection(final int statusCode, final String closeReason) {

    if (!isClosedByUser()) {

      log.debug("{}JsonRpcWsClient disconnected from {} because {}.", label, uri, closeReason);

      reconnect(closeReason);

    } else {

      pendingRequests.closeAllPendingRequests();

      handlerManager.afterConnectionClosed(session, closeReason);

      fireDisconnected();
    }
  }

  private void reconnect(final String closeReason) {
    reconnect(closeReason, 0, true);
  }

  private void reconnect(final String closeReason, final long delayMillis,
      boolean fireReconnecting) {

    reconnecting = true;

    if (fireReconnecting) {
      fireReconnecting();
    }

    if (heartbeating) {
      disableHeartbeat();
    }

    createExecServiceIfNecessary();

    disconnectExec.schedule(new Runnable() {
      @Override
      public void run() {
        try {

          log.debug("{} JsonRpcWsClient reconnecting to {}. ", label, uri);

          connectIfNecessary(true);

          reconnecting = false;

        } catch (Exception e) {

          log.debug("tryReconnectingForever = {}", tryReconnectingForever);
          log.debug("tryReconnectingMaxTime = {}", tryReconnectingMaxTime);
          log.debug("maxTimeReconnecting = {}", maxTimeReconnecting);
          log.debug("currentTime = {}", System.currentTimeMillis());
          log.debug("Stop connection retries: {}", System.currentTimeMillis() > maxTimeReconnecting);

          if (!tryReconnectingForever && (tryReconnectingMaxTime == 0
              || System.currentTimeMillis() > maxTimeReconnecting)) {

            log.warn("{} Exception trying to reconnect to server {}. Notifying disconnection",
                label, uri, e);

            notifyDisconnection(closeReason, true);

          } else {

            log.warn("{} Exception trying to reconnect to server {}. Retrying in {} ms", label,
                uri, RECONNECT_DELAY_TIME_MILLIS, e);

            reconnect(closeReason, RECONNECT_DELAY_TIME_MILLIS, false);
          }
        }
      }

    }, delayMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  protected void closeWithReconnection() {
    log.debug("{} Closing websocket session to force reconnection", label);
    closeNativeClient();
    handleReconnectDisconnection(999, "ping timeout");
  }

  @Override
  public void connect() throws IOException {
    this.closedByClient = false;
    connectIfNecessary(true);
  }

  public void connectWithSession() throws IOException {

    this.startSessionWhenConnected = true;

    this.closedByClient = false;

    connectIfNecessary(true);

    log.debug("{} Connected to server with session {}", label, getSession().getSessionId());

  }

  protected void internalConnectIfNecessary(boolean shutdownReconnectThread) throws IOException {

    if (!isNativeClientConnected()) {

      if (!reconnecting) {
        updateMaxTimeReconnecting();
      }

      if (isClosedByUser()) {
        throw new JsonRpcClientClosedException(
            "Trying to send a message in a client closed explicitly. "
                + "When a client is closed, it can't be reused. It is necessary to create another one");
      }

      log.debug("{} Connecting webSocket client to server {}", label, uri);

      try {

        connectNativeClient();

      } catch (Exception e) {

        String exceptionMessage;

        if (e instanceof TimeoutException) {

          exceptionMessage = label + " Timeout of " + this.connectionTimeout
              + "ms when waiting to connect to Websocket server " + uri;

          if (retryingIfTimeoutToConnect) {

            log.debug(exceptionMessage + ". Retrying...");

            internalConnectIfNecessary(shutdownReconnectThread);
          }

        } else {
          exceptionMessage = label + " Exception connecting to WebSocket server " + uri;
        }

        this.closeClient("Closed by exception: " + exceptionMessage, shutdownReconnectThread);

        throw new JsonRpcException(exceptionMessage, e);

      }

      updateSession();
    }
  }

  private void updateMaxTimeReconnecting() {

    if (tryReconnectingForever) {
      maxTimeReconnecting = Long.MAX_VALUE;
    } else if (tryReconnectingMaxTime <= 0) {
      maxTimeReconnecting = 0;
    } else {
      maxTimeReconnecting = System.currentTimeMillis() + tryReconnectingMaxTime;
    }
  }

  private void updateSession() throws IOException {

    if (session == null) {
      session = new ClientSession(null, null, this);
      configureResponseSender();
    }

    if (reconnecting) {

      boolean sameServer = executeConnectProtocol();

      if (sameServer) {
        fireReconnectedSameServer();
      } else {
        fireReconnectedNewServer();
      }

    } else {

      if (startSessionWhenConnected) {
        rsHelper.sendRequest(METHOD_CONNECT, String.class);
      }

      handlerManager.afterConnectionEstablished(session);
      fireConnected();
    }

    if (heartbeating) {
      enableHeartbeat();
    }
  }

  boolean executeConnectProtocol() throws IOException {
    try {
      rsHelper.sendRequest(METHOD_CONNECT, String.class);

      log.debug("{} Reconnected to the same session in server {}", label, uri);

      return true;

    } catch (JsonRpcErrorException e) {

      // Invalid session exception
      if (e.getCode() == 40007) {

        pendingRequests.closeAllPendingRequests();

        try {

          rsHelper.setSessionId(null);
          rsHelper.sendRequest(METHOD_CONNECT, String.class);

          log.debug("{} Reconnected to a new session in server {}", label, uri);

          return false;

        } catch (Exception e2) {
          closeClient("Closed by exception: " + e.getMessage(), true);
          throw new JsonRpcException(label + " Exception executing reconnect protocol", e2);
        }

      } else {
        closeClient("Closed by exception: " + e.getMessage(), true);
        throw new JsonRpcException(label + " Exception executing reconnect protocol", e);
      }
    }
  }

  void configureResponseSender() {
    rs = new ResponseSender() {
      @Override
      public void sendResponse(Message message) throws IOException {
        String jsonMessage = message.toString();
        log.debug("{} <-Res {}", label, jsonMessage);
        sendTextMessage(jsonMessage);
      }

      @Override
      public void sendPingResponse(Message message) throws IOException {
        String jsonMessage = message.toString();
        log.trace("{} <-Res {}", label, jsonMessage);
        sendTextMessage(jsonMessage);
      }
    };
  }

  protected void connectIfNecessary(boolean shutdownReconnectThread) throws IOException {

    try {

      lock.tryLockTimeout("connectIfNecessary()");
      try {

      internalConnectIfNecessary(shutdownReconnectThread);

      } finally {
        lock.unlock();
      }

    } catch (TimeoutRuntimeException e) {

      this.closeClient("Closed by exception: " + e.getMessage(), shutdownReconnectThread);

      throw new TimeoutRuntimeException(
          label + " Timeout trying to connect to websocket server " + uri, e);
    }
  }

  public void onConnected(Handler connectedHandler) {
    this.connectedHandler = connectedHandler;
  }

  public void onConnectionFailed(Handler connectionFailedHandler) {
    this.connectionFailedHandler = connectionFailedHandler;
  }

  public void onDisconnected(Handler disconnectedHandler) {
    this.disconnectedHandler = disconnectedHandler;
  }

  public void onReconnecting(Handler reconnectingHandler) {
    this.reconnectingHandler = reconnectingHandler;
  }

  public void onReconnected(ReconnectedHandler reconnectedHandler) {
    this.reconnectedHandler = reconnectedHandler;
  }

  public void setTryReconnectingMaxTime(long tryReconnectingMaxTime) {
    this.tryReconnectingForever = false;
    this.tryReconnectingMaxTime = tryReconnectingMaxTime;
  }

  protected abstract void sendTextMessage(String jsonMessage) throws IOException;

  protected abstract void closeNativeClient();

  protected abstract boolean isNativeClientConnected();

  protected abstract void connectNativeClient() throws TimeoutException, Exception;

}
