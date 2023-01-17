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

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class JsonRpcClientLocalTest {

  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcClientLocalTest.class);

  static class EchoJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

    @Override
    public void handleRequest(Transaction transaction, Request<JsonObject> request)
        throws Exception {

      LOG.info("Request id:" + request.getId());
      LOG.info("Request method:" + request.getMethod());
      LOG.info("Request params:" + request.getParams());

      transaction.sendResponse(request.getParams());
    }
  }

  static class Params {
    String param1;
    String param2;
  }

  @Test
  public void echoTest() throws Exception {

    LOG.info("Client started");

    JsonRpcClient client = new JsonRpcClientLocal(new EchoJsonRpcHandler());

    Params params = new Params();
    params.param1 = "Value1";
    params.param2 = "Value2";

    Params result = client.sendRequest("echo", params, Params.class);

    LOG.info("Response:" + result);

    Assert.assertEquals(params.param1, result.param1);
    Assert.assertEquals(params.param2, result.param2);

    client.close();

    LOG.info("Client finished");

  }

}
