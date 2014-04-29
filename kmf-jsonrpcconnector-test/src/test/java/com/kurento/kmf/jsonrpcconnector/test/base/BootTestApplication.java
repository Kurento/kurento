package com.kurento.kmf.jsonrpcconnector.test.base;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;
import com.kurento.kmf.jsonrpcconnector.test.AsyncServerTest;
import com.kurento.kmf.jsonrpcconnector.test.BidirectionalMultiTest;
import com.kurento.kmf.jsonrpcconnector.test.BidirectionalTest;
import com.kurento.kmf.jsonrpcconnector.test.CloseSessionTest;
import com.kurento.kmf.jsonrpcconnector.test.ErrorServerTest;
import com.kurento.kmf.jsonrpcconnector.test.MultipleSessionsTest;
import com.kurento.kmf.jsonrpcconnector.test.NewSessionTest;
import com.kurento.kmf.jsonrpcconnector.test.NotificationTest;
import com.kurento.kmf.jsonrpcconnector.test.ReconnectionTest;
import com.kurento.kmf.jsonrpcconnector.test.ServerEventsTest;
import com.kurento.kmf.jsonrpcconnector.test.handler.EchoJsonRpcHandler;

@Configuration
@ComponentScan(basePackageClasses = { com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration.class })
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
