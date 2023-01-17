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

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.kurento.jsonrpc.client.JsonRpcClientHttp;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

public class JsonRpcClientConnectTest extends JsonRpcConnectorBaseTest {

  @Test
  public void correctConnectTest() {

    try {

      try (JsonRpcClientHttp client =
          new JsonRpcClientHttp("http://localhost:" + getPort() + "/jsonrpc")) {
        client.connect();
      }

    } catch (IOException e) {
      fail("IOException shouldn't be thrown");
    }
  }

  @Test
  public void incorrectConnectTest() {

    try {

      try (JsonRpcClientHttp client = new JsonRpcClientHttp("http://localhost:9999/jsonrpc")) {
        client.connect();
      }

    } catch (IOException e) {
      return;
    }

    fail("IOException should be thrown");
  }

}
