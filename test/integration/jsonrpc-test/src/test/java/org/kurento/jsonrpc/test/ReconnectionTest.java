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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.Handler;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionAdapter;
import org.kurento.jsonrpc.client.ReconnectedHandler;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.kurento.jsonrpc.test.util.EventWaiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectionTest extends JsonRpcConnectorBaseTest {

  private static final Logger log = LoggerFactory.getLogger(ReconnectionTest.class);

  public static class ServerHandler extends DefaultJsonRpcHandler<String> {

    @Override
    public void handleRequest(final Transaction transaction, Request<String> request)
        throws Exception {

      Session session = transaction.getSession();

      if (session.isNew()) {
        transaction.sendResponse("new");
      } else {
        transaction.sendResponse("old");
      }
    }

    @Override
    public void afterConnectionEstablished(Session session) throws Exception {
      session.setReconnectionTimeout(5000);
    }
  }

  @Test
  public void givenReconnectedSession_whenSessionIdIsRemovedFromClient_thenServerUsesWebSocketAsSessionIdSource()
      throws IOException, InterruptedException {

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection")) {

      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("new");
      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");
      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");

      String sessionId = client.getSession().getSessionId();

      client.closeNativeClient();

      // Wait for reconnection
      Thread.sleep(100);

      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");
      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");

      client.setSessionId(null);

      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");
      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");

      assertThat(client.getSession().getSessionId()).isEqualTo(sessionId);

    }
  }

  @Test
  public void givenClient_whenNativeSocketIsClosed_thenSessionIsAutomaticallyReconnected()
      throws IOException, InterruptedException {

    final EventWaiter reconnecting = new EventWaiter("reconnecting");
    final EventWaiter reconnected = new EventWaiter("reconnected");

    final EventWaiter reconnectingHandler = new EventWaiter("reconnectingHandler");
    final EventWaiter reconnectedHandler = new EventWaiter("reconnectedHandler");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void reconnecting() {
        reconnecting.eventReceived();
      }

      @Override
      public void reconnected(boolean sameServer) {
        reconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.onReconnecting(new Handler() {
        @Override
        public void run() {
          reconnectingHandler.eventReceived();
        }
      });

      client.onReconnected(new ReconnectedHandler() {
        @Override
        public void run(boolean sameServer) {
          reconnectedHandler.eventReceived();
        }
      });

      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("new");
      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");
      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");

      client.closeNativeClient();

      // Wait for reconnection
      Thread.sleep(100);

      reconnecting.waitFor(3000);
      reconnectingHandler.waitFor(3000);

      reconnected.waitFor(3000);
      reconnectedHandler.waitFor(3000);

      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");
      assertThat(client.sendRequest("sessiontest", String.class)).isEqualTo("old");

    }
  }

  @Test
  public void givenJsonRpcClientAndServer_whenServerIsDown_thenClientKeepsReconnectingUntilServerIsUpAgain()
      throws Exception {

    final EventWaiter reconnecting = new EventWaiter("reconnecting");
    final EventWaiter reconnected = new EventWaiter("reconnected");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void reconnecting() {
        reconnecting.eventReceived();
      }

      @Override
      public void reconnected(boolean sameServer) {
        reconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.setTryReconnectingForever(true);
      client.enableHeartbeat(2000);

      Thread.sleep(1000);

      client.connect();

      log.debug("--------> Client connected to server");

      server.close();

      log.debug("--------> Server closed");

      reconnecting.waitFor(3000);

      log.debug("--------> Event reconnecting received in client");

      assertThat(reconnected.isEventRecived()).isFalse();

      // Wait some time to verify client is reconnecting
      Thread.sleep(20000);

      assertThat(reconnected.isEventRecived()).isFalse();

      log.debug("--------> Starting new server after 20s");

      startServer();

      log.debug("--------> New server started");

      log.debug("--------> Waiting 10s to client reconnection");

      reconnected.waitFor(10000);

      log.debug("--------> Client reconnected event received");

    }
  }

  @Test
  public void givenJsonRpcClientAndServer_whenServerIsDown_thenClientKeepsReconnectingUntilMaxTime()
      throws Exception {

    final EventWaiter reconnecting = new EventWaiter("reconnecting");
    final EventWaiter disconnected = new EventWaiter("disconnected");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void reconnecting() {
        reconnecting.eventReceived();
      }

      @Override
      public void disconnected() {
        disconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.setTryReconnectingMaxTime(7000);
      client.enableHeartbeat(2000);

      Thread.sleep(1000);

      client.connect();

      log.debug("--------> Client connected to server");

      server.close();

      log.debug("--------> Server closed");

      reconnecting.waitFor(3000);

      log.debug("--------> Event reconnecting received in client");

      assertThat(disconnected.isEventRecived()).isFalse();

      Thread.sleep(7000);

      disconnected.waitFor(20000);

    } finally {
      startServer();
    }
  }

  @Test
  public void givenClientWithHeartbeat_whenWaitMoreThanIdleTimeout_thenClientIsNotDisconnected()
      throws IOException, InterruptedException {

    final EventWaiter reconnecting = new EventWaiter("reconnecting");
    final EventWaiter reconnected = new EventWaiter("reconnected");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void reconnecting() {
        reconnecting.eventReceived();
      }

      @Override
      public void reconnected(boolean sameServer) {
        reconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.setIdleTimeout(5000);
      client.enableHeartbeat(4000);

      for (int i = 0; i < 5; i++) {
        client.sendRequest("sessiontest", String.class);
        Thread.sleep(10000);
      }
    }

    assertThat(reconnecting.isEventRecived()).as("Event reconnecting received").isEqualTo(false);
    assertThat(reconnecting.isEventRecived()).as("Event reconnected received").isEqualTo(false);
  }

  @Test
  public void givenReconnectingClient_whenClientIsClosed_thenClientIsNotReconnectedWhenServerIsUpAgain()
      throws Exception {

    final EventWaiter reconnecting = new EventWaiter("reconnecting");
    final EventWaiter reconnected = new EventWaiter("reconnected");
    final EventWaiter disconnected = new EventWaiter("disconnected");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void reconnecting() {
        reconnecting.eventReceived();
      }

      @Override
      public void reconnected(boolean sameServer) {
        reconnected.eventReceived();
      }

      @Override
      public void disconnected() {
        disconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.setTryReconnectingForever(true);
      client.enableHeartbeat(2000);

      Thread.sleep(1000);

      client.connect();

      log.debug("--------> Client connected to server");

      server.close();

      log.debug("--------> Server closed");

      reconnecting.waitFor(3000);

      log.debug("--------> Event reconnecting received in client");

      assertThat(reconnected.isEventRecived()).isFalse();

      // Wait some time to verify client is reconnecting
      Thread.sleep(20000);

      assertThat(reconnected.isEventRecived()).isFalse();

      log.debug("--------> Starting new server after 20s");

      client.close();

      disconnected.waitFor(20000);

      log.debug("--------> Client is disconnected");

      startServer();

      log.debug("--------> New server started");

      Thread.sleep(10000);

      assertThat(reconnected.isEventRecived()).isFalse();

      log.debug("--------> Client is not reconnected after 10s after closing it");

    }
  }

}
