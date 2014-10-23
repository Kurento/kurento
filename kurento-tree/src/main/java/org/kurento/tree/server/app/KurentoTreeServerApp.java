package org.kurento.tree.server.app;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.commons.PropertiesManager.getPropertyJson;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.kurento.commons.ConfigFileManager;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.kurento.tree.server.kmsmanager.FixedNRealKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.AotFixedClientsNoRootTreeManager;
import org.kurento.tree.server.treemanager.TreeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.google.gson.JsonArray;

@Configuration
@Import(JsonRpcConfiguration.class)
@EnableAutoConfiguration
public class KurentoTreeServerApp implements JsonRpcConfigurer {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoTreeServerApp.class);

	public static final String WEBSOCKET_PORT_PROPERTY = "ws.port";
	public static final String WEBSOCKET_PORT_DEFAULT = "8890";

	public static final String WEBSOCKET_PATH_PROPERTY = "ws.path";
	public static final String WEBSOCKET_PATH_DEFAULT = "kurento-tree";

	public static final String KMSS_URIS_PROPERTY = "kms.uris";
	public static final String KMSS_URIS_DEFAULT = "[ \"ws://localhost:8888/kurento\" ]";

	public static TreeManager treeManager;

	@Bean
	public JsonRpcHandler jsonRpcHandler() {
		return new JsonRpcHandler(treeManager());
	}

	@Bean
	public TreeManager treeManager() {

		if (treeManager == null) {
			JsonArray kmsUris = getPropertyJson(KMSS_URIS_PROPERTY,
					KMSS_URIS_DEFAULT, JsonArray.class);
			List<String> kmsWsUris = JsonUtils.toStringList(kmsUris);

			log.info("Configuring Kurento Tree Server to use kmss: "
					+ kmsWsUris);

			KmsManager kmsManager;
			try {
				kmsManager = new FixedNRealKmsManager(kmsWsUris);
				treeManager = new AotFixedClientsNoRootTreeManager(kmsManager);
			} catch (IOException e) {
				log.error("Exception connecting to KMS", e);
				System.exit(1);
			}
		}

		return treeManager;
	}

	public static void setTreeManager(TreeManager treeManager) {
		KurentoTreeServerApp.treeManager = treeManager;
	}

	public static TreeManager getTreeManager() {
		return treeManager;
	}

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addHandler(jsonRpcHandler(), "/kurento-tree");
	}

	public static ConfigurableApplicationContext start() {

		ConfigFileManager.loadConfigFile("kurento-tree.conf.json");

		String port = getProperty(WEBSOCKET_PORT_PROPERTY,
				WEBSOCKET_PORT_DEFAULT);

		SpringApplication application = new SpringApplication(
				KurentoTreeServerApp.class);

		Properties properties = new Properties();
		properties.put("server.port", port);
		application.setDefaultProperties(properties);
		application.setHeadless(false);

		return application.run();
	}

	public static void main(String[] args) throws Exception {
		start();
	}

}
