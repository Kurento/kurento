
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
