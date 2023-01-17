/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
package org.kurento.client;

import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;

class JsonRpcConnectionListenerKurento implements JsonRpcWSConnectionListener {

  private KurentoConnectionListener listener;

  public JsonRpcConnectionListenerKurento(KurentoConnectionListener listener) {
    this.listener = listener;
  }

  @Override
  public void connectionFailed() {
    listener.connectionFailed();
  }

  @Override
  public void connected() {
    listener.connected();
  }

  @Override
  public void disconnected() {
    listener.disconnected();
  }

  @Override
  public void reconnected(boolean sameServer) {
    listener.reconnected(sameServer);
  }

  public static JsonRpcWSConnectionListener create(KurentoConnectionListener listener) {

    if (listener == null) {
      return null;
    }

    return new JsonRpcConnectionListenerKurento(listener);
  }

  @Override
  public void reconnecting() {
    listener.disconnected();
  }

}
