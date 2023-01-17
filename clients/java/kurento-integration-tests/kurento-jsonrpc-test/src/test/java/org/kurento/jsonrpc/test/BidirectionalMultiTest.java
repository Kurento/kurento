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

import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BidirectionalMultiTest extends JsonRpcConnectorBaseTest {

  public static class Handler extends DefaultJsonRpcHandler<Integer> {

    private static Logger log = LoggerFactory.getLogger(Handler.class);

    @Override
    public void handleRequest(Transaction transaction, Request<Integer> request) throws Exception {

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

          for (int i = 0; i < 5; i++) {
            Object response = session.sendRequest("method", params);
            session.sendRequest("method", response);
          }

        } catch (IOException e) {
          e.printStackTrace();
        }

      } catch (InterruptedException e) {
      }
    }

  }

  private static final Logger log = LoggerFactory.getLogger(BidirectionalMultiTest.class);

  @Test
  public void test() throws IOException, InterruptedException {

    log.debug("Client started");

    JsonRpcClient client = createJsonRpcClient("/BidirectionalMultiTest");

    client.setServerRequestHandler(new DefaultJsonRpcHandler<Integer>() {

      @Override
      public void handleRequest(Transaction transaction, Request<Integer> request)
          throws Exception {

        log.debug("Reverse request: " + request);
        transaction.sendResponse(request.getParams() + 1);
      }
    });

    for (int i = 0; i < 60; i++) {
      client.sendRequest("echo", i, Integer.class);
    }

    client.close();

    log.debug("Client finished");
  }

}
