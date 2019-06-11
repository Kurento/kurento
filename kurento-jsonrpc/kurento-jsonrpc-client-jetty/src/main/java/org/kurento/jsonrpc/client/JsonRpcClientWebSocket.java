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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
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

    @OnWebSocketConnect
    public void onConnect(Session session) {
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
      receivedTextMessage(message);
    }
  }

  protected final SslContextFactory sslContextFactory;

  protected volatile Session jettyWsSession;
  protected volatile WebSocketClient jettyClient;

  public JsonRpcClientWebSocket(String url) {
    this(url, null, new SslContextFactory());
  }

  public JsonRpcClientWebSocket(String url, SslContextFactory sslContextFactory) {
    this(url, null, sslContextFactory);
  }

  public JsonRpcClientWebSocket(String url, JsonRpcWSConnectionListener connectionListener) {
    this(url, connectionListener, new SslContextFactory());
  }

  public JsonRpcClientWebSocket(String url, JsonRpcWSConnectionListener connectionListener,
      SslContextFactory sslContextFactory) {
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
      jettyWsSession.getRemote().sendString(jsonMessage);
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

      jettyClient = new WebSocketClient(sslContextFactory);
      jettyClient.setConnectTimeout((long)this.connectionTimeout);
      WebSocketPolicy policy = jettyClient.getPolicy();
      policy.setMaxBinaryMessageBufferSize(maxPacketSize);
      policy.setMaxTextMessageBufferSize(maxPacketSize);
      policy.setMaxBinaryMessageSize(maxPacketSize);
      policy.setMaxTextMessageSize(maxPacketSize);

      jettyClient.start();

    }

    int numRetries = 0;
    int maxRetries = 5;
    while (true) {

      try {

        jettyWsSession =
            jettyClient.connect(new WebSocketClientSocket(), uri, new ClientUpgradeRequest())
                .get(this.connectionTimeout, TimeUnit.MILLISECONDS);

        jettyWsSession.setIdleTimeout(this.idleTimeout);

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
