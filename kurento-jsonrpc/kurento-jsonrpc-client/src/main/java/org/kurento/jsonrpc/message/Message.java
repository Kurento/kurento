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

package org.kurento.jsonrpc.message;

import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.JsonRpcConstants;

import com.google.gson.annotations.SerializedName;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public abstract class Message {

  /**
   * JSON RPC version.
   */
  @SerializedName("jsonrpc")
  private final String jsonrpc = JsonRpcConstants.JSON_RPC_VERSION;

  protected transient String sessionId;

  public Message() {
  }

  public Message(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getVersion() {
    return jsonrpc;
  }

  @Override
  public String toString() {
    return JsonUtils.toJsonMessage(this);
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

}
