/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kurento.kmf.jsonrpcconnector.internal.server.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistration;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcHandlerRegistry;

/**
 * A {@link WebSocketHandlerRegistry} that maps {@link WebSocketHandler}s to
 * URLs for use in a Servlet container.
 * 
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultJsonRpcHandlerRegistry implements JsonRpcHandlerRegistry {

	private final List<DefaultJsonRpcHandlerRegistration> registrations = new ArrayList<>();

	@Override
	public JsonRpcHandlerRegistration addHandler(
			JsonRpcHandler<?> webSocketHandler, String... paths) {

		DefaultJsonRpcHandlerRegistration registration = new DefaultJsonRpcHandlerRegistration();
		registration.addHandler(webSocketHandler, paths);
		this.registrations.add(registration);
		return registration;
	}

	@Override
	public JsonRpcHandlerRegistration addPerSessionHandler(
			Class<? extends JsonRpcHandler<?>> handlerClass, String... paths) {

		DefaultJsonRpcHandlerRegistration registration = new DefaultJsonRpcHandlerRegistration();
		registration.addPerSessionHandler(handlerClass, paths);
		this.registrations.add(registration);
		return registration;

	}

	@Override
	public JsonRpcHandlerRegistration addPerSessionHandler(String beanName,
			String... paths) {

		DefaultJsonRpcHandlerRegistration registration = new DefaultJsonRpcHandlerRegistration();
		registration.addPerSessionHandler(beanName, paths);
		this.registrations.add(registration);
		return registration;

	}

	public List<DefaultJsonRpcHandlerRegistration> getRegistrations() {
		return registrations;
	}

}
