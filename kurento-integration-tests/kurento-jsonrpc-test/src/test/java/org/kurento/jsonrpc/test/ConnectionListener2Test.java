package org.kurento.jsonrpc.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListener2Test extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(ConnectionListener2Test.class);

	@Test
	public void serverDisconnectedTest() throws IOException,
			InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);

		JsonRpcClient client = new JsonRpcClientWebSocket("ws://localhost:"
				+ getPort() + "/connectionlistener",
				new JsonRpcWSConnectionListener() {

					@Override
					public void disconnected() {
						log.info("disconnected");
						latch.countDown();
					}

					@Override
					public void connectionFailed() {
					}

					@Override
					public void connected() {
					}

					@Override
					public void reconnected(boolean sameServer) {
						// TODO Auto-generated method stub

					}
				});

		client.sendRequest("sessiontest", String.class);
		context.close();

		if (!latch.await(20, TimeUnit.SECONDS)) {
			fail("Event disconnected() not thrown in 20s");
		}

		client.close();
	}

}
