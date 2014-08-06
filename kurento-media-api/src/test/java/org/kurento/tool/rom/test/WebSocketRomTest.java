package org.kurento.tool.rom.test;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.kurento.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.jsonrpcconnector.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpcconnector.server.JsonRpcConfigurer;
import org.kurento.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public class WebSocketRomTest extends AbstractRomTest {

	private static RomServerJsonRpcHandler handler;

	@Configuration
	@ComponentScan(basePackageClasses = JsonRpcConfiguration.class)
	@EnableAutoConfiguration
	public static class BootTestApplication implements JsonRpcConfigurer {

		@Override
		public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
			registry.addHandler(handler, "/handler");
		}
	}

	protected ConfigurableApplicationContext context;

	@Override
	protected JsonRpcClient createJsonRpcClient() {
		return new JsonRpcClientWebSocket("ws://localhost:" + getPort()
				+ "/handler");
	}

	@Override
	protected void startJsonRpcServer(RomServerJsonRpcHandler jsonRpcHandler) {

		handler = jsonRpcHandler;

		Properties properties = new Properties();
		properties.put("server.port", getPort());

		SpringApplication application = new SpringApplication(
				BootTestApplication.class);
		application.setDefaultProperties(properties);

		context = application.run();
	}

	protected static String getPort() {
		String port = System.getProperty("http.port");
		if (port == null) {
			port = "7788";
		}
		return port;
	}

	@Override
	protected void destroyJsonRpcServer() {
		context.close();
	}
}
