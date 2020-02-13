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

package org.kurento.jsonrpc.internal.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

import com.google.common.collect.ImmutableList;

public class PerSessionJsonRpcHandler<T> implements JsonRpcHandler<T>, BeanFactoryAware {

  private static final Log logger = LogFactory.getLog(PerConnectionWebSocketHandler.class);

  private final BeanCreatingHelper<JsonRpcHandler<T>> provider;

  private final Map<Session, JsonRpcHandler<T>> handlers = new ConcurrentHashMap<>();

  private boolean useSockJS;

  private String label;

  private List<String> allowedOrigins = ImmutableList.of();
  
  private List<Object> interceptors = ImmutableList.of();

  private boolean pingWachdog;

  public PerSessionJsonRpcHandler(String handlerName) {
    this(handlerName, null);
  }

  public PerSessionJsonRpcHandler(Class<? extends JsonRpcHandler<T>> handlerType) {
    this(null, handlerType);
  }

  public PerSessionJsonRpcHandler(String handlerName,
      Class<? extends JsonRpcHandler<T>> handlerType) {
    this.provider = new BeanCreatingHelper<>(handlerType, handlerName);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends JsonRpcHandler<T>> getHandlerType() {
    Class<? extends JsonRpcHandler<T>> clazz =
        (Class<? extends JsonRpcHandler<T>>) provider.getCreatedBeanType();

    // FIXME this has to be done in order to obtain the type of T when the
    // bean is created from a name
    if (clazz == null) {
      this.provider.createBean();
      clazz = (Class<? extends JsonRpcHandler<T>>) provider.getCreatedBeanType();
    }

    return clazz;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.provider.setBeanFactory(beanFactory);
  }

  @Override
  public void handleRequest(Transaction transaction, Request<T> request) throws Exception {

    JsonRpcHandler<T> handler = getHandler(transaction.getSession());

    Assert.isTrue(handler != null, "Handler of class " + provider.getClass()
        + " can't be created. Be sure that there" + " is a bean registered of this type");

    try {
      handler.handleRequest(transaction, request);
    } catch (Exception e) {
      handler.handleUncaughtException(transaction.getSession(), e);
    }
  }

  private JsonRpcHandler<T> getHandler(Session session) {
    JsonRpcHandler<T> handler = this.handlers.get(session);
    Assert.isTrue(handler != null, "JsonRpcHandler not found for " + session);
    return handler;
  }

  @Override
  public void afterConnectionEstablished(Session session) throws Exception {
    JsonRpcHandler<T> handler = this.provider.createBean();
    this.handlers.put(session, handler);

    try {
      handler.afterConnectionEstablished(session);
    } catch (Exception e) {
      handler.handleUncaughtException(session, e);
    }
  }

  @Override
  public void afterConnectionClosed(Session session, String status) throws Exception {
    try {
      JsonRpcHandler<T> handler = getHandler(session);
      try {
        handler.afterConnectionClosed(session, status);
      } catch (Exception e) {
        handler.handleUncaughtException(session, e);
      }
    } finally {
      destroy(session);
    }
  }

  	@Override
	public void afterReconnection(Session session) throws Exception {
		JsonRpcHandler<T> handler = null;
		try {
			handler = getHandler(session);
			handler.afterReconnection(session);
		} catch (Exception e) {
			handler.handleUncaughtException(session, e);
		}
	}

  private void destroy(Session session) {
    JsonRpcHandler<T> handler = this.handlers.remove(session);
    try {
      if (handler != null) {
        this.provider.destroy(handler);
      }
    } catch (Throwable t) {
      logger.warn("Error while destroying handler", t);
    }
  }

  @Override
  public void handleTransportError(Session session, Throwable exception) throws Exception {
    JsonRpcHandler<T> handler = getHandler(session);
    try {
      handler.handleTransportError(session, exception);
    } catch (Exception e) {
      handler.handleUncaughtException(session, e);
    }
  }

  @Override
  public void handleUncaughtException(Session session, Exception exception) {
    logger.error("Uncaught exception while execution PerSessionJsonRpcHandler", exception);
  }

  @Override
  public PerSessionJsonRpcHandler<T> withSockJS() {
    this.useSockJS = true;
    return this;
  }

  @Override
  public boolean isSockJSEnabled() {
    return this.useSockJS;
  }

  @Override
  public PerSessionJsonRpcHandler<T> withLabel(String label) {
    this.label = label;
    return this;
  }

  @Override
  public String getLabel() {
    return label;
  }

  public PerSessionJsonRpcHandler<T> withPingWachdog(boolean pingAsWachdog) {
    this.pingWachdog = pingAsWachdog;
    return this;
  }

  @Override
  public boolean isPingWatchdog() {
    return pingWachdog;
  }

  @Override
  public final PerSessionJsonRpcHandler<T> withAllowedOrigins(String... origins) {
    this.allowedOrigins = ImmutableList.copyOf(origins);
    return this;
  }

  @Override
  public List<String> allowedOrigins() {
    return this.allowedOrigins;
  }

  @Override
  public JsonRpcHandler<T> withInterceptors(Object... interceptors) {
    this.interceptors = ImmutableList.copyOf(interceptors);
    return this;
  }

  @Override
  public List<Object> interceptors() {
    return this.interceptors;
  }

}
