package org.kurento.test.services;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.kurento.control.server.KurentoControlServerApp;
import org.kurento.jsonrpc.client.JsonRpcClient;

public class ControlServerManager {

	private ConfigurableApplicationContext context;

	public ControlServerManager(JsonRpcClient client, int httpPort) {

		KurentoControlServerApp.setJsonRpcClient(client);

		SpringApplication application = new SpringApplication(
				KurentoControlServerApp.class);

		Properties properties = new Properties();
		properties.put("server.port", httpPort);
		application.setDefaultProperties(properties);

		context = application.run();
	}

	public void destroy() {
		context.close();
	}
}
