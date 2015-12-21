
package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.test.model.Sample2;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

public class ConstructionTest {

  protected static RomManager manager;

  @BeforeClass
  public static void initFactory() {
    manager = new RomManager(new RomClientJsonRpcClient(new JsonRpcClientLocal(
        new RomServerJsonRpcHandler("org.kurento.client.internal.test.model", "Impl"))));
  }

  @Test
  public void initObject() {

    Sample2 obj = new Sample2.Builder("XXX", 33, manager).withAtt3(0.5f).att4().build();

    String att1 = obj.getAtt1();
    assertEquals(att1, "XXX");

    int att2 = obj.getAtt2();
    assertEquals(att2, 33);

    float att3 = obj.getAtt3();
    assertEquals(att3, 0.5f, 0.01);

    boolean att4 = obj.getAtt4();
    assertEquals(att4, true);

  }

}
