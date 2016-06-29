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
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.client.Handler;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionAdapter;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.kurento.jsonrpc.test.util.EventWaiter;

public class ConnectionListenerTest extends JsonRpcConnectorBaseTest {

  @Test
  public void givenPortWithoutServer_whenClientTryToConnect_thenAnExceptionIsThrownAndConnectionFailedEventIsFired()
      throws IOException, InterruptedException {

    final EventWaiter connectionFailed = new EventWaiter("connectionFailed");
    final EventWaiter connectionFailedHandler = new EventWaiter("connectionFailedHandler");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void connectionFailed() {
        connectionFailed.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:65000/reconnection", listener)) {

      client.onConnectionFailed(new Handler() {
        @Override
        public void run() {
          connectionFailedHandler.eventReceived();
        }
      });

      client.sendRequest("sessiontest", String.class);

      fail("KurentoException informing connection exception should be thrown");

    } catch (KurentoException e) {
      assertThat(e.getMessage()).contains("Exception connecting to");
    }

    connectionFailed.waitFor(20000);
    connectionFailedHandler.waitFor(20000);
  }

  @Test
  public void givenServer_whenClientIsConnected_thenConnectedEventIsFired()
      throws IOException, InterruptedException {

    final EventWaiter connected = new EventWaiter("connected");
    final EventWaiter connectedHandler = new EventWaiter("connectedHandler");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void connected() {
        connected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.onConnected(new Handler() {

        @Override
        public void run() {
          connectedHandler.eventReceived();
        }

      });

      client.sendRequest("sessiontest", String.class);

      connected.waitFor(20000);
      connectedHandler.waitFor(20000);
    }
  }

  @Test
  public void givenConnectedClient_whenClientIsClosedByUser_thenDisconnectedEventIsFired()
      throws IOException, InterruptedException {

    final EventWaiter disconnected = new EventWaiter("disconnected");
    final EventWaiter disconnectedHandler = new EventWaiter("disconnectedHandler");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void disconnected() {
        disconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.onDisconnected(new Handler() {
        @Override
        public void run() {
          disconnectedHandler.eventReceived();
        }
      });

      client.sendRequest("sessiontest", String.class);
      client.close();

      disconnected.waitFor(20000);
      disconnectedHandler.waitFor(20000);
    }
  }

  @Test
  public void givenConnectedClient_whenServerIsClosed_thenDisconnectedEventIsFired()
      throws Exception {

    final EventWaiter disconnected = new EventWaiter("disconnected");
    final EventWaiter disconnectedHandler = new EventWaiter("disconnectedHandler");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void disconnected() {
        disconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.onDisconnected(new Handler() {
        @Override
        public void run() {
          disconnectedHandler.eventReceived();
        }
      });

      client.sendRequest("sessiontest", String.class);

      stopServer();

      disconnected.waitFor(20000);
      disconnectedHandler.waitFor(20000);

    } finally {
      startServer();
    }
  }

  @Test
  public void givenConnectedClient_whenClientIsClosedByUser_thenIsClosedByUserMethodIsTrueWhenDisconnectedEventIsReceived()
      throws IOException, InterruptedException {

    final EventWaiter disconnected = new EventWaiter("disconnected");
    final EventWaiter disconnectedHandler = new EventWaiter("disconnectedHandler");
    final EventWaiter closedByClientWhenDisconnected = new EventWaiter(
        "closedByClientWhenDisconnected");

    final JsonRpcClientWebSocket[] clientForListener = new JsonRpcClientWebSocket[1];

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {

      @Override
      public void disconnected() {
        disconnected.eventReceived();
        if (clientForListener[0].isClosedByUser()) {
          closedByClientWhenDisconnected.eventReceived();
        }
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.onDisconnected(new Handler() {
        @Override
        public void run() {
          disconnectedHandler.eventReceived();
        }
      });

      clientForListener[0] = client;

      client.sendRequest("sessiontest", String.class);
      client.close();

      disconnected.waitFor(20000);
      disconnectedHandler.waitFor(20000);
      closedByClientWhenDisconnected.waitFor(20000);
    }
  }

}
