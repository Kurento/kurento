package org.kurento.room.server.app;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Properties;

import org.kurento.client.KurentoClient;
import org.kurento.commons.ConfigFileManager;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JsonRpcConfiguration.class)
@EnableAutoConfiguration
public class KurentoRoomServerApp implements JsonRpcConfigurer {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoRoomServerApp.class);

	private static final String UNSECURE_RANDOM_PROPERTY = "unsecureRandom";
	private static final boolean UNSECURE_RANDOM_DEFAULT = true;

	public static final String WEBSOCKET_PORT_PROPERTY = "ws.port";
	public static final String WEBSOCKET_PORT_DEFAULT = "8890";

	public static final String WEBSOCKET_PATH_PROPERTY = "ws.path";
	public static final String WEBSOCKET_PATH_DEFAULT = "kurento-tree";

	public static final String KMSS_URI_PROPERTY = "kms.uri";
	public static final String KMSS_URI_DEFAULT = "ws://localhost:8888/kurento";

	@Bean
	public RoomManager roomManager() {
		return new RoomManager(KurentoClient.create(getProperty(
				KMSS_URI_PROPERTY, KMSS_URI_DEFAULT)));
	}

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addHandler(clientsJsonRpcHandler(),
				getProperty(WEBSOCKET_PATH_PROPERTY, WEBSOCKET_PATH_DEFAULT));
	}

	@Bean
	public RoomManagerJsonRpcHandler clientsJsonRpcHandler() {
		return new RoomManagerJsonRpcHandler(roomManager());
	}

	@Bean
	public UserRegistry userRegistry() {
		return new UserRegistry();
	}

	public static ConfigurableApplicationContext start() {

		ConfigFileManager.loadConfigFile("kurento-room.conf.json");

		if (getProperty(UNSECURE_RANDOM_PROPERTY, UNSECURE_RANDOM_DEFAULT)) {
			log.info("Using /dev/urandom for secure random generation");
			System.setProperty("java.security.egd", "file:/dev/./urandom");
		}

		String port = getProperty(WEBSOCKET_PORT_PROPERTY,
				WEBSOCKET_PORT_DEFAULT);

		SpringApplication application = new SpringApplication(
				KurentoRoomServerApp.class);

		Properties properties = new Properties();
		properties.put("server.port", port);
		application.setDefaultProperties(properties);

		return application.run();
	}

	public static void main(String[] args) throws Exception {
		start();
	}

}
