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

package org.kurento.jsonrpc.internal.ws;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.jsonrpc.JsonRpcException;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;

public class PendingRequests {

  private static final Logger log = LoggerFactory.getLogger(PendingRequests.class);

  private final ConcurrentMap<Integer, SettableFuture<Response<JsonElement>>> pendingRequests =
      new ConcurrentHashMap<>();

  public void handleResponse(Response<JsonElement> response) {

    SettableFuture<Response<JsonElement>> responseFuture = pendingRequests.remove(response.getId());

    if (responseFuture == null) {
      // TODO It is necessary to do something else? Who is watching this?
      log.warn("Received response {} with an id not registered as pending request. Maybe the request timed out", response);
    } else {
      responseFuture.set(response);
    }
  }

  public ListenableFuture<Response<JsonElement>> prepareResponse(Integer id) {

    Preconditions.checkNotNull(id, "The request id cannot be null");

    SettableFuture<Response<JsonElement>> responseFuture = SettableFuture.create();

    if (pendingRequests.putIfAbsent(id, responseFuture) != null) {
      throw new JsonRpcException("Can not send a request with the id '" + id
          + "'. There is already a pending request with this id");
    }

    return responseFuture;
  }

  public void closeAllPendingRequests() {
    log.debug("Sending error to all pending requests");
    for (SettableFuture<Response<JsonElement>> responseFuture : pendingRequests.values()) {
      responseFuture.set(new Response<JsonElement>(
          new ResponseError(0, "Connection with server have been closed")));
    }
    pendingRequests.clear();
  }

}
