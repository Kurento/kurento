package org.kurento.jsonrpc.test.base;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.kurento.jsonrpc.test.AsyncServerTest;
import org.kurento.jsonrpc.test.BidirectionalMultiTest;
import org.kurento.jsonrpc.test.BidirectionalTest;
import org.kurento.jsonrpc.test.CloseSessionTest;
import org.kurento.jsonrpc.test.ErrorServerTest;
import org.kurento.jsonrpc.test.MultipleSessionsTest;
import org.kurento.jsonrpc.test.NewSessionTest;
import org.kurento.jsonrpc.test.NotificationTest;
import org.kurento.jsonrpc.test.ReconnectionTest;
import org.kurento.jsonrpc.test.ServerEventsTest;
import org.kurento.jsonrpc.test.handler.EchoJsonRpcHandler;

@Configuration
@ComponentScan(basePackageClasses = { org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration.class })
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
