package com.kurento.kmf.phone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.internal.ThriftConnectorApplicationContextConfiguration;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class,
		MediaPipelineFactory.class,
		ThriftConnectorApplicationContextConfiguration.class })
public class PhoneApp implements JsonRpcConfigurer {

	private static final Logger LOG = LoggerFactory.getLogger(PhoneApp.class);

	@Autowired
	private Environment env;

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addPerSessionHandler(PhoneHandler.class, "/phone");
	}

	@Bean
	public ThriftConnectorConfiguration thriftConnectorConfiguration() {

		ThriftConnectorConfiguration config = new ThriftConnectorConfiguration();
		config.setHandlerAddress(env
				.getProperty("handler.address", "127.0.0.1"));
		config.setHandlerPort(Integer.parseInt(env.getProperty("handler.port",
				"9900")));

		LOG.info("Using Handler Address:"
				+ config.getHandlerAddress()
				+ " (This address is used from Kurento Media Server to connect to this proxy)");
		LOG.info("Using Handler Port:"
				+ config.getHandlerPort()
				+ " (This port is used from Kurento Media Server to connect to this proxy)");

		return config;
	}

	@Bean
	public ThriftInterfaceConfiguration thriftInterfaceConfiguration() {

		ThriftInterfaceConfiguration config = new ThriftInterfaceConfiguration();
		config.setServerAddress(env.getProperty("mediaserver.address",
				"127.0.0.1"));
		config.setServerPort(Integer.parseInt(env.getProperty(
				"mediaserver.port", "9090")));

		LOG.info("Using Kurento Media Server Address:"
				+ config.getServerAddress()
				+ " (This address is used from this proxy to connect to Kurento Media Server)");
		LOG.info("Using Kurento Media Server Port:"
				+ config.getServerPort()
				+ " (This port is used from this proxy to connect to Kurento Media Server)");

		return config;
	}

	@Bean
	@Scope("prototype")
	public PhoneHandler multipleJsonRpcHandler() {
		return new PhoneHandler();
	}

	@Bean
	public MediaApiConfiguration mediaApiConfiguration() {
		return new MediaApiConfiguration();
	}

	@Bean
	public Registry registry() {
		return new Registry();
	}

	public static void main(String[] args) throws Exception {

		SpringApplication application = new SpringApplication(PhoneApp.class);
		application.run(args);
	}
}
