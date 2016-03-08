
package org.kurento.jsonrpc.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionAdapter;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.kurento.jsonrpc.test.util.EventWaiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListenerTest extends JsonRpcConnectorBaseTest {

  private static final Logger log = LoggerFactory.getLogger(ConnectionListenerTest.class);

  @Test
  public void givenPortWithoutServer_whenClientTryToConnect_thenAnExceptionIsThrownAndConnectionFailedEventIsFired()
      throws IOException, InterruptedException {

    final EventWaiter connectionFailed = new EventWaiter("connectionFailed");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void connectionFailed() {
        connectionFailed.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:65000/reconnection", listener)) {

      client.sendRequest("sessiontest", String.class);

      fail("KurentoException informing connection exception should be thrown");

    } catch (KurentoException e) {
      assertThat(e.getMessage()).contains("Exception connecting to");
    }

    connectionFailed.waitFor(20000);
  }

  @Test
  public void givenServer_whenClientIsConnected_thenConnectedEventIsFired()
      throws IOException, InterruptedException {

    final EventWaiter connected = new EventWaiter("connected");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void connected() {
        connected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.sendRequest("sessiontest", String.class);

      connected.waitFor(20000);
    }
  }

  @Test
  public void givenConnectedClient_whenClientIsClosedByUser_thenDisconnectedEventIsFired()
      throws IOException, InterruptedException {

    final EventWaiter disconnected = new EventWaiter("disconnected");

    JsonRpcWSConnectionAdapter listener = new JsonRpcWSConnectionAdapter() {
      @Override
      public void disconnected() {
        disconnected.eventReceived();
      }
    };

    try (JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://localhost:" + getPort() + "/reconnection", listener)) {

      client.sendRequest("sessiontest", String.class);
      client.close();

      disconnected.waitFor(20000);

    }
  }

  @Test
  public void givenConnectedClient_whenClientIsClosedByUser_thenIsClosedByUserMethodIsTrueWhenDisconnectedEventIsReceived()
      throws IOException, InterruptedException {

    final EventWaiter disconnected = new EventWaiter("disconnected");
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

      clientForListener[0] = client;

      client.sendRequest("sessiontest", String.class);
      client.close();

      disconnected.waitFor(20000);
      closedByClientWhenDisconnected.waitFor(20000);
    }
  }

}
