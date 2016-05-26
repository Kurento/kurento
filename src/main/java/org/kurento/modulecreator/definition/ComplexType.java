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

public class ComplexType extends Type {

  public enum TypeFormat {
    REGISTER, ENUM
  }

  private TypeFormat typeFormat;

  @SerializedName("extends")
  private TypeRef extendsProp;
  private List<Property> properties = new ArrayList<Property>();
  private List<Property> parentProperties;

  private List<String> values;

  public ComplexType(String name, String doc, List<Property> properties, List<String> values) {
    super(name, doc);
    this.properties = properties;
    this.values = values;
    if (properties != null) {
      typeFormat = TypeFormat.REGISTER;
    } else if (values != null) {
      typeFormat = TypeFormat.ENUM;
    } else {
      new AssertionError("Properties or values have to have a non null value");
    }
  }

  public TypeRef getExtends() {
    return extendsProp;
  }

  public void setExtends(TypeRef extendsProp) {
    this.extendsProp = extendsProp;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public TypeFormat getTypeFormat() {
    return typeFormat;
  }

  public List<String> getValues() {
    return values;
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

  private void resolveParentProperties() {
    this.parentProperties = new ArrayList<Property>();
    if (this.extendsProp != null) {
      ComplexType complexType = (ComplexType) extendsProp.getType();
      this.parentProperties.addAll(complexType.getParentProperties());
      this.parentProperties.addAll(complexType.getProperties());
    }
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  @Override
  public List<ModelElement> getChildren() {
    List<ModelElement> children = new ArrayList<ModelElement>();
    if (extendsProp != null) {
      children.add(extendsProp);
    }

    if (properties != null) {
      children.addAll(properties);
    }
    return children;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((extendsProp == null) ? 0 : extendsProp.hashCode());
    result = prime * result + ((properties == null) ? 0 : properties.hashCode());
    result = prime * result + ((typeFormat == null) ? 0 : typeFormat.hashCode());
    result = prime * result + ((values == null) ? 0 : values.hashCode());
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
    ComplexType other = (ComplexType) obj;
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
    if (typeFormat != other.typeFormat) {
      return false;
    }
    if (values == null) {
      if (other.values != null) {
        return false;
      }
    } else if (!values.equals(other.values)) {
      return false;
    }
    return true;
  }

}
