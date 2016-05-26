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

public class RemoteClass extends Type {

  @SerializedName("extends")
  private TypeRef extendsProp;
  private Method constructor;
  private List<Method> methods;
  private List<Property> properties;
  private List<TypeRef> events;
  private boolean abstractClass;

  public RemoteClass(String name, String doc, TypeRef extendsProp) {
    super(name, doc);
    this.extendsProp = extendsProp;
    this.methods = new ArrayList<Method>();
    this.properties = new ArrayList<Property>();
    this.events = new ArrayList<TypeRef>();
  }

  public RemoteClass(String name, String doc, TypeRef extendsProp, Method constructor,
      List<Method> methods, List<Property> properties, List<TypeRef> events) {
    super(name, doc);
    this.extendsProp = extendsProp;
    this.constructor = constructor;
    this.methods = methods;
    this.properties = properties;
    this.events = events;
  }

  public Method getConstructor() {
    return constructor;
  }

  public List<TypeRef> getEvents() {
    return events;
  }

  public TypeRef getExtends() {
    return extendsProp;
  }

  public List<Method> getMethods() {
    return methods;
  }

  public boolean isAbstract() {
    return abstractClass;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public void addMethod(Method method) {
    this.methods.add(method);
  }

  public void addProperty(Property property) {
    this.properties.add(property);
  }

  public void setAbstract(boolean abstractModel) {
    this.abstractClass = abstractModel;
  }

  public void setConstructor(Method constructor) {
    this.constructor = constructor;
  }

  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  public void setEvents(List<TypeRef> events) {
    this.events = events;
  }

  public void setExtendsProp(TypeRef extendsProp) {
    this.extendsProp = extendsProp;
  }

  public void setMethods(List<Method> methods) {
    this.methods = methods;
  }

  public boolean isAssignableTo(String remoteClassName) {
    if (this.getName().equals(remoteClassName)) {
      return true;
    } else {
      if (getExtends() != null) {
        return ((RemoteClass) getExtends().getType()).isAssignableTo(remoteClassName);
      } else {
        return false;
      }
    }
  }

  @Override
  public List<ModelElement> getChildren() {
    List<ModelElement> children = new ArrayList<ModelElement>();
    if (extendsProp != null) {
      children.add(extendsProp);
    }
    children.addAll(properties);
    if (constructor != null) {
      children.add(constructor);
    }
    children.addAll(methods);
    children.addAll(events);
    return children;
  }

  @Override
  public String toString() {
    return "RemoteClass [extends=" + extendsProp + ", constructor=" + constructor + ", methods="
        + methods + ", doc=" + getDoc() + ", name=" + getName() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (abstractClass ? 1231 : 1237);
    result = prime * result + ((constructor == null) ? 0 : constructor.hashCode());
    result = prime * result + ((events == null) ? 0 : events.hashCode());
    result = prime * result + ((extendsProp == null) ? 0 : extendsProp.hashCode());
    result = prime * result + ((methods == null) ? 0 : methods.hashCode());
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
    RemoteClass other = (RemoteClass) obj;
    if (abstractClass != other.abstractClass) {
      return false;
    }
    if (constructor == null) {
      if (other.constructor != null) {
        return false;
      }
    } else if (!constructor.equals(other.constructor)) {
      return false;
    }
    if (events == null) {
      if (other.events != null) {
        return false;
      }
    } else if (!events.equals(other.events)) {
      return false;
    }
    if (extendsProp == null) {
      if (other.extendsProp != null) {
        return false;
      }
    } else if (!extendsProp.equals(other.extendsProp)) {
      return false;
    }
    if (methods == null) {
      if (other.methods != null) {
        return false;
      }
    } else if (!methods.equals(other.methods)) {
      return false;
    }
    return true;
  }

  public void expandMethodsWithOpsParams() {
    List<Method> newMethods = new ArrayList<Method>();
    for (Method method : this.methods) {
      newMethods.addAll(method.expandIfOpsParams());
    }
    this.methods.addAll(newMethods);
  }

}
