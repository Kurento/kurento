package org.kurento.kmf.demo.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import org.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import org.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import org.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import org.kurento.kmf.media.factory.KmfMediaApi;
import org.kurento.kmf.media.factory.MediaPipelineFactory;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = { JsonRpcConfiguration.class })
public class GroupCall implements JsonRpcConfigurer {

	@Autowired
	private Environment env;

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addPerSessionHandler(ParticipantHandler.class, "/groupcall");
	}

	@Bean
	@Scope("prototype")
	public ParticipantHandler multipleJsonRpcHandler() {
		return new ParticipantHandler();
	}

	@Bean
	public RoomManager roomManager() {
		return new RoomManager();
	}

	@Bean
	MediaPipelineFactory mediaPipelineFactory() {
		return KmfMediaApi.createMediaPipelineFactoryFromSystemProps();
	}

	public static void main(String[] args) throws Exception {

		SpringApplication application = new SpringApplication(GroupCall.class);
		application.run(args);
	}
}
