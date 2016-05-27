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
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

public class CloseSessionTest extends JsonRpcConnectorBaseTest {

  public static class Handler extends DefaultJsonRpcHandler<String> {

    int counter = 0;

    @Override
    public void handleRequest(final Transaction transaction, Request<String> request)
        throws Exception {

      Session session = transaction.getSession();

      if (session.isNew()) {
        transaction.sendResponse("new");
      } else {
        transaction.sendResponse("old");
      }

      if (counter == 2) {
        session.close();
      }
      counter++;
    }
  }

  @Test
  public void test() throws IOException, InterruptedException {

    JsonRpcClient client = createJsonRpcClient("/close_session_handler");

    Assert.assertEquals("new", client.sendRequest("sessiontest", String.class));
    Assert.assertEquals("old", client.sendRequest("sessiontest", String.class));
    Assert.assertEquals("old", client.sendRequest("sessiontest", String.class));

    client = createJsonRpcClient("/close_session_handler");

    Assert.assertEquals("new", client.sendRequest("sessiontest", String.class));
    Assert.assertEquals("old", client.sendRequest("sessiontest", String.class));
    Assert.assertEquals("old", client.sendRequest("sessiontest", String.class));

    client.close();

  }

}
