package org.kurento.control.server;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.kurento.jsonrpc.client.JsonRpcClient;

public class ControlServerManager {

	private ConfigurableApplicationContext context;

	public ControlServerManager(JsonRpcClient client, int httpPort) {

		ControlServerApp.setJsonRpcClient(client);

		SpringApplication application = new SpringApplication(
				ControlServerApp.class);

		Properties properties = new Properties();
		properties.put("server.port", httpPort);
		application.setDefaultProperties(properties);

		context = application.run();
	}

	public void destroy() {
		context.close();
	}
}
