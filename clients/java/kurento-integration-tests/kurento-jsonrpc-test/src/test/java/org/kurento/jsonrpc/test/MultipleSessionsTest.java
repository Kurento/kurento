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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.DemoBean;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.springframework.beans.factory.annotation.Autowired;

public class MultipleSessionsTest extends JsonRpcConnectorBaseTest {

  public static class Handler extends DefaultJsonRpcHandler<String> {

    @Autowired
    DemoBean demoBean;

    private int counter = 0;

    @Override
    public void handleRequest(Transaction transaction, Request<String> request) throws Exception {

      if (demoBean == null) {
        throw new RuntimeException("Not autowired dependencies");
      }

      transaction.sendResponse(counter);
      counter++;
    }
  }

  @Ignore
  @Test
  public void test() throws InterruptedException {

    ExecutorService executorService = Executors.newFixedThreadPool(5);

    List<Callable<Void>> callables = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      callables.add(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          counterSession();
          return null;
        }
      });
    }

    List<Future<Void>> futures = executorService.invokeAll(callables);
    executorService.shutdown();
    executorService.awaitTermination(99999, TimeUnit.DAYS);

    for (Future<Void> future : futures) {
      try {
        future.get();
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void beanNameTest() throws IOException {

    createJsonRpcClient("/jsonrpc_multiple2").sendRequest("count");

  }

  private void counterSession() {

    JsonRpcClient client = createJsonRpcClient("/jsonrpc_multiple");

    try {

      for (int i = 0; i < 5; i++) {

        int counter = client.sendRequest("count", null, Integer.class);
        Assert.assertEquals(i, counter);
      }

      client.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
