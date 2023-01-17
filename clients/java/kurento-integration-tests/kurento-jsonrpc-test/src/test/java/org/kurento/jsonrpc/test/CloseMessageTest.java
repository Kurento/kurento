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
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

public class CloseMessageTest extends JsonRpcConnectorBaseTest {

  @Test
  public void test() throws IOException, InterruptedException {

    JsonRpcClientWebSocket client = (JsonRpcClientWebSocket) createJsonRpcClient("/reconnection");
    client.setSendCloseMessage(true);

    Assert.assertEquals("new", client.sendRequest("sessiontest", String.class));
    Assert.assertEquals("old", client.sendRequest("sessiontest", String.class));
    Assert.assertEquals("old", client.sendRequest("sessiontest", String.class));

    String sessionId = client.getSession().getSessionId();

    client.close();

    JsonRpcClient client2 = createJsonRpcClient("/reconnection");
    client2.connect();
    client2.setSessionId(sessionId);

    Assert.assertEquals("new", client2.sendRequest("sessiontest", String.class));
    Assert.assertEquals("old", client2.sendRequest("sessiontest", String.class));

  }

}
