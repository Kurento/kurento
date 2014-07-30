package com.kurento.kmf.connector;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;

public class MediaConnectorManager {

	private ConfigurableApplicationContext context;

	public MediaConnectorManager(JsonRpcClient client, int httpPort) {

		MediaConnectorApp.setJsonRpcClient(client);

		SpringApplication application = new SpringApplication(
				MediaConnectorApp.class);

		Properties properties = new Properties();
		properties.put("server.port", httpPort);
		application.setDefaultProperties(properties);

		context = application.run();
	}

	public void destroy() {
		context.close();
	}
}
