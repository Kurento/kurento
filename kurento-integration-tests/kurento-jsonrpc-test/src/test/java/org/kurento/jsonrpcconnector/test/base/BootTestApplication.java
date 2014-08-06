package org.kurento.jsonrpcconnector.test.base;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.kurento.jsonrpcconnector.JsonRpcHandler;
import org.kurento.jsonrpcconnector.server.JsonRpcConfigurer;
import org.kurento.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import org.kurento.jsonrpcconnector.test.AsyncServerTest;
import org.kurento.jsonrpcconnector.test.BidirectionalMultiTest;
import org.kurento.jsonrpcconnector.test.BidirectionalTest;
import org.kurento.jsonrpcconnector.test.CloseSessionTest;
import org.kurento.jsonrpcconnector.test.ErrorServerTest;
import org.kurento.jsonrpcconnector.test.MultipleSessionsTest;
import org.kurento.jsonrpcconnector.test.NewSessionTest;
import org.kurento.jsonrpcconnector.test.NotificationTest;
import org.kurento.jsonrpcconnector.test.ReconnectionTest;
import org.kurento.jsonrpcconnector.test.ServerEventsTest;
import org.kurento.jsonrpcconnector.test.handler.EchoJsonRpcHandler;

@Configuration
@ComponentScan(basePackageClasses = { org.kurento.jsonrpcconnector.internal.server.config.JsonRpcConfiguration.class })
@EnableAutoConfiguration
public class BootTestApplication implements JsonRpcConfigurer {

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {

		registry.addHandler(echoJsonRpcHandler(), "/jsonrpc");

		registry.addHandler(new BidirectionalTest.Handler(), "/jsonrpcreverse");
		
		registry.addHandler(new BidirectionalMultiTest.Handler(), "/BidirectionalMultiTest");

		registry.addHandler(new ServerEventsTest.Handler(), "/serverevents");

		registry.addHandler(new AsyncServerTest.Handler(), "/async_handler");

		registry.addHandler(new ErrorServerTest.Handler(), "/error_handler");

		registry.addPerSessionHandler(MultipleSessionsTest.Handler.class,
				"/jsonrpc_multiple");

		registry.addPerSessionHandler("multipleJsonRpcHandler",
				"/jsonrpc_multiple2");

		registry.addHandler(new NewSessionTest.Handler(),
				"/new_session_handler");

		registry.addHandler(new CloseSessionTest.Handler(),
				"/close_session_handler");

		registry.addHandler(new ReconnectionTest.Handler(), "/reconnection");

		registry.addHandler(new NotificationTest.Handler(), "/notification");

	}

	@Bean
	@Scope("prototype")
	public MultipleSessionsTest.Handler multipleJsonRpcHandler() {
		return new MultipleSessionsTest.Handler();
	}

	@Bean
	public DemoBean demoBean() {
		return new DemoBean();
	}

	@Bean
	public JsonRpcHandler<?> echoJsonRpcHandler() {
		return new EchoJsonRpcHandler();
	}

}
