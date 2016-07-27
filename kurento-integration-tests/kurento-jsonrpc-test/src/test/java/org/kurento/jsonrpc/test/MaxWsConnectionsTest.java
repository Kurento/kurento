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

package org.kurento.jsonrpc.test;

import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxWsConnectionsTest extends JsonRpcConnectorBaseTest {

  private static final Logger log = LoggerFactory.getLogger(MaxWsConnectionsTest.class);

  @WebSocket
  public static class WebSocketClientSocket {

    @OnWebSocketClose
    public void onClose(int statusCode, String closeReason) {

    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
    }
  }

  @Test
  public void test() throws Exception {

    List<Session> clients = new ArrayList<>();

    while (true) {

      URI wsUri = new URI("ws", null, "localhost", Integer.parseInt(getPort()), "/jsonrpc", null,
          null);

      WebSocketClient jettyClient = new WebSocketClient(new SslContextFactory(true));
      jettyClient.start();
      Session session = jettyClient
          .connect(new WebSocketClientSocket(), wsUri, new ClientUpgradeRequest()).get();

      clients.add(session);

      log.debug("WebSocket client {} connected", clients.size());

      Thread.sleep(100);

      if (!session.isOpen()) {
        if (clients.size() < MAX_WS_CONNECTIONS) {
          fail("WebSocket num " + clients.size() + " disconnected. MAX_WS_CONNECTION="
              + MAX_WS_CONNECTIONS);
        } else {
          log.debug("WebSocket client {} disconnected from server", clients.size());
          break;
        }
      } else {

        if (clients.size() > MAX_WS_CONNECTIONS) {
          fail("Server should close automatically WebSocket connection above " + MAX_WS_CONNECTIONS
              + " but it has " + clients.size() + " open connections");
        }
      }
    }

  }

}
