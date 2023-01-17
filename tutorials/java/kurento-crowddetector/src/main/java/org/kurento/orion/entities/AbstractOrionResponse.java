/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.orion.entities;

import com.google.gson.annotations.SerializedName;

/**
 * Abstract response having an {@link StatusCode} object, common to all Orion responses. The only
 * exception is the {@link ContextUpdateResponse}, which holds a list of objects, each one of them
 * having its own status code.
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
class AbstractOrionResponse implements OrionResponse {

  @SerializedName("statusCode")
  private StatusCode statusCode;

  @Override
  public StatusCode getStatus() {
    return this.statusCode;
  }

  @Override
  public void setStatus(StatusCode statusCode) {
    this.statusCode = statusCode;
  }

  @Override
  public String toString() {
    return statusCode.toString();
  }
}
