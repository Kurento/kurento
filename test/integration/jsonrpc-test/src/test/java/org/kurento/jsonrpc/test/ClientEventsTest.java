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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEventsTest extends JsonRpcConnectorBaseTest {

  private static final Logger log = LoggerFactory.getLogger(ClientEventsTest.class);

  private static final long TIMEOUT = 5000;

  @Test
  public void test() throws IOException, InterruptedException {

    log.debug("Client started");

    JsonRpcClient client = createJsonRpcClient("/jsonrpcreverse");

    final CountDownLatch afterConnectionEstablishedLatch = new CountDownLatch(1);
    final CountDownLatch afterConnectionClosedLatch = new CountDownLatch(1);
    final CountDownLatch inverseRequestLatch = new CountDownLatch(2);
    final String[] inverseRequestParams = new String[1];

    client.setServerRequestHandler(new DefaultJsonRpcHandler<String>() {

      @Override
      public void afterConnectionEstablished(Session session) throws Exception {

        log.debug("Connection established with sessionId: " + session.getSessionId());
        afterConnectionEstablishedLatch.countDown();
      }

      @Override
      public void handleRequest(Transaction transaction, Request<String> request) throws Exception {

        log.debug("Reverse request: " + request);

        transaction.sendResponse(request.getParams());
        inverseRequestParams[0] = request.getParams();

        inverseRequestLatch.countDown();
      }

      @Override
      public void afterConnectionClosed(Session session, String status) throws Exception {

        log.debug("Connection closed: " + status);
        afterConnectionClosedLatch.countDown();
      }
    });

    String result = client.sendRequest("echo", "params", String.class);

    Assert.assertTrue("The method 'afterConnectionEstablished' is not invoked",
        afterConnectionEstablishedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

    log.debug("Response:" + result);

    Assert.assertEquals("params", result);

    Assert.assertTrue("The method 'handleRequest' is not invoked",
        inverseRequestLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

    String newResult = inverseRequestParams[0];

    Assert.assertEquals("params", newResult);

    client.close();

    Assert.assertTrue("The method 'afterConnectionClosed' is not invoked",
        afterConnectionClosedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

    log.debug("Client finished");

  }

}
