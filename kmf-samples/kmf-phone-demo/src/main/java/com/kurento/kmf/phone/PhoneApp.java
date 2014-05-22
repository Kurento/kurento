package com.kurento.kmf.phone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import com.kurento.kmf.media.factory.KmfMediaApi;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

@Configuration
@EnableAutoConfiguration
@Import(JsonRpcConfiguration.class)
public class PhoneApp implements JsonRpcConfigurer {

	@Autowired
	private Environment env;

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addPerSessionHandler(PhoneHandler.class, "/phone");
	}

	@Bean
	@Scope("prototype")
	public PhoneHandler multipleJsonRpcHandler() {
		return new PhoneHandler();
	}

	@Bean
	public Registry registry() {
		return new Registry();
	}

	@Bean
	MediaPipelineFactory mediaPipelineFactory() {
		return KmfMediaApi.createMediaPipelineFactoryFromSystemProps();
	}

	public static void main(String[] args) throws Exception {

		SpringApplication application = new SpringApplication(PhoneApp.class);
		application.run(args);
	}
}
