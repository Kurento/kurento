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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

/**
 * A context element from Orion.
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
public class OrionContextElement {

  private String type;

  private boolean isPattern;
  private String id;
  private final List<OrionAttribute<?>> attributes = newArrayList();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isPattern() {
    return isPattern;
  }

  public void setPattern(boolean isPattern) {
    this.isPattern = isPattern;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<OrionAttribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(" Type: ").append(type).append("\n");
    sb.append(" Id: ").append(id).append("\n");
    sb.append(" IsPattern: ").append(isPattern).append("\n");

    return sb.toString();
  }
}
