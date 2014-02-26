package com.kurento.kmf.jsonrpcconnector.test.base;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientHttp;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientWebSocket;

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

		context.close();
	}

	protected static String getPort() {
		String port = System.getProperty("http.port");
		if (port == null) {
			port = "7788";
		}
		return port;
	}

	public JsonRpcClient createJsonRpcClient(String servicePath) {

		String clientType = System.getProperty("jsonrpcconnector-client-type");

		if (clientType == null) {
			clientType = "ws";
		}

		JsonRpcClient client;
		if ("ws".equals(clientType)) {
			client = new JsonRpcClientWebSocket("ws://localhost:" + getPort()
					+ servicePath);
		} else if ("http".equals(clientType)) {
			client = new JsonRpcClientHttp("http://localhost:" + getPort()
					+ servicePath);
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
