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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.server.ProtocolException;
import org.kurento.client.internal.test.model.SampleRemoteClass;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

public class RomInterfaceImplTest {

  protected static RomManager manager;

  @BeforeClass
  public static void initFactory() {
    manager = new RomManager(new RomClientJsonRpcClient(new JsonRpcClientLocal(
        new RomServerJsonRpcHandler("org.kurento.client.internal.test.model", "Impl"))));
  }

  private SampleRemoteClass obj;

  @Before
  public void initObject() {
    obj = new SampleRemoteClass.Builder(manager).build();
  }

  @Test
  public void voidReturnMethodTest() throws ProtocolException {
    obj.methodReturnVoid();
  }

  @Test
  public void stringReturnMethodTest() throws ProtocolException {
    assertEquals(obj.methodReturnsString(), "XXXX");
  }

  @Test
  public void intReturnMethodTest() throws ProtocolException {
    assertEquals(obj.methodReturnsInt(), 0);
  }

  @Test
  public void booleanReturnMethodTest() throws ProtocolException {
    assertEquals(obj.methodReturnsBoolean(), false);
  }

  @Test
  public void floatReturnMethodTest() throws ProtocolException {
    assertEquals(obj.methodReturnsFloat(), 0.5f, 0.01);
  }

  @Test
  public void stringParamMethodTest() throws ProtocolException {
    assertEquals(obj.methodParamString("XXXX"), "XXXX");
  }

  @Test
  public void intParamMethodTest() throws ProtocolException {
    assertEquals(obj.methodParamInt(55), 55);
  }

  @Test
  public void booleanParamMethodTest() throws ProtocolException {
    assertEquals(obj.methodParamBoolean(true), true);
  }

  @Test
  public void floatParamMethodTest() throws ProtocolException {
    assertEquals(obj.methodParamFloat(0.5f), 0.5f, 0.01);
  }

}
