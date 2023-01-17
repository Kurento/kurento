/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.jsonrpc.internal.server.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.internal.http.JsonRpcHttpRequestHandler;
import org.kurento.jsonrpc.internal.server.PerSessionJsonRpcHandler;
import org.kurento.jsonrpc.internal.server.ProtocolManager;
import org.kurento.jsonrpc.internal.server.SessionsManager;
import org.kurento.jsonrpc.internal.ws.JsonRpcWebSocketHandler;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
public class JsonRpcConfiguration implements WebSocketConfigurer {

  @Autowired
  protected ApplicationContext ctx;

  private final List<JsonRpcConfigurer> configurers = new ArrayList<>();
  private DefaultJsonRpcHandlerRegistry instanceRegistry;

  private DefaultJsonRpcHandlerRegistry getJsonRpcHandlersRegistry() {
    if (instanceRegistry == null) {
      instanceRegistry = new DefaultJsonRpcHandlerRegistry();
      for (JsonRpcConfigurer configurer : this.configurers) {
        configurer.registerJsonRpcHandlers(instanceRegistry);
      }
    }
    return instanceRegistry;
  }

  @Autowired(required = false)
  public void setConfigurers(List<JsonRpcConfigurer> configurers) {
    if (!CollectionUtils.isEmpty(configurers)) {
      this.configurers.addAll(configurers);
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

    Map<String, Object> urlMap = new LinkedHashMap<>();

    for (DefaultJsonRpcHandlerRegistration registration : registry.getRegistrations()) {

      for (Entry<JsonRpcHandler<?>, List<String>> e : registration.getHandlerMap().entrySet()) {

        JsonRpcHandler<?> handler = e.getKey();
        List<String> paths = e.getValue();
        putHandlersMappings(urlMap, handler, paths);
      }

      for (Entry<String, List<String>> e : registration.getPerSessionHandlerBeanNameMap()
          .entrySet()) {

        String handlerBeanName = e.getKey();
        JsonRpcHandler<?> handler =
            (JsonRpcHandler<?>) ctx.getBean("perSessionJsonRpcHandler", handlerBeanName, null);
        List<String> paths = e.getValue();
        putHandlersMappings(urlMap, handler, paths);
      }

      for (Entry<Class<? extends JsonRpcHandler<?>>, List<String>> e : registration
          .getPerSessionHandlerClassMap().entrySet()) {

        Class<? extends JsonRpcHandler<?>> handlerClass = e.getKey();
        JsonRpcHandler<?> handler =
            (JsonRpcHandler<?>) ctx.getBean("perSessionJsonRpcHandler", null, handlerClass);
        List<String> paths = e.getValue();
        putHandlersMappings(urlMap, handler, paths);
      }
    }

    SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
    hm.setUrlMap(urlMap);
    hm.setOrder(1);
    return hm;
  }

  private void putHandlersMappings(Map<String, Object> urlMap, JsonRpcHandler<?> handler,
      List<String> paths) {

    JsonRpcHttpRequestHandler requestHandler =
        new JsonRpcHttpRequestHandler((ProtocolManager) ctx.getBean("protocolManager", handler));

    for (String path : paths) {
      urlMap.put(path, requestHandler);
    }
  }

  // ---------------- Websockets -------------------
  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry wsHandlerRegistry) {

    DefaultJsonRpcHandlerRegistry registry = getJsonRpcHandlersRegistry();

    for (DefaultJsonRpcHandlerRegistration registration : registry.getRegistrations()) {

      for (Entry<JsonRpcHandler<?>, List<String>> e : registration.getHandlerMap().entrySet()) {

        JsonRpcHandler<?> handler = e.getKey();
        List<String> paths = e.getValue();

        publishWebSocketEndpoint(wsHandlerRegistry, handler, paths);
      }

      for (Entry<String, List<String>> e : registration.getPerSessionHandlerBeanNameMap()
          .entrySet()) {

        String handlerBeanName = e.getKey();
        JsonRpcHandler<?> handler =
            (JsonRpcHandler<?>) ctx.getBean("perSessionJsonRpcHandler", handlerBeanName, null);
        List<String> paths = e.getValue();

        publishWebSocketEndpoint(wsHandlerRegistry, handler, paths);
      }

      for (Entry<Class<? extends JsonRpcHandler<?>>, List<String>> e : registration
          .getPerSessionHandlerClassMap().entrySet()) {

        Class<? extends JsonRpcHandler<?>> handlerClass = e.getKey();
        JsonRpcHandler<?> handler =
            (JsonRpcHandler<?>) ctx.getBean("perSessionJsonRpcHandler", null, handlerClass);
        List<String> paths = e.getValue();

        publishWebSocketEndpoint(wsHandlerRegistry, handler, paths);
      }

    }
  }

  private void publishWebSocketEndpoint(WebSocketHandlerRegistry wsHandlerRegistry,
      JsonRpcHandler<?> handler, List<String> paths) {

    ProtocolManager protocolManager = (ProtocolManager) ctx.getBean("protocolManager", handler);

    JsonRpcWebSocketHandler wsHandler = new JsonRpcWebSocketHandler(protocolManager);

    protocolManager.setPingWachdog(handler.isPingWatchdog());

    for (String path : paths) {

      WebSocketHandlerRegistration registration = wsHandlerRegistry.addHandler(wsHandler, path);

      List<String> origins = handler.allowedOrigins();
      registration.setAllowedOrigins(origins.toArray(new String[origins.size()]));

      if (handler.isSockJSEnabled()) {
        registration.withSockJS().setSessionCookieNeeded(false);
      }
    
      if (handler.getLabel() != null) {
        wsHandler.setLabel(handler.getLabel());
      }
      
      HandshakeInterceptor[] interceptors = new HandshakeInterceptor[handler.interceptors().size()];
      int i = 0;
      for (Object obj : handler.interceptors()) {
    	  if (obj instanceof HandshakeInterceptor) {
	    	  interceptors[i] = (HandshakeInterceptor) obj;
	    	  i++;
    	  }
      }
      registration.addInterceptors(interceptors);
      
    }
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
  public PerSessionJsonRpcHandler<?> perSessionJsonRpcHandler(String beanName,
      Class<? extends JsonRpcHandler<?>> beanClass) {
    return new PerSessionJsonRpcHandler(beanName, beanClass);
  }

  @Bean(destroyMethod = "shutdown")
  public TaskScheduler jsonrpcTaskScheduler() {
    return new ThreadPoolTaskScheduler();
  }

}
