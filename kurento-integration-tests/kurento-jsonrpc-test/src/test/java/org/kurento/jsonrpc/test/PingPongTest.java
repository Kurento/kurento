package org.kurento.jsonrpc.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(PingPongTest.class);

	public static class Handler extends DefaultJsonRpcHandler<String> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			transaction.sendResponse("OK");
		}
		
		@Override
		public boolean isPingWachdog() {
			return true;
		}
	}
	
	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient("/pingpong", new JsonRpcWSConnectionListener(){

			@Override
			public void connected() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void connectionFailed() {
				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				
			}

			@Override
			public void disconnected() {
				System.out.println("#######################################");
				
			}});

		client.setHeartbeatInterval(500);
		client.enableHeartbeat();
		
		String result = client.sendRequest("echo", "Params", String.class);

		log.info("Response:" + result);

		Assert.assertEquals(result, "OK");

		Thread.sleep(2000);
		
		log.info("Disabling heartbeat in client");
		
		client.disableHeartbeat();
		
		Thread.sleep(50000);

		log.info("Client finished");

	}

}
