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
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.message.Request;

public class JsonRpcHandlerTypesTest {

  static class Params {
    String param1;
    String param2;
  }

  static class JsonRpcHandlerParams extends DefaultJsonRpcHandler<Params> {
    @Override
    public void handleRequest(Transaction transaction, Request<Params> request) throws Exception {
    }
  }

  static interface FakeInterface<E> {
  }

  static class JsonRpcHandlerParamsMulti extends DefaultJsonRpcHandler<Params>
      implements FakeInterface<Object> {
    @Override
    public void handleRequest(Transaction transaction, Request<Params> request) throws Exception {
    }
  }

  static class JsonRpcHandlerGrandson extends JsonRpcHandlerParams {
    @Override
    public void handleRequest(Transaction transaction, Request<Params> request) throws Exception {
    }
  }

  static class JsonRpcHandlerDefault extends DefaultJsonRpcHandler<Params> {
    @Override
    public void handleRequest(Transaction transaction, Request<Params> request) throws Exception {
    }
  }

  @Test
  public void getParamsTypeTest() {

    Assert.assertEquals(Params.class,
        JsonRpcHandlerManager.getParamsType(new JsonRpcHandlerParams().getHandlerType()));

    Assert.assertEquals(Params.class,
        JsonRpcHandlerManager.getParamsType(new JsonRpcHandlerParamsMulti().getHandlerType()));

    Assert.assertEquals(Params.class,
        JsonRpcHandlerManager.getParamsType(new JsonRpcHandlerGrandson().getHandlerType()));

    Assert.assertEquals(Params.class,
        JsonRpcHandlerManager.getParamsType(new JsonRpcHandlerDefault().getHandlerType()));

  }
}
