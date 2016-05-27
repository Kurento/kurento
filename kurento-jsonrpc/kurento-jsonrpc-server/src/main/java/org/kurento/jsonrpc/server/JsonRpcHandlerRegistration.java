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

/**
 * Provides methods for configuring a JsonRpcHandler handler.
 */
public interface JsonRpcHandlerRegistration {

  /**
   * Add more handlers that will share the same configuration
   * 
   * @param handler
   *          the handler to register
   * @param paths
   *          paths to register the handler in
   * @return the handler registration
   */
  JsonRpcHandlerRegistration addHandler(JsonRpcHandler<?> handler, String... paths);

}