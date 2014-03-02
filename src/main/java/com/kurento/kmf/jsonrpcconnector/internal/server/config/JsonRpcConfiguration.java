/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.jsonrpcconnector.internal.server.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.tomcat.websocket.server.WsSci;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.internal.http.JsonRpcHttpRequestHandler;
import com.kurento.kmf.jsonrpcconnector.internal.server.PerSessionJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.internal.server.ProtocolManager;
import com.kurento.kmf.jsonrpcconnector.internal.server.SessionsManager;
import com.kurento.kmf.jsonrpcconnector.internal.ws.JsonRpcWebSocketHandler;
import com.kurento.kmf.jsonrpcconnector.server.JsonRpcConfigurer;
import com.kurento.kmf.spring.KurentoServletContextListener;

@Configuration
@EnableWebSocket
public class JsonRpcConfiguration implements WebSocketConfigurer {

	public static final boolean INJECT_SESSION_ID = true;

	public static final String X_AUTH_HEADER = "X-Auth-Token";

	@Autowired
	protected ApplicationContext ctx;

	private final List<JsonRpcConfigurer> configurers = new ArrayList<>();
	private DefaultJsonRpcHandlerRegistry registry;

	private DefaultJsonRpcHandlerRegistry getJsonRpcHandlersRegistry() {
		if (registry == null) {
			registry = new DefaultJsonRpcHandlerRegistry();
			for (JsonRpcConfigurer configurer : this.configurers) {
				configurer.registerJsonRpcHandlers(registry);
			}
		}
		return registry;
	}

	@Autowired(required = false)
	public void setConfigurers(List<JsonRpcConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addAll(configurers);
		}
	}

	@Configuration
	@AutoConfigureAfter({ JsonRpcConfiguration.class })
	protected static class ApplicationContextFilterConfiguration {

		@Autowired
		private JsonRpcProperties props;

		@Bean
		public Filter applicationContextOAuthFilter(ApplicationContext context) {
			return new OncePerRequestFilter() {
				@Override
				protected void doFilterInternal(HttpServletRequest request,
						HttpServletResponse response, FilterChain filterChain)
						throws ServletException, IOException {

					if (!"".equals(props.getKeystoneHost())) {
						String accessToken = request.getHeader(X_AUTH_HEADER);
						String kmfAuthToken = props.getAuthToken();

						String url = props.getKeystoneHost() + ':'
								+ props.getKeystonePort()
								+ props.getKeystonePath() + accessToken;

						OAuthRequest oauthReq = new OAuthRequest(Verb.GET, url);
						oauthReq.addHeader(X_AUTH_HEADER, kmfAuthToken);

						Response oauthResp = oauthReq.send();
						if (!oauthResp.isSuccessful()) {
							response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
							return;
						}
					}

					filterChain.doFilter(request, response);
				}
			};
		}
	}

	@Bean
	public JsonRpcProperties jsonRpcProperties() {
		return new JsonRpcProperties();
	}

	// ---------------- HttpRequestHandlers -------------

	@Bean
	public HandlerMapping jsonRpcHandlerMapping() {

		DefaultJsonRpcHandlerRegistry registry = getJsonRpcHandlersRegistry();

		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();

		for (DefaultJsonRpcHandlerRegistration registration : registry
				.getRegistrations()) {

			for (Entry<JsonRpcHandler<?>, List<String>> e : registration
					.getHandlerMap().entrySet()) {

				JsonRpcHandler<?> handler = e.getKey();
				List<String> paths = e.getValue();
				putHandlersMappings(urlMap, handler, paths);
			}

			for (Entry<String, List<String>> e : registration
					.getPerSessionHandlerBeanNameMap().entrySet()) {

				String handlerBeanName = e.getKey();
				JsonRpcHandler<?> handler = (JsonRpcHandler<?>) ctx.getBean(
						"perSessionJsonRpcHandler", handlerBeanName, null);
				List<String> paths = e.getValue();
				putHandlersMappings(urlMap, handler, paths);
			}

			for (Entry<Class<? extends JsonRpcHandler<?>>, List<String>> e : registration
					.getPerSessionHandlerClassMap().entrySet()) {

				Class<? extends JsonRpcHandler<?>> handlerClass = e.getKey();
				JsonRpcHandler<?> handler = (JsonRpcHandler<?>) ctx.getBean(
						"perSessionJsonRpcHandler", null, handlerClass);
				List<String> paths = e.getValue();
				putHandlersMappings(urlMap, handler, paths);
			}
		}

		SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
		hm.setUrlMap(urlMap);
		hm.setOrder(1);
		return hm;
	}

	private void putHandlersMappings(Map<String, Object> urlMap,
			JsonRpcHandler<?> handler, List<String> paths) {

		JsonRpcHttpRequestHandler requestHandler = new JsonRpcHttpRequestHandler(
				(ProtocolManager) ctx.getBean("protocolManager", handler));

		for (String path : paths) {
			urlMap.put(path, requestHandler);
		}
	}

	// ---------------- Websockets -------------------

	@Override
	public void registerWebSocketHandlers(
			WebSocketHandlerRegistry wsHandlerRegistry) {

		DefaultJsonRpcHandlerRegistry registry = getJsonRpcHandlersRegistry();

		for (DefaultJsonRpcHandlerRegistration registration : registry
				.getRegistrations()) {

			for (Entry<JsonRpcHandler<?>, List<String>> e : registration
					.getHandlerMap().entrySet()) {

				JsonRpcHandler<?> handler = e.getKey();
				List<String> paths = e.getValue();

				publishWebSocketEndpoint(wsHandlerRegistry, handler, paths);
			}

			for (Entry<String, List<String>> e : registration
					.getPerSessionHandlerBeanNameMap().entrySet()) {

				String handlerBeanName = e.getKey();
				JsonRpcHandler<?> handler = (JsonRpcHandler<?>) ctx.getBean(
						"perSessionJsonRpcHandler", handlerBeanName, null);
				List<String> paths = e.getValue();

				publishWebSocketEndpoint(wsHandlerRegistry, handler, paths);
			}

			for (Entry<Class<? extends JsonRpcHandler<?>>, List<String>> e : registration
					.getPerSessionHandlerClassMap().entrySet()) {

				Class<? extends JsonRpcHandler<?>> handlerClass = e.getKey();
				JsonRpcHandler<?> handler = (JsonRpcHandler<?>) ctx.getBean(
						"perSessionJsonRpcHandler", null, handlerClass);
				List<String> paths = e.getValue();

				publishWebSocketEndpoint(wsHandlerRegistry, handler, paths);
			}

		}
	}

	private void publishWebSocketEndpoint(
			WebSocketHandlerRegistry wsHandlerRegistry,
			JsonRpcHandler<?> handler, List<String> paths) {

		JsonRpcWebSocketHandler wsHandler = new JsonRpcWebSocketHandler(
				(ProtocolManager) ctx.getBean("protocolManager", handler));

		for (String path : paths) {

			wsHandlerRegistry.addHandler(wsHandler, path + "/ws").withSockJS();
		}
	}

	// This methods workaround the bug
	// https://jira.springsource.org/browse/SPR-10841

	@Bean
	public TomcatEmbeddedServletContainerFactory tomcatContainerFactory() {
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		factory.setTomcatContextCustomizers(Arrays
				.asList(new TomcatContextCustomizer[] { tomcatContextCustomizer() }));
		return factory;
	}

	@Bean
	public TomcatContextCustomizer tomcatContextCustomizer() {
		return new TomcatContextCustomizer() {
			@Override
			public void customize(Context context) {
				context.addServletContainerInitializer(new WsSci(), null);
			}
		};
	}

	// ----------------------- Components ------------------------

	@Bean
	public SessionsManager sessionsManager() {
		return new SessionsManager();
	}

	@Bean
	@Scope("prototype")
	public ProtocolManager protocolManager(JsonRpcHandler<?> key) {
		return new ProtocolManager(key);
	}

	@Bean
	@Scope("prototype")
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PerSessionJsonRpcHandler<?> perSessionJsonRpcHandler(
			String beanName, Class<? extends JsonRpcHandler<?>> beanClass) {
		return new PerSessionJsonRpcHandler(beanName, beanClass);
	}

	@Bean(destroyMethod = "shutdown")
	public TaskScheduler jsonrpcTaskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	// ----------------------- Kurento context ---------------------

	@Bean
	public ServletListenerRegistrationBean<KurentoServletContextListener> kurentoServletContextListenerRegistrationBean() {
		return new ServletListenerRegistrationBean<KurentoServletContextListener>(
				new KurentoServletContextListener());
	}

	@Bean
	KurentoContextInitializer kurentoContextInitializer() {
		return new KurentoContextInitializer();
	}
}
