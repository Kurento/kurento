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

package org.kurento.jsonrpc;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public abstract class DefaultJsonRpcHandler<P> implements JsonRpcHandler<P> {

  private final Logger log = LoggerFactory.getLogger(DefaultJsonRpcHandler.class);

  private boolean useSockJs;
  private String label;
  private boolean pingWatchdog = false;

  private List<String> allowedOrigins = ImmutableList.of();
  
  private List<Object> interceptors = ImmutableList.of();

  @Override
  public void afterConnectionEstablished(Session session) throws Exception {
  }

  @Override
  public void afterConnectionClosed(Session session, String status) throws Exception {
  }

  @Override
  public void afterReconnection(Session session) throws Exception {
  }

  @Override
  public void handleTransportError(Session session, Throwable exception) throws Exception {
    log.warn("Transport error. Exception " + exception.getClass().getName() + ":"
        + exception.getLocalizedMessage());
  }

  @Override
  public void handleUncaughtException(Session session, Exception exception) {
    log.warn("Uncaught exception in handler {}", this.getClass().getName(), exception);
  }

  @Override
  public Class<?> getHandlerType() {
    return this.getClass();
  }

  @Override
  public DefaultJsonRpcHandler<P> withSockJS() {
    this.useSockJs = true;
    return this;
  }

  @Override
  public boolean isSockJSEnabled() {
    return this.useSockJs;
  }

  @Override
  public final DefaultJsonRpcHandler<P> withAllowedOrigins(String... origins) {
    this.allowedOrigins = ImmutableList.copyOf(origins);
    return this;
  }

  @Override
  public List<String> allowedOrigins() {
    return this.allowedOrigins;
  }

  @Override
  public DefaultJsonRpcHandler<P> withLabel(String label) {
    this.label = label;
    return this;
  }

  @Override
  public String getLabel() {
    return label;
  }

  public DefaultJsonRpcHandler<P> withPingWatchdog(boolean pingAsWachdog) {
    this.pingWatchdog = pingAsWachdog;
    return this;
  }

  @Override
  public boolean isPingWatchdog() {
    return pingWatchdog;
  }
  
  @Override
  public JsonRpcHandler<P> withInterceptors(Object... interceptors) {
    this.interceptors = ImmutableList.copyOf(interceptors);
    return this;
  }

  @Override
  public List<Object> interceptors() {
    return this.interceptors;
  }
}
