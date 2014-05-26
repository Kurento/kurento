package com.kurento.kmf.connector;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import com.kurento.kmf.common.PropertiesManager;
import com.kurento.kmf.common.PropertiesManager.PropertyHolder;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcProperties;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import com.kurento.kmf.media.factory.KmfMediaApi;

@Configuration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class })
@EnableAutoConfiguration
public class MediaConnectorApp implements JsonRpcConfigurer {

	private static final Logger log = LoggerFactory
			.getLogger(MediaConnectorApp.class);

	private static JsonRpcClient client;

	public static void setJsonRpcClient(JsonRpcClient client) {
		MediaConnectorApp.client = client;
	}

	@Autowired
	private Environment env;

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
	public ThriftConnectorJsonRpcHandler2 thriftConnectorJsonRpcHandler() {
		return new ThriftConnectorJsonRpcHandler2();
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
