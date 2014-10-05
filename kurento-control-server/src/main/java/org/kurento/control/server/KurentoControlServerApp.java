package org.kurento.control.server;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.commons.PropertiesManager.getPropertyOrException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.kurento.commons.ConfigFileFinder;
import org.kurento.commons.ConfigFilePropertyHolder;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.internal.server.config.JsonRpcProperties;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class })
@EnableAutoConfiguration
public class KurentoControlServerApp implements JsonRpcConfigurer {

	private static final String CONFIG_FILE_PATH_PROPERTY = "configFilePath";

	private static final String OAUTHSERVER_URL_PROPERTY = "controlServer.oauthserverUrl";
	private static final String OAUTHSERVER_URL_DEFAULT = "";

	public static final String WEBSOCKET_PORT_PROPERTY = "controlServer.net.websocket.port";
	public static final String WEBSOCKET_PORT_DEFAULT = "8888";

	public static final String WEBSOCKET_PATH_PROPERTY = "controlServer.net.websocket.path";
	public static final String WEBSOCKET_PATH_DEFAULT = "kurento";

	public static final String KEYSTORE_PASS_PROPERTY = "controlServer.net.websocket.keystore.pass";
	public static final String KEYSTORE_FILE_PROPERTY = "controlServer.net.websocket.keystore.path";

	public static final String WEBSOCKET_SECURE_PORT_PROPERTY = "controlServer.net.websocket.securePort";

	private static final String UNSECURE_RANDOM_PROPERTY = "controlServer.unsecureRandom";
	private static final boolean UNSECURE_RANDOM_DEFAULT = false;

	private static final String LOG_CONFIG_FILE_PROPERTY = "controlServer.logConfigFile";
	private static final String LOG_CONFIG_FILE_DEFAULT = null;

	private static final Logger log = LoggerFactory
			.getLogger(KurentoControlServerApp.class);

	private static JsonRpcClient client;

	public static void setJsonRpcClient(JsonRpcClient client) {
		KurentoControlServerApp.client = client;
	}

	@Bean
	public JsonRpcClient client() {

		if (client != null) {
			return client;
		} else {
			return KmsConnectionHelper.createJsonRpcClient();
		}
	}

	@Bean
	public EmbeddedServletContainerCustomizer containerCustomizer()
			throws Exception {

		final String securePort = getProperty(WEBSOCKET_SECURE_PORT_PROPERTY);

		if (securePort == null) {
			return createNoCustomizationCustomizer();
		}

		int httpsPort = 8443;
		try {
			httpsPort = Integer.parseInt(securePort);
		} catch (NumberFormatException e) {
			log.warn("Property '" + WEBSOCKET_PORT_PROPERTY
					+ "' can't be parsed as integer. Error: " + e.getMessage()
					+ ". Defaulting to port 8443");
		}

		final String keystoreFile = getPropertyOrException(
				KEYSTORE_FILE_PROPERTY, "Property '" + KEYSTORE_FILE_PROPERTY
						+ "' is mandatory with '"
						+ WEBSOCKET_SECURE_PORT_PROPERTY + "'");

		final String keystorePass = getPropertyOrException(
				KEYSTORE_PASS_PROPERTY, "Property '" + KEYSTORE_PASS_PROPERTY
						+ "' is mandatory with '"
						+ WEBSOCKET_SECURE_PORT_PROPERTY + "'");

		log.info("Starting Kurento Control Server with secure websockets (wss) in port: "
				+ httpsPort);

		return createTomcatCustomizer(keystoreFile, keystorePass, httpsPort);
	}

	private EmbeddedServletContainerCustomizer createNoCustomizationCustomizer() {
		return new EmbeddedServletContainerCustomizer() {
			@Override
			public void customize(ConfigurableEmbeddedServletContainer container) {
			}
		};
	}

	private EmbeddedServletContainerCustomizer createTomcatCustomizer(
			final String keystoreFile, final String keystorePass,
			final int httpsPort) {

		return new EmbeddedServletContainerCustomizer() {
			@Override
			public void customize(ConfigurableEmbeddedServletContainer container) {

				TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;

				tomcat.addConnectorCustomizers(new TomcatConnectorCustomizer() {

					@Override
					public void customize(Connector connector) {

						log.debug("Customizing Tomcat to setup https");
						log.debug("Https port: " + httpsPort);
						log.debug("Keystore: " + keystoreFile);

						connector.setPort(httpsPort);
						connector.setSecure(true);
						connector.setScheme("https");

						connector.setAttribute("clientAuth", "false");
						connector.setAttribute("sslProtocol", "TLS");
						connector.setAttribute("SSLEnabled", true);

						Http11NioProtocol proto = (Http11NioProtocol) connector
								.getProtocolHandler();
						proto.setSSLEnabled(true);
						proto.setKeystoreFile(Paths.get(keystoreFile)
								.toAbsolutePath().toString());
						proto.setKeystorePass(keystorePass);
						proto.setKeystoreType("PKCS12");
						proto.setKeyAlias("tomcat");

					}
				});
			}
		};
	}

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		String path = getProperty(WEBSOCKET_PATH_PROPERTY,
				WEBSOCKET_PATH_DEFAULT);

		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		registry.addHandler(jsonRpcHandler(), "/" + path);
		registry.addHandler(jsonRpcHandler(), "/" + path + "/");
	}

	@Bean
	public JsonRpcProperties jsonRpcProperties() {
		JsonRpcProperties configuration = new JsonRpcProperties();

		configuration.setKeystoneHost(getProperty(OAUTHSERVER_URL_PROPERTY,
				OAUTHSERVER_URL_DEFAULT));

		return configuration;
	}

	@Bean
	public JsonRpcHandler jsonRpcHandler() {
		return new JsonRpcHandler();
	}

	public static void main(String[] args) throws Exception {
		start();
	}

	public static ConfigurableApplicationContext start() {

		loadConfigFile();

		if (getProperty(UNSECURE_RANDOM_PROPERTY, UNSECURE_RANDOM_DEFAULT)) {
			log.info("Using /dev/urandom for secure random generation");
			System.setProperty("java.security.egd", "file:/dev/./urandom");
		}

		String logFile = getProperty(LOG_CONFIG_FILE_PROPERTY,
				LOG_CONFIG_FILE_DEFAULT);

		if (logFile != null) {
			log.info("Using logback file " + logFile);
			System.setProperty("logback.configurationFile", logFile);
		}

		String port = getProperty(WEBSOCKET_PORT_PROPERTY,
				WEBSOCKET_PORT_DEFAULT);

		SpringApplication application = new SpringApplication(
				KurentoControlServerApp.class);

		Properties properties = new Properties();
		properties.put("server.port", port);
		application.setDefaultProperties(properties);

		return application.run();
	}

	private static void loadConfigFile() {

		try {

			String configFilePath = System
					.getProperty(CONFIG_FILE_PATH_PROPERTY);

			Path configFile = null;

			if (configFilePath != null) {
				configFile = Paths.get(configFilePath);
			} else {
				configFile = ConfigFileFinder.searchDefaultConfigFile();
			}

			if (configFile != null && Files.exists(configFile)) {
				ConfigFilePropertyHolder
						.configurePropertiesFromConfigFile(configFile);
			} else {
				log.warn("Config file not found. Using all default values");
			}

		} catch (IOException e) {
			log.warn("Exception loading config file", e);
		}
	}
}
