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

import org.kurento.jsonrpc.message.Request;

public interface JsonRpcHandler<P> {

  /**
   * Invoked when a new JsonRpc request arrives.
   *
   * @param transaction
   *          the transaction to which the request belongs
   * @param request
   *          the request
   *
   * @throws TransportException
   *           when there is an error in the transport mechanism
   *
   * @throws Exception
   *           this method can handle or propagate exceptions.
   */
  void handleRequest(Transaction transaction, Request<P> request) throws Exception;

  void afterConnectionEstablished(Session session) throws Exception;

  void afterConnectionClosed(Session session, String status) throws Exception;

  void afterReconnection(Session session) throws Exception;

  void handleTransportError(Session session, Throwable exception) throws Exception;

  void handleUncaughtException(Session session, Exception exception);

  Class<?> getHandlerType();

  /**
   * This method configures the handler to use sockJS
   */
  JsonRpcHandler<P> withSockJS();

  JsonRpcHandler<P> withAllowedOrigins(String... string);
  
  JsonRpcHandler<P> withInterceptors(Object... interceptors);

  boolean isSockJSEnabled();

  List<String> allowedOrigins();

  JsonRpcHandler<P> withLabel(String label);

  String getLabel();

  boolean isPingWatchdog();
  
  List<Object> interceptors();
}
