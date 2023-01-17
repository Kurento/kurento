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

package org.kurento.jsonrpc.internal.server.config;

import java.util.Arrays;

import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistration;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

/**
 * Base class for {@link WebSocketHandlerRegistration}s that gathers all the configuration options
 * but allows sub-classes to put together the actual HTTP request mappings.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultJsonRpcHandlerRegistration implements JsonRpcHandlerRegistration {

  private final MultiValueMap<JsonRpcHandler<?>, String> handlerMap = new LinkedMultiValueMap<>();
  private final MultiValueMap<Class<? extends JsonRpcHandler<?>>, String> perSessionHandlerClassMap =
      new LinkedMultiValueMap<>();
  private final MultiValueMap<String, String> perSessionHandlerBeanNameMap =
      new LinkedMultiValueMap<>();

  @Override
  public JsonRpcHandlerRegistration addHandler(JsonRpcHandler<?> handler, String... paths) {
    Assert.notNull(handler);
    Assert.notEmpty(paths);
    this.handlerMap.put(handler, Arrays.asList(paths));
    return this;
  }

  public JsonRpcHandlerRegistration addPerSessionHandler(
      Class<? extends JsonRpcHandler<?>> handlerClass, String[] paths) {
    Assert.notNull(handlerClass);
    Assert.notEmpty(paths);
    this.perSessionHandlerClassMap.put(handlerClass, Arrays.asList(paths));
    return this;
  }

  public JsonRpcHandlerRegistration addPerSessionHandler(String beanName, String[] paths) {
    Assert.notNull(beanName);
    Assert.notEmpty(paths);
    this.perSessionHandlerBeanNameMap.put(beanName, Arrays.asList(paths));
    return this;
  }

  public MultiValueMap<JsonRpcHandler<?>, String> getHandlerMap() {
    return handlerMap;
  }

  public MultiValueMap<String, String> getPerSessionHandlerBeanNameMap() {
    return perSessionHandlerBeanNameMap;
  }

  public MultiValueMap<Class<? extends JsonRpcHandler<?>>, String> getPerSessionHandlerClassMap() {
    return perSessionHandlerClassMap;
  }

}
