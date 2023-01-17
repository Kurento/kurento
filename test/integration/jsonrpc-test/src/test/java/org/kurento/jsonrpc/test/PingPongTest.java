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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionAdapter;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongTest extends JsonRpcConnectorBaseTest {

  private static final Logger log = LoggerFactory.getLogger(PingPongTest.class);

  public static class Handler extends DefaultJsonRpcHandler<String> {

    @Override
    public void handleRequest(final Transaction transaction, Request<String> request)
        throws Exception {

      transaction.sendResponse("OK");
    }

    @Override
    public boolean isPingWatchdog() {
      return true;
    }
  }

  @Test
  public void test() throws IOException, InterruptedException {

    log.debug("Client started");

    JsonRpcClient client = createJsonRpcClient("/pingpong", new JsonRpcWSConnectionAdapter() {

      @Override
      public void connectionFailed() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

      }

      @Override
      public void disconnected() {
        System.out.println("#######################################");

      }
    });

    client.setHeartbeatInterval(500);
    client.enableHeartbeat();

    String result = client.sendRequest("echo", "Params", String.class);

    log.debug("Response:" + result);

    Assert.assertEquals(result, "OK");

    Thread.sleep(20000);

    log.debug("----------------- Disabling heartbeat in client ----------------");

    client.disableHeartbeat();

    // This should lead to reconnect clients

    Thread.sleep(30000);

    log.debug("----------------- Enabling heartbeat in client ----------------");
    client.enableHeartbeat();

    Thread.sleep(30000);

    log.debug("Client finished");

  }

}
