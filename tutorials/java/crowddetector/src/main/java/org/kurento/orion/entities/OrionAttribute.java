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

/**
 * Attribute to be used in context elements and queries
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @param <T>
 *          The type of attribute
 *
 */
public class OrionAttribute<T> {

  private String name;
  private String type;
  private T value;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public OrionAttribute(String name, String type, T value) {
    super();
    this.name = name;
    this.type = type;
    this.value = value;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(" Name: ").append(name).append("\n");
    sb.append(" Type: ").append(type).append("\n");
    sb.append(" Value: ").append(value).append("\n");

    return sb.toString();
  }
}
