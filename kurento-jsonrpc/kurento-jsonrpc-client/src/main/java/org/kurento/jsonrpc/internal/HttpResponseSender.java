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

package org.kurento.jsonrpc.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.Response;

public class HttpResponseSender implements ResponseSender {

  private List<Response<Object>> responses = new ArrayList<>();

  public synchronized List<Response<Object>> getResponseListToSend() {
    List<Response<Object>> returnResponses = responses;
    responses = new ArrayList<>();
    return returnResponses;
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized void sendResponse(Message message) throws IOException {
    responses.add((Response<Object>) message);
  }

  @Override
  public void sendPingResponse(Message message) throws IOException {
    sendResponse(message);
  }
}
