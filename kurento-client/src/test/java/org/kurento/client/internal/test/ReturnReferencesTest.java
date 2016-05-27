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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.test.model.client.SampleClass;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

public class ReturnReferencesTest {

  private static RomManager manager;

  @BeforeClass
  public static void initFactory() {
    manager = new RomManager(new RomClientJsonRpcClient(new JsonRpcClientLocal(
        new RomServerJsonRpcHandler("org.kurento.client.internal.test.model.server", "Impl"))));
  }

  @Test
  public void objectRefTest() {

    SampleClass obj = new SampleClass.Builder("AAA", false, manager).withAtt3(0.5f).withAtt4(22)
        .build();

    SampleClass obj2 = new SampleClass.Builder("BBB", false, manager).withAtt3(0.5f).withAtt4(22)
        .build();

    SampleClass obj3 = obj.echoObjectRef(obj2);

    assertEquals(obj3.getAtt1(), obj2.getAtt1());
    assertEquals(obj3.getAtt2(), obj2.getAtt2());
  }

  @Test
  public void objectRefTestAsync() throws InterruptedException {

    SampleClass obj = new SampleClass.Builder("AAA", false, manager).withAtt3(0.5f).withAtt4(22)
        .build();

    final SampleClass obj2 = new SampleClass.Builder("BBB", false, manager).withAtt3(0.5f)
        .withAtt4(22).build();

    SampleClass obj3 = obj.echoObjectRef(obj2);

    Assert.assertNotNull(obj3);

    assertEquals(obj3.getAtt1(), obj2.getAtt1());
    assertEquals(obj3.getAtt2(), obj2.getAtt2());
  }

}
