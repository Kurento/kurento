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

			try (JsonRpcClientHttp client = new JsonRpcClientHttp(
					"http://localhost:" + getPort() + "/jsonrpc")) {
				client.connect();
			}

		} catch (IOException e) {
			fail("IOException shouldn't be thrown");
		}
	}

	@Test
	public void incorrectConnectTest() {

		try {

			try (JsonRpcClientHttp client = new JsonRpcClientHttp(
					"http://localhost:9999/jsonrpc")) {
				client.connect();
			}

		} catch (IOException e) {
			return;
		}

		fail("IOException should be thrown");
	}

}
