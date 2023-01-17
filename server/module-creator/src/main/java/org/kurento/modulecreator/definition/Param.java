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
package org.kurento.modulecreator.definition;

import com.google.gson.JsonElement;

public class Param extends DataItem {

  public Param(String name, String doc, TypeRef type, boolean optional) {
    super(name, doc, type, optional);
  }

  public Param(String name, String doc, TypeRef type, JsonElement defaultValue) {
    super(name, doc, type, defaultValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Param [type()=");
    sb.append(getType());
    sb.append(", optional()=");
    sb.append(isOptional());
    sb.append(", doc()=");
    sb.append(getDoc());
    if (isOptional()) {
      sb.append(", defaultValue()=");
      sb.append(getDefaultValue());
    }
    sb.append("]");

    return sb.toString();
  }
}
