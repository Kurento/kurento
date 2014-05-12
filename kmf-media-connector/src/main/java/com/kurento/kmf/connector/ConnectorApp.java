package com.kurento.kmf.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcProperties;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.internal.ThriftConnectorApplicationContextConfiguration;

@Configuration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class,
		ThriftConnectorApplicationContextConfiguration.class })
@EnableAutoConfiguration
public class ConnectorApp implements JsonRpcConfigurer {

	private static final Logger LOG = LoggerFactory
			.getLogger(ConnectorApp.class);

	@Autowired
	private Environment env;

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
	public ThriftConnectorJsonRpcHandler thriftConnectorJsonRpcHandler() {
		return new ThriftConnectorJsonRpcHandler();
	}

	@Bean
	public ThriftConnectorConfiguration thriftConnectorConfiguration() {

		ThriftConnectorConfiguration config = new ThriftConnectorConfiguration();
		config.setHandlerAddress(env
				.getProperty("handler.address", "127.0.0.1"));
		config.setHandlerPort(Integer.parseInt(env.getProperty("handler.port",
				"9900")));

		LOG.info(
				"Using Handler Address: {} (This address is used from Kurento Media Server to connect to this proxy)",
				config.getHandlerAddress());
		LOG.info(
				"Using Handler Port: {} (This port is used from Kurento Media Server to connect to this proxy)",
				Integer.valueOf(config.getHandlerPort()));

		return config;
	}

	@Bean
	public ThriftInterfaceConfiguration thriftInterfaceConfiguration() {

		ThriftInterfaceConfiguration config = new ThriftInterfaceConfiguration();
		config.setServerAddress(env.getProperty("mediaserver.address",
				"127.0.0.1"));
		config.setServerPort(Integer.parseInt(env.getProperty(
				"mediaserver.port", "9090")));

		LOG.info(
				"Using Kurento Media Server Address: {} (This address is used from this proxy to connect to Kurento Media Server)",
				config.getServerAddress());
		LOG.info(
				"Using Kurento Media Server Port:{} (This port is used from this proxy to connect to Kurento Media Server)",
				Integer.valueOf(config.getServerPort()));

		return config;
	}

	public static void main(String[] args) throws Exception {

		SpringApplication application = new SpringApplication(
				ConnectorApp.class);

		application.run(args);
	}
}
