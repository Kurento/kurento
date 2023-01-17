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
 * Context element with a status code
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
public class OrionContextElementResponse extends AbstractOrionResponse {

  @SerializedName("contextElement")
  private OrionContextElement contextElement;

  public OrionContextElement getContextElement() {
    return contextElement;
  }

  public void setContextElement(OrionContextElement contextElement) {
    this.contextElement = contextElement;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(contextElement).append("\n");
    sb.append(super.toString());

    return sb.toString();
  }
}
