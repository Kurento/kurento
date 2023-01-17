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

package org.kurento.jsonrpc.internal.client;

import java.io.IOException;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;

public abstract class AbstractTransaction implements Transaction {

  protected final Session session;
  protected boolean async;
  protected final Request<?> request;

  public AbstractTransaction(Session session, Request<?> request) {
    super();
    this.session = session;
    this.request = request;
  }

  @Override
  public Session getSession() {
    return session;
  }

  @Override
  public void startAsync() {
    async = true;
  }

  public boolean isAsync() {
    return async;
  }

  @Override
  public void sendResponse(Object result) throws IOException {
    internalSendResponse(new Response<>(request.getId(), result));
  }

  @Override
  public void sendError(int code, String type, String data) throws IOException {
    internalSendResponse(new Response<>(request.getId(), new ResponseError(code, type, data)));
  }

  @Override
  public void sendError(int code, String type, String message, String data) throws IOException {
    internalSendResponse(
        new Response<>(request.getId(), new ResponseError(code, type, message, data)));
  }

  @Override
  public void sendError(Throwable e) throws IOException {
    ResponseError error = ResponseError.newFromException(e);
    internalSendResponse(new Response<>(request.getId(), error));
  }

  @Override
  public void sendVoidResponse() throws IOException {
    sendResponse(null);
  }

  @Override
  public void sendError(ResponseError error) throws IOException {
    internalSendResponse(new Response<>(request.getId(), error));
  }

  @Override
  public void sendResponseObject(Response<? extends Object> response) throws IOException {
    internalSendResponse(response);
  }

  @Override
  public boolean isNotification() {
    return request.getId() == null;
  }

  protected abstract void internalSendResponse(Response<? extends Object> response)
      throws IOException;

}
