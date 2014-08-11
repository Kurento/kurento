package org.kurento.control.server;

import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.kurento.client.factory.KurentoClientFactory;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.PropertiesManager.PropertyHolder;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.internal.server.config.JsonRpcProperties;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class })
@EnableAutoConfiguration
public class KurentoControlServerApp implements JsonRpcConfigurer {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoControlServerApp.class);

	private static JsonRpcClient client;

	@Autowired
	private Environment env;

	public static void setJsonRpcClient(JsonRpcClient client) {
		KurentoControlServerApp.client = client;
	}

	@Bean
	public EmbeddedServletContainerCustomizer containerCustomizer()
			throws Exception {

		final String keystoreFile = env.getProperty("keystore.file");

		if (keystoreFile == null) {
			return createNoCustomizationCustomizer();
		}

		final String keystorePass = env.getProperty("keystore.pass");
		if (keystorePass == null) {
			throw new KurentoException(
					"Property 'keystore.pass' is mandatory with keystore.file");
		}

		int httpsPort = 8443;
		try {
			String httpsPortAsStr = env.getProperty("server.port");
			if (httpsPortAsStr != null) {
				httpsPort = Integer.parseInt(httpsPortAsStr);
			}
		} catch (NumberFormatException e) {
			log.warn("Property 'server.port' can't be parsed as string. Error: "
					+ e.getMessage() + ". Defaulting to port 8443");
		}

		log.debug("Starting KMC with secure sockets in port " + httpsPort);

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

	@PostConstruct
	public void initPropertyManager() {
		PropertiesManager.setPropertyHolder(new PropertyHolder() {
			@Override
			public String getProperty(String property) {
				return env.getProperty(property);
			}
		});
	}

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addHandler(thriftConnectorJsonRpcHandler(), "/kurento");
	}

	@Bean
	public JsonRpcProperties jsonRpcProperties() {
		JsonRpcProperties configuration = new JsonRpcProperties();
		// Official FI-WARE OAuth server: http://cloud.lab.fi-ware.org
		configuration.setKeystoneHost(env.getProperty("oauthserver.url", ""));
		return configuration;
	}

	@Bean
	public JsonRpcHandler thriftConnectorJsonRpcHandler() {
		return new JsonRpcHandler();
	}

	@Bean
	public JsonRpcClient client() {

		if (client != null) {
			return client;
		} else {
			return KurentoClientFactory.createJsonRpcClient();
		}
	}

	public static void main(String[] args) throws Exception {

		SpringApplication application = new SpringApplication(
				KurentoControlServerApp.class);

		application.run(args);
	}

}
