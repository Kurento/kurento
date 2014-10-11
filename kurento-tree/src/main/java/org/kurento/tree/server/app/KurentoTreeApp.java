package org.kurento.tree.server.app;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Properties;

import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.kurento.tree.server.FakeTreeManager;
import org.kurento.tree.server.TreeManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JsonRpcConfiguration.class)
@EnableAutoConfiguration
public class KurentoTreeApp implements JsonRpcConfigurer {

	public static final String WEBSOCKET_PORT_PROPERTY = "ws.port";
	public static final String WEBSOCKET_PORT_DEFAULT = "8890";

	@Bean
	public JsonRpcHandler jsonRpcHandler() {
		return new JsonRpcHandler(treeManager());
	}

	@Bean
	public TreeManager treeManager() {
		return new FakeTreeManager();
	}

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addHandler(jsonRpcHandler(), "/kurentotree");
	}

	public static void main(String[] args) throws Exception {
		start();
	}

	public static ConfigurableApplicationContext start() {

		String port = getProperty(WEBSOCKET_PORT_PROPERTY,
				WEBSOCKET_PORT_DEFAULT);

		SpringApplication application = new SpringApplication(
				KurentoTreeApp.class);

		Properties properties = new Properties();
		properties.put("server.port", port);
		application.setDefaultProperties(properties);

		return application.run();
	}
}
