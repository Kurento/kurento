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
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BasicAsyncClientEchoTest extends JsonRpcConnectorBaseTest {

  private static final Logger log = LoggerFactory.getLogger(BasicAsyncClientEchoTest.class);

  static class Params {
    String param1;
    String param2;
  }

  @Test
  public void test() throws IOException, InterruptedException {

    log.debug("Client started");

    JsonRpcClient client = createJsonRpcClient("/jsonrpc");

    final JsonObject params = new JsonObject();
    params.addProperty("param1", "Value1");
    params.addProperty("param2", "Value2");

    CountDownLatch finishTestLatch = new CountDownLatch(1);

    client.sendRequest("echo", params, new Continuation<JsonElement>() {

      @Override
      public void onSuccess(JsonElement result) {
        log.debug("Response:" + result);

        Assert.assertEquals(params.get("param1").getAsString(), "Value1");
        Assert.assertEquals(params.get("param2").getAsString(), "Value2");
      }

      @Override
      public void onError(Throwable cause) {
        cause.printStackTrace();
      }
    });

    finishTestLatch.await(5, TimeUnit.SECONDS);

    client.close();

    log.debug("Client finished");
  }

}
