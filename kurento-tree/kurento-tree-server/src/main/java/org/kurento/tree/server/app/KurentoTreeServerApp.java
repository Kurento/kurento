package org.kurento.tree.server.app;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.commons.PropertiesManager.getPropertyJson;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.kurento.commons.ConfigFileManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.kmsmanager.RealElasticKmsManager;
import org.kurento.tree.server.treemanager.LessLoadedElasticTM;
import org.kurento.tree.server.treemanager.OneKmsTM;
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

	private static final String UNSECURE_RANDOM_PROPERTY = "unsecureRandom";
	private static final boolean UNSECURE_RANDOM_DEFAULT = true;

	public static final String WEBSOCKET_PORT_PROPERTY = "ws.port";
	public static final String WEBSOCKET_PORT_DEFAULT = "8890";

	public static final String WEBSOCKET_PATH_PROPERTY = "ws.path";
	public static final String WEBSOCKET_PATH_DEFAULT = "kurento-tree";

	public static final String KMSS_URIS_PROPERTY = "kms.uris";
	public static final String KMSS_URIS_DEFAULT = "[ \"ws://localhost:8888/kurento\" ]";

	@Bean
	public KmsManager kmsManager() {

		try {

			JsonArray kmsUris = getPropertyJson(KMSS_URIS_PROPERTY,
					KMSS_URIS_DEFAULT, JsonArray.class);
			List<String> kmsWsUris = JsonUtils.toStringList(kmsUris);

			log.info("Configuring Kurento Tree Server to use kmss: "
					+ kmsWsUris);

			return new RealElasticKmsManager(kmsWsUris);

		} catch (IOException e) {
			throw new KurentoException(e);
		}
	}

	@Bean
	public TreeManager treeManager() {

		KmsManager kmsManager = kmsManager();

		if (kmsManager.getKmss().size() == 1) {
			return new OneKmsTM(kmsManager);
		} else {
			return new LessLoadedElasticTM(kmsManager);
		}
	}

	public KmsRegistrar registrar() {
		KmsManager kmsManager = kmsManager();
		if (kmsManager instanceof KmsRegistrar) {
			return (KmsRegistrar) kmsManager;
		} else {
			log.warn("Kurento Tree server is using a DummyRegistrar. New KMSs will be ignored");
			return new DummyRegistrar();
		}
	}

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addHandler(clientsJsonRpcHandler(), "/kurento-tree");
		registry.addHandler(registrarJsonRpcHandler(), "/registrar");
	}

	@Bean
	public ClientsJsonRpcHandler clientsJsonRpcHandler() {
		return new ClientsJsonRpcHandler(treeManager());
	}

	private RegistrarJsonRpcHandler registrarJsonRpcHandler() {
		return new RegistrarJsonRpcHandler(registrar());
	}

	public static ConfigurableApplicationContext start() {

		ConfigFileManager.loadConfigFile("kurento-tree.conf.json");

		if (getProperty(UNSECURE_RANDOM_PROPERTY, UNSECURE_RANDOM_DEFAULT)) {
			log.info("Using /dev/urandom for secure random generation");
			System.setProperty("java.security.egd", "file:/dev/./urandom");
		}

		String port = getProperty(WEBSOCKET_PORT_PROPERTY,
				WEBSOCKET_PORT_DEFAULT);

		SpringApplication application = new SpringApplication(
				KurentoTreeServerApp.class);

		Properties properties = new Properties();
		properties.put("server.port", port);
		application.setDefaultProperties(properties);

		return application.run();
	}

	public static void main(String[] args) throws Exception {
		start();
	}

}
