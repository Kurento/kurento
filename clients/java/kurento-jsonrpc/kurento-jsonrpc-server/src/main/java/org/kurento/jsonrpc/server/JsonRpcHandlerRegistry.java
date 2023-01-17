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

package org.kurento.jsonrpc.server;

import org.kurento.jsonrpc.JsonRpcHandler;

public interface JsonRpcHandlerRegistry {

  /**
   * Configure a JsonRpcHandler at the specified URL paths.
   * 
   * @param jsonRpcHandler
   * @param paths
   * @return The handler registration object
   */
  JsonRpcHandlerRegistration addHandler(JsonRpcHandler<?> jsonRpcHandler, String... paths);

  JsonRpcHandlerRegistration addPerSessionHandler(Class<? extends JsonRpcHandler<?>> handlerClass,
      String... string);

  JsonRpcHandlerRegistration addPerSessionHandler(String beanName, String... string);

}
