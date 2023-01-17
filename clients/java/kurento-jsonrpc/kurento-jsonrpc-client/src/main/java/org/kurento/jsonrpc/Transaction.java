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

import java.io.IOException;

import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;

/**
 * A transaction represents a conversation between a client and the server.
 *
 * @author Ivan Gracia (igracia@gsyc.es) since 1.0.0
 *
 */
public interface Transaction {

  void sendResponseObject(Response<? extends Object> response) throws IOException;

  void sendVoidResponse() throws IOException;

  void sendResponse(Object result) throws IOException;

  void sendError(int code, String type, String data) throws IOException;

  void sendError(int code, String type, String message, String data) throws IOException;

  void sendError(Throwable e) throws IOException;

  Session getSession();

  void startAsync();

  boolean isNotification();

  void sendError(ResponseError error) throws IOException;

}
