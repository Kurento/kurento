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

package org.kurento.client.internal.test;

import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

public class LocalRomTest extends AbstractRomTest {

  private JsonRpcHandler<? extends Object> handler;

  @Override
  protected JsonRpcClient createJsonRpcClient() {
    return new JsonRpcClientLocal(handler);
  }

  @Override
  protected void startJsonRpcServer(RomServerJsonRpcHandler jsonRpcHandler) {
    this.handler = jsonRpcHandler;
  }

  @Override
  protected void destroyJsonRpcServer() {

  }

}
