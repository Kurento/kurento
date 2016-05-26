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
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Event extends Type {

  private List<Property> properties;

  @SerializedName("extends")
  private TypeRef extendsProp;

  private List<Property> parentProperties;

  public Event(String name, String doc, List<Property> properties) {
    super(name, doc);
    this.properties = properties;
  }

  public void setExtends(TypeRef extendsProp) {
    this.extendsProp = extendsProp;
  }

  public TypeRef getExtends() {
    return extendsProp;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  public List<Property> getParentProperties() {
    if (parentProperties == null) {
      resolveParentProperties();
    }
    return parentProperties;
  }

  public void setParentProperties(List<Property> parentProperties) {
    this.parentProperties = parentProperties;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((extendsProp == null) ? 0 : extendsProp.hashCode());
    result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
    Event other = (Event) obj;
    if (extendsProp == null) {
      if (other.extendsProp != null) {
        return false;
      }
    } else if (!extendsProp.equals(other.extendsProp)) {
      return false;
    }
    if (properties == null) {
      if (other.properties != null) {
        return false;
      }
    } else if (!properties.equals(other.properties)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Event [properties=" + properties + ", extendsProp=" + extendsProp + ", getDoc()="
        + getDoc() + ", getName()=" + getName() + "]";
  }

  @Override
  public List<ModelElement> getChildren() {
    List<ModelElement> elements = new ArrayList<ModelElement>(properties);
    if (extendsProp != null) {
      elements.add(extendsProp);
    }
    return elements;
  }

  private void resolveParentProperties() {
    this.parentProperties = new ArrayList<Property>();
    if (this.extendsProp != null) {
      Event event = (Event) extendsProp.getType();
      this.parentProperties.addAll(event.getParentProperties());
      this.parentProperties.addAll(event.getProperties());
    }
  }

}
