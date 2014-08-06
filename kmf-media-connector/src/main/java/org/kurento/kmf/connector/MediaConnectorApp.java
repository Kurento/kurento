package org.kurento.kmf.connector;

import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
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

import org.kurento.kmf.common.PropertiesManager;
import org.kurento.kmf.common.PropertiesManager.PropertyHolder;
import org.kurento.kmf.common.exception.KurentoException;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import org.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcProperties;
import org.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import org.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import org.kurento.kmf.media.factory.KmfMediaApi;

@Configuration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class })
@EnableAutoConfiguration
public class MediaConnectorApp implements JsonRpcConfigurer {

	private static final Logger log = LoggerFactory
			.getLogger(MediaConnectorApp.class);

	private static JsonRpcClient client;

	@Autowired
	private Environment env;

	public static void setJsonRpcClient(JsonRpcClient client) {
		MediaConnectorApp.client = client;
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
			public void customize(
					ConfigurableEmbeddedServletContainer container) {

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
		registry.addHandler(thriftConnectorJsonRpcHandler(), "/thrift");
	}

	@Bean
	public JsonRpcProperties jsonRpcProperties() {
		JsonRpcProperties configuration = new JsonRpcProperties();
		// Official FI-WARE OAuth server: http://cloud.lab.fi-ware.org
		configuration.setKeystoneHost(env.getProperty("oauthserver.url", ""));
		return configuration;
	}

	@Bean
	public MediaConnectorJsonRpcHandler thriftConnectorJsonRpcHandler() {
		return new MediaConnectorJsonRpcHandler();
	}

	@Bean
	public JsonRpcClient client() {

		if (client != null) {
			return client;
		} else {
			return KmfMediaApi.createJsonRpcClientFromSystemProperties();
		}
	}

	public static void main(String[] args) throws Exception {

		SpringApplication application = new SpringApplication(
				MediaConnectorApp.class);

		application.run(args);
	}

}
