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
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

public class NotificationTest extends JsonRpcConnectorBaseTest {

  private static CountDownLatch serverRequestLatch;

  public static class Handler extends DefaultJsonRpcHandler<Integer> {

    @Override
    public void handleRequest(final Transaction transaction, Request<Integer> request)
        throws Exception {

      if (!transaction.isNotification()) {
        throw new RuntimeException("Notification expected");
      }

      Thread.sleep(1000);

      transaction.getSession().sendNotification("response", request.getParams());
    }
  }

  @Test
  public void test() throws IOException, InterruptedException {

    serverRequestLatch = new CountDownLatch(3);

    JsonRpcClient client = createJsonRpcClient("/notification");

    client.setServerRequestHandler(new DefaultJsonRpcHandler<Integer>() {

      @Override
      public void handleRequest(Transaction transaction, Request<Integer> request)
          throws Exception {

        serverRequestLatch.countDown();
      }
    });

    client.sendNotification("echo", 1);
    client.sendNotification("echo", 2);
    client.sendNotification("echo", 3);

    Assert.assertTrue("The server has not invoked requests",
        serverRequestLatch.await(5000, TimeUnit.MILLISECONDS));

    client.close();

  }

}
