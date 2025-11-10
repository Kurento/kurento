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
 */

package org.kurento.jsonrpc.client;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.exceptions.UpgradeException;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcClientWebSocket extends AbstractJsonRpcClientWebSocket {

  private static Logger log = LoggerFactory.getLogger(JsonRpcClientWebSocket.class);

  @WebSocket
  public class WebSocketClientSocket {

    @OnWebSocketClose
    public void onClose(int statusCode, String closeReason) {
      log.debug("Websocket disconnected because '{}' (status code {})", closeReason, statusCode);
      handleReconnectDisconnection(statusCode, closeReason);
    }

    @OnWebSocketOpen
    public void onConnect(Session session) {
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
      receivedTextMessage(message);
    }
  }

  protected final SslContextFactory.Client sslContextFactory;

  protected volatile Session jettyWsSession;
  protected volatile WebSocketClient jettyClient;

  public JsonRpcClientWebSocket(String url) {
    this(url, null, new SslContextFactory.Client());
  }

  public JsonRpcClientWebSocket(String url, SslContextFactory.Client sslContextFactory) {
    this(url, null, sslContextFactory);
  }

  public JsonRpcClientWebSocket(String url, JsonRpcWSConnectionListener connectionListener) {
    this(url, connectionListener, new SslContextFactory.Client());
  }

  public JsonRpcClientWebSocket(String url, JsonRpcWSConnectionListener connectionListener,
      SslContextFactory.Client sslContextFactory) {
    super(url, connectionListener);
    this.sslContextFactory = sslContextFactory;
  }

  @Override
  protected void sendTextMessage(String jsonMessage) throws IOException {

    if (jettyWsSession == null) {
      throw new IllegalStateException(
          label + " JsonRpcClient is disconnected from WebSocket server at '" + this.uri + "'");
    }

    synchronized (jettyWsSession) {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<Throwable> failure = new AtomicReference<>();

      jettyWsSession.sendText(jsonMessage, Callback.from(latch::countDown, error -> {
        failure.set(error);
        latch.countDown();
      }));

      try {
        boolean completed = latch.await(this.connectionTimeout, TimeUnit.MILLISECONDS);
        if (!completed) {
          throw new IOException("Timed out waiting for Jetty to transmit message");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while waiting for Jetty to transmit message", e);
      }

      if (failure.get() != null) {
        throw new IOException("Jetty failed to transmit WebSocket message", failure.get());
      }
    }
  }

  @Override
  protected boolean isNativeClientConnected() {
    return jettyWsSession != null && jettyWsSession.isOpen();
  }

  @Override
  protected void connectNativeClient() throws TimeoutException, Exception {

    if (jettyClient == null || jettyClient.isStopped() || jettyClient.isStopping()) {

      log.debug("Connecting JettyWS client with connectionTimeout={} millis",
          this.connectionTimeout);

      // Create HttpClient with SSL configuration
      HttpClient httpClient = new HttpClient();
      httpClient.setSslContextFactory(sslContextFactory);
      httpClient.setConnectTimeout(this.connectionTimeout);

      // Create WebSocketClient with the HttpClient
      jettyClient = new WebSocketClient(httpClient);

      // Configure message buffer sizes directly on WebSocketClient
      jettyClient.setMaxBinaryMessageSize(maxPacketSize);
      jettyClient.setMaxTextMessageSize(maxPacketSize);
      jettyClient.setInputBufferSize(maxPacketSize);
      jettyClient.setConnectTimeout(this.connectionTimeout);

      // Set idle timeout on the WebSocketClient
      jettyClient.setIdleTimeout(Duration.ofMillis(this.idleTimeout));

      jettyClient.start();

    }

    int numRetries = 0;
    int maxRetries = 5;
    while (true) {

      try {

        jettyWsSession = jettyClient.connect(new WebSocketClientSocket(), new ClientUpgradeRequest(uri))
            .get(this.connectionTimeout, TimeUnit.MILLISECONDS);

        return;

      } catch (ExecutionException e) {
        if (e.getCause() instanceof UpgradeException && numRetries < maxRetries) {
          log.warn(
              "Upgrade exception when trying to connect to {}. Try {} of {}. Retrying in 200ms ",
              uri, numRetries + 1, maxRetries);
          Thread.sleep(200);
          numRetries++;
        } else {
          throw e;
        }
      }
    }

  }

  @Override
  public void closeNativeClient() {
    if (jettyClient != null) {
      log.debug("{} Closing client", label);
      try {
        jettyClient.stop();
        jettyClient.destroy();
      } catch (Exception e) {
        log.warn("{} Could not properly close websocket client.", label, e);
      }
      jettyClient = null;
    }

    if (jettyWsSession != null) {
      jettyWsSession.close();
      jettyWsSession = null;
    } else {
      log.warn("{} Trying to close a JsonRpcClientWebSocket with jettyWsSession=null", label);
    }
  }

}
