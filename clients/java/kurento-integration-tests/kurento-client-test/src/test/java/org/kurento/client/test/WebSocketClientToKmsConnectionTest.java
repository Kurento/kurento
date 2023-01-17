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

package org.kurento.client.test;

import static org.junit.Assert.fail;

import java.net.URI;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;
import org.kurento.test.base.KurentoClientTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientToKmsConnectionTest extends KurentoClientTest {

  private static Logger log = LoggerFactory.getLogger(WebSocketClientToKmsConnectionTest.class);

  @WebSocket
  public class WebSocketHandler {

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      log.debug("WebSocket OnClose");
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
      log.debug("WebSocket OnConnect");
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
      log.debug("WebSocket OnMessage: " + msg);
    }
  }

  @Test
  public void reconnectTest() throws Exception {

    for (int i = 0; i < 2; i++) {

      String kmsUrl = kms.getWsUri();

      log.debug("Connecting to KMS in " + kmsUrl);

      WebSocketClient client = new WebSocketClient();
      WebSocketHandler socket = new WebSocketHandler();

      client.start();
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      Session wsSession = client.connect(socket, new URI(kmsUrl), request).get();

      wsSession.getRemote().sendString("xxxx");

      kms.stopKms();

      Thread.sleep(3000);

      kms.start();

    }
  }

  @Test
  public void errorSendingClosedKmsTest() throws Exception {

    String kmsUrl = kms.getWsUri();

    log.debug("Connecting to KMS in " + kmsUrl);

    WebSocketClient client = new WebSocketClient();
    WebSocketHandler socket = new WebSocketHandler();

    client.start();
    ClientUpgradeRequest request = new ClientUpgradeRequest();
    Session wsSession = client.connect(socket, new URI(kmsUrl), request).get();

    wsSession.getRemote().sendString("xxxx");

    kms.stopKms();

    Thread.sleep(3000);

    try {

      wsSession.getRemote().sendString("xxxx");
      fail("Trying to send to a closed WebSocket should raise an exception");
    } catch (Exception e) {

    }
  }
}
