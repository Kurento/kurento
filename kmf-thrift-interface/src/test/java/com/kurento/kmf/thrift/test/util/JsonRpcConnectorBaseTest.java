package com.kurento.kmf.thrift.test.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

import com.kurento.kmf.commons.tests.JsonRpcConnectorTests;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcServerThrift;

@Category(JsonRpcConnectorTests.class)
public class JsonRpcConnectorBaseTest {

	private static final String LOCALHOST = "127.0.0.1";

	private static final int THRIFT_SERVER_PORT = 19191;
	private static final int THRIFT_CLIENT_PORT = 55555;

	private static JsonRpcServerThrift server;

	@BeforeClass
	public static void start() throws Exception {

		server = new JsonRpcServerThrift(new EchoJsonRpcHandler(), LOCALHOST,
				THRIFT_SERVER_PORT);

		server.start();
	}

	@AfterClass
	public static void stop() {
		server.destroy();
	}

	protected JsonRpcClient createJsonRpcClient() {

		return new JsonRpcClientThrift(LOCALHOST, THRIFT_SERVER_PORT,
				LOCALHOST, THRIFT_CLIENT_PORT);
	}

	public static void main(String[] args) throws Exception {
		start();
	}

}
