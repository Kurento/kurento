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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

/**
 * Query context request object
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 */
public class QueryContext {

  @SerializedName("entities")
  private List<OrionContextElement> entities;

  @SerializedName("updateAction")
  private List<OrionAttribute<?>> attributes;

  public QueryContext(List<OrionContextElement> entities, OrionAttribute<?>... attributes) {
    this.attributes = ImmutableList.copyOf(attributes);
    this.entities = ImmutableList.copyOf(entities);
  }

  public QueryContext(List<OrionContextElement> entities) {
    this.entities = ImmutableList.copyOf(entities);
    this.attributes = ImmutableList.of();
  }

  public QueryContext(OrionContextElement entity) {
    this.entities = ImmutableList.of(entity);
    this.attributes = ImmutableList.of();
  }

  public List<OrionContextElement> getEntities() {
    return entities;
  }

  public void setEntities(List<OrionContextElement> entities) {
    this.entities = entities;
  }

  public List<OrionAttribute<?>> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<OrionAttribute<?>> attributes) {
    this.attributes = attributes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (OrionContextElement element : entities) {
      sb.append(element).append("\n");
    }

    return sb.toString();
  }
}
