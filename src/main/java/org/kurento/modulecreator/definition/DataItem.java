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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;

public class DataItem extends NamedElement {

  private TypeRef type;
  private boolean optional = false;
  private JsonElement defaultValue;

  public DataItem(String name, String doc, TypeRef type, boolean optional) {
    super(name, doc);
    this.type = type;
    this.optional = optional;
  }

  public DataItem(String name, String doc, TypeRef type, JsonElement defaultValue) {
    super(name, doc);
    this.type = type;
    this.optional = true;
    this.defaultValue = defaultValue;
  }

  public TypeRef getType() {
    return type;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  public void setType(TypeRef type) {
    this.type = type;
  }

  public JsonElement getDefaultValue() {
    return defaultValue;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (optional ? 1231 : 1237);
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DataItem other = (DataItem) obj;
    if (optional != other.optional) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

  @Override
  public List<ModelElement> getChildren() {
    return new ArrayList<ModelElement>(Arrays.asList(type));
  }

}
