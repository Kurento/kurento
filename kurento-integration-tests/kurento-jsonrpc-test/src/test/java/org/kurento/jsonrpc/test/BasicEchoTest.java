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
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicEchoTest extends JsonRpcConnectorBaseTest {

  private static final Logger log = LoggerFactory.getLogger(BasicEchoTest.class);

  static class Params {
    String param1;
    String param2;
  }

  @Test
  public void test() throws IOException {

    log.debug("Client started");

    JsonRpcClient client = createJsonRpcClient("/jsonrpc");
    Params params = new Params();
    params.param1 = "Value1";
    params.param2 = "Value2";

    Params result = client.sendRequest("echo", params, Params.class);

    log.debug("Response:" + result);

    Assert.assertEquals(params.param1, result.param1);
    Assert.assertEquals(params.param2, result.param2);

    client.close();

    log.debug("Client finished");

  }

}
