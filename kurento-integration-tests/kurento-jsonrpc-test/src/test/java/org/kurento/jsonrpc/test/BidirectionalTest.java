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

public class BidirectionalTest extends JsonRpcConnectorBaseTest {

  public static class Handler extends DefaultJsonRpcHandler<Object> {

    private static Logger log = LoggerFactory.getLogger(Handler.class);

    @Override
    public void handleRequest(Transaction transaction, Request<Object> request) throws Exception {

      log.debug("Request id:" + request.getId());
      log.debug("Request method:" + request.getMethod());
      log.debug("Request params:" + request.getParams());

      transaction.sendResponse(request.getParams());

      final Session session = transaction.getSession();
      final Object params = request.getParams();

      new Thread() {
        @Override
        public void run() {
          asyncReverseSend(session, params);
        }
      }.start();
    }

    public void asyncReverseSend(Session session, Object params) {

      try {

        Thread.sleep(1000);

        try {

          Object response = session.sendRequest("method", params);
          session.sendRequest("method", response);

        } catch (IOException e) {
          e.printStackTrace();
        }

      } catch (InterruptedException e) {
      }
    }

  }

  private static final Logger log = LoggerFactory.getLogger(BidirectionalTest.class);

  public static class Params {
    String param1;
    String param2;
  }

  @Test
  public void test() throws IOException, InterruptedException {

    log.debug("Client started");

    JsonRpcClient client = createJsonRpcClient("/jsonrpcreverse");

    final CountDownLatch inverseRequestLatch = new CountDownLatch(2);
    final Params[] inverseRequestParams = new Params[1];

    client.setServerRequestHandler(new DefaultJsonRpcHandler<Params>() {

      @Override
      public void handleRequest(Transaction transaction, Request<Params> request) throws Exception {

        log.debug("Reverse request: " + request);

        transaction.sendResponse(request.getParams());
        inverseRequestParams[0] = request.getParams();

        inverseRequestLatch.countDown();
      }
    });

    Params params = new Params();
    params.param1 = "Value1";
    params.param2 = "Value2";

    Params result = client.sendRequest("echo", params, Params.class);

    log.debug("Response:" + result);

    Assert.assertEquals(params.param1, result.param1);
    Assert.assertEquals(params.param2, result.param2);

    inverseRequestLatch.await();

    Params newResult = inverseRequestParams[0];

    Assert.assertEquals(params.param1, newResult.param1);
    Assert.assertEquals(params.param2, newResult.param2);

    client.close();

    log.debug("Client finished");

  }

}
