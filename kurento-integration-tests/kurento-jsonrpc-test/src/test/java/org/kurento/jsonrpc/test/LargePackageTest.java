package org.kurento.jsonrpc.test;

import java.io.IOException;

import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

public class LargePackageTest extends JsonRpcConnectorBaseTest {

	public static class Handler extends DefaultJsonRpcHandler<Integer> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<Integer> request) throws Exception {

			String largeString = newLargeString();
			
			System.out.println(largeString.getBytes().length);
			
			transaction.sendResponse(largeString);
		}

		private String newLargeString() {			
			StringBuilder sb = new StringBuilder();			
			for(int i=0; i<6600; i++){
				sb.append("aaaaaaaaaa");
			}			
			return sb.toString();
		}
	}

	@Test
	public void test() throws IOException, InterruptedException {

		JsonRpcClient client = createJsonRpcClient("/largepackage");

		String largePackage = client.sendRequest("echo", String.class);
		
		System.out.println(largePackage);
		
		Thread.sleep(2000);
		
		client.close();

	}

}
