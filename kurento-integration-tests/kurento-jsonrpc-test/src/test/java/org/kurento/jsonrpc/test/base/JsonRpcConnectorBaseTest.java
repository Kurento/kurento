package org.kurento.jsonrpc.test.base;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.JsonRpcConnectorTests;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientHttp;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Category(JsonRpcConnectorTests.class)
public class JsonRpcConnectorBaseTest {

	protected static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {

		Properties properties = new Properties();
		properties.put("server.port", getPort());

		SpringApplication application = new SpringApplication(
				BootTestApplication.class);

		application.setDefaultProperties(properties);

		System.out.println("Properties: " + properties);

		context = application.run();

	}

	@AfterClass
	public static void stop() {

		if (context != null) {
			context.close();
		}
	}

	protected static String getPort() {
		String port = System.getProperty("http.port");
		if (port == null) {
			port = "7788";
		}
		return port;
	}

	protected JsonRpcClient createJsonRpcClient(String servicePath) {
		return createJsonRpcClient(servicePath, null);
	}

	protected JsonRpcClient createJsonRpcClient(String servicePath,
			JsonRpcWSConnectionListener listener) {

		String clientType = System.getProperty("jsonrpcconnector-client-type");

		if (clientType == null) {
			clientType = "ws";
		}

		JsonRpcClient client;
		if ("ws".equals(clientType)) {
			client = new JsonRpcClientWebSocket("ws://localhost:" + getPort()
					+ servicePath, listener);
		} else if ("http".equals(clientType)) {
			client = new JsonRpcClientHttp("http://localhost:" + getPort()
					+ servicePath, null);
		} else {
			throw new RuntimeException(
					"Unrecognized property value jsonrpcconnector-client-type="
							+ clientType);
		}

		return client;
	}

	public static void main(String[] args) throws Exception {
		start();
	}

}
