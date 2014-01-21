package com.kurento.kmf.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.internal.ThriftConnectorApplicationContextConfiguration;

@Configuration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class,
		ThriftConnectorApplicationContextConfiguration.class })
@EnableAutoConfiguration
public class BootApplication implements JsonRpcConfigurer {

	private static final Logger LOG = LoggerFactory
			.getLogger(BootApplication.class);

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addHandler(thriftConnectorJsonRpcHandler(), "/thrift");
	}

	@Bean
	public ThriftConnectorJsonRpcHandler thriftConnectorJsonRpcHandler() {
		return new ThriftConnectorJsonRpcHandler();
	}

	@Bean
	public ThriftConnectorConfiguration thriftConnectorConfiguration() {

		ThriftConnectorConfiguration config = new ThriftConnectorConfiguration();
		config.setHandlerAddress(getProperty("handler.address", "193.147.51.4"));
		config.setHandlerPort(getProperty("handler.port", 9999));

		LOG.info("Handler Address:" + config.getHandlerAddress());
		LOG.info("Handler Port:" + config.getHandlerPort());

		return config;
	}

	@Bean
	public ThriftInterfaceConfiguration thriftInterfaceConfiguration() {

		ThriftInterfaceConfiguration config = new ThriftInterfaceConfiguration();
		config.setServerAddress(getProperty("mediaserver.address",
				"193.147.51.29"));
		config.setServerPort(getProperty("mediaserver.port", 9090));

		LOG.info("Server Address:" + config.getServerAddress());
		LOG.info("Server Port:" + config.getServerPort());

		return config;
	}

	private String getProperty(String property, String defaultValue) {
		String value = System.getProperty(property);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	private int getProperty(String property, int defaultValue) {
		String value = System.getProperty(property);
		if (value == null) {
			return defaultValue;
		}
		return Integer.parseInt(value);
	}

}
