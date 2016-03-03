package org.kurento.jsonrpc.client;

import static org.kurento.jsonrpc.JsonUtils.fromJson;
import static org.kurento.jsonrpc.JsonUtils.fromJsonRequest;
import static org.kurento.jsonrpc.JsonUtils.fromJsonResponse;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_CONNECT;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_PING;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.TimeoutReentrantLock;
import org.kurento.commons.TimeoutRuntimeException;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.TransportException;
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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class AbstractJsonRpcClientWebSocket extends JsonRpcClient {

  private static final int CONNECTION_LOCK_TIMEOUT = 25000;

  public static Logger log = LoggerFactory.getLogger(AbstractJsonRpcClientWebSocket.class);

  private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
      .setNameFormat("JsonRpcClientWebsocket-%d").build();

  private long requestTimeout = PropertiesManager.getProperty("jsonRpcClientWebSocket.timeout",
      60000);

  private volatile ExecutorService execService;
  private volatile ExecutorService disconnectExecService;

  protected String url;

  private final PendingRequests pendingRequests = new PendingRequests();
  private ResponseSender rs;

  private JsonRpcWSConnectionListener connectionListener;

  private boolean reconnecting;

  private TimeoutReentrantLock lock;

  private boolean sendCloseMessage;

  private boolean concurrentServerRequest = true;

  public AbstractJsonRpcClientWebSocket(String url,
      JsonRpcWSConnectionListener connectionListener) {

    this.lock = new TimeoutReentrantLock(CONNECTION_LOCK_TIMEOUT, "Server " + url);
    this.url = url;
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

  protected void fireReconnectedNewServer() {
    if (connectionListener != null) {
      createExecServiceIfNecessary();
      execService.submit(new Runnable() {
        @Override
        public void run() {
          connectionListener.reconnected(false);
        }
      });
    }
  }

  protected void fireReconnectedSameServer() {
    if (connectionListener != null) {
      createExecServiceIfNecessary();
      execService.submit(new Runnable() {
        @Override
        public void run() {
          connectionListener.reconnected(true);
        }
      });
    }
  }

  protected void fireConnectionFailed() {
    if (connectionListener != null) {
      createExecServiceIfNecessary();
      execService.submit(new Runnable() {
        @Override
        public void run() {
          connectionListener.connectionFailed();
        }
      });
    }
  }

  protected void createExecServiceIfNecessary() {

    if (execService == null || disconnectExecService == null || execService.isShutdown()
        || execService.isTerminated() || disconnectExecService.isShutdown()
        || disconnectExecService.isTerminated()) {

      lock.tryLockTimeout("createExecServiceIfNecessary");

      try {

        if (execService == null || execService.isShutdown() || execService.isTerminated()) {
          execService = Executors.newCachedThreadPool(threadFactory);
        }

        if (disconnectExecService == null || disconnectExecService.isShutdown()
            || disconnectExecService.isTerminated()) {
          disconnectExecService = Executors.newCachedThreadPool(threadFactory);
        }
      } finally {
        lock.unlock();
      }
    }
  }

  protected <P, R> Response<R> internalSendRequestWebSocket(Request<P> request,
      Class<R> resultClass) throws IOException {

    connectIfNecessary();

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
      throw new KurentoException(label + " Interrupted while waiting for a response", e);
    } catch (ExecutionException e) {
      throw new KurentoException(label + " This exception shouldn't be thrown", e);
    } catch (TimeoutException e) {
      throw new TransportException(label + " Timeout of " + requestTimeout
          + " milliseconds waiting from response to request " + jsonMessage.trim(), e);
    }
  }

  protected <P> void internalSendRequestWebSocket(final Request<P> request,
      final Class<JsonElement> resultClass,
      final Continuation<Response<JsonElement>> continuation) {

    try {

      connectIfNecessary();

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

              Response<JsonElement> response = MessageUtils.convertResponse(responseJson,
                  resultClass);

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
        }, execService);

      }

    } catch (Exception e) {
      continuation.onError(e);
    }
  }

  @Override
  public void close() throws IOException {

    super.close();

    String sessionId = this.session != null ? this.session.getSessionId() : "";
    log.info("{} Explicit close of JsonRpcClientWebsocket with sessionId={}", label, sessionId);

    if (sendCloseMessage) {
      try {
        sendRequest(JsonRpcConstants.METHOD_CLOSE);
      } catch (Exception e) {
        log.warn("{} Exception sending close message. {}:{}", label, e.getClass().getName(),
            e.getMessage());
      }
    }

    pendingRequests.closeAllPendingRequests();

    this.closeClient();

  }

  protected synchronized void closeClient() {

    closeNativeClient();

    if (execService != null) {
      try {
        execService.shutdown();
      } catch (Exception e) {
        log.debug("{} Could not properly shut down executor service. Reason: {}", label,
            e.getMessage());
      }
      execService = null;
    }

    if (disconnectExecService != null) {
      try {
        disconnectExecService.shutdown();
      } catch (Exception e) {
        log.debug("{} Could not properly shut down disconnect executor service. Reason: {}", label,
            e.getMessage());
      }
      disconnectExecService = null;
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

      execService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            handlerManager.handleRequest(session, fromJsonRequest(message, JsonElement.class), rs);
          } catch (IOException e) {
            log.warn("{} Exception processing request {}", label, message, e);
          }
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

    if (!isClosed()) {

      reconnect(closeReason);

    } else {

      pendingRequests.closeAllPendingRequests();

      handlerManager.afterConnectionClosed(session, closeReason);

      if (connectionListener != null) {
        connectionListener.disconnected();
      }
    }
  }

  private void reconnect(final String closeReason) {

    reconnecting = true;

    createExecServiceIfNecessary();

    disconnectExecService.execute(new Runnable() {
      @Override
      public void run() {
        try {

          log.debug("{}JsonRpcWsClient reconnecting to {}", label, url);

          if (connectionListener != null) {
            connectionListener.reconnecting();
          }

          connectIfNecessary();

          reconnecting = false;

        } catch (Exception e) {

          // TODO Implement retries here

          log.warn(
              "{} Exception trying to reconnect to server {}. The websocket was closed due to {}",
              label, url, closeReason, e);

          pendingRequests.closeAllPendingRequests();

          handlerManager.afterConnectionClosed(session, closeReason);

          if (connectionListener != null) {
            connectionListener.disconnected();
          }
        }
      }

    });
  }

  @Override
  protected void closeWithReconnection() {
    log.info("{} Closing websocket session to force reconnection", label);
    closeNativeClient();
    handleReconnectDisconnection(999, "ping timeout");
  }

  @Override
  public void connect() throws IOException {
    this.closed = false;
    connectIfNecessary();
  }

  protected void internalConnectIfNecessary() throws IOException {

    if (isClosed()) {
      throw new KurentoException("Trying to send a message in a client closed explicitly. "
          + "When a client is closed, it can't be reused. It is necessary to create another one");
    }

    if (!isNativeClientConnected()) {

      log.debug("{} Connecting webSocket client to server {}", label, url);

      try {

        connectNativeClient();

      } catch (TimeoutException e) {

        fireConnectionFailed();

        this.closeClient();

        throw new KurentoException(label + " Timeout of " + this.connectionTimeout
            + "ms when waiting to connect to Websocket server " + url);

      } catch (Exception e) {

        fireConnectionFailed();

        this.closeClient();

        throw new KurentoException(label + " Exception connecting to WebSocket server " + url, e);
      }

      initNewSession();
    }
  }

  private void initNewSession() throws IOException {

    configureResponseSender();

    if (connectionListener != null && !reconnecting) {
      connectionListener.connected();
    }

    if (session == null) {
      createJsonRpcSession();
    } else {
      executeConnectProtocol();
    }

    if (heartbeating) {
      enableHeartbeat();
    }
  }

  void executeConnectProtocol() throws IOException {
    try {
      rsHelper.sendRequest(METHOD_CONNECT, String.class);

      log.info("{} Reconnected to the same session in server {}", label, url);

      fireReconnectedSameServer();

    } catch (JsonRpcErrorException e) {

      // Invalid session exception
      if (e.getCode() == 40007) {

        pendingRequests.closeAllPendingRequests();

        try {

          rsHelper.setSessionId(null);
          rsHelper.sendRequest(METHOD_CONNECT, String.class);

          log.info("{} Reconnected to a new session in server {}", label, url);

          fireReconnectedNewServer();

        } catch (Exception e2) {
          closeClient();
          throw new KurentoException(label + " Exception executing reconnect protocol", e2);
        }

      } else {
        closeClient();
        throw new KurentoException(label + " Exception executing reconnect protocol", e);
      }
    }
  }

  void createJsonRpcSession() {
    session = new ClientSession(null, null, this);
    handlerManager.afterConnectionEstablished(session);
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

  protected void connectIfNecessary() throws IOException {

    try {

      lock.tryLockTimeout("connectIfNecessary()");
      try {

        internalConnectIfNecessary();

      } finally {
        lock.unlock();
      }

    } catch (TimeoutRuntimeException e) {

      this.closeClient();
      throw new TimeoutRuntimeException(
          label + " Timeout trying to connect to websocket server " + url, e);
    }
  }

  protected abstract void sendTextMessage(String jsonMessage) throws IOException;

  protected abstract void closeNativeClient();

  protected abstract boolean isNativeClientConnected();

  protected abstract void connectNativeClient() throws TimeoutException, Exception;

}