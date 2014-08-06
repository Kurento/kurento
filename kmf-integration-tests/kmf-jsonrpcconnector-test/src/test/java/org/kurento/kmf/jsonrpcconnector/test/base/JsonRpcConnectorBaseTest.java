package org.kurento.kmf.jsonrpcconnector.test.base;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;

import org.kurento.kmf.commons.tests.JsonRpcConnectorTests;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClientHttp;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClientWebSocket;

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

		// KurentoApplicationContextUtils
		// .closeAllKurentoApplicationContexts(((WebApplicationContext) context)
		// .getServletContext());

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
		return createJsonRpcClient(servicePath, new HttpHeaders());
	}

	/**
	 * @param string
	 * @param headers
	 * @return
	 */
	protected JsonRpcClient createJsonRpcClient(String servicePath,
			HttpHeaders headers) {

		String clientType = System.getProperty("jsonrpcconnector-client-type");

		if (clientType == null) {
			clientType = "ws";
		}

		JsonRpcClient client;
		if ("ws".equals(clientType)) {
			client = new JsonRpcClientWebSocket("ws://localhost:" + getPort()
					+ servicePath, headers);
		} else if ("http".equals(clientType)) {
			client = new JsonRpcClientHttp("http://localhost:" + getPort()
					+ servicePath, headers);
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
