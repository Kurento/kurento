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
 * Query context response object
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 */
public class QueryContextResponse extends AbstractOrionResponse {

  @SerializedName("contextElement")
  private OrionContextElement element;

  public QueryContextResponse() {
  }

  public OrionContextElement getElement() {
    return element;
  }

  public void setElement(OrionContextElement element) {
    this.element = element;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(element).append("\n");
    sb.append(super.toString());

    return sb.toString();
  }
}