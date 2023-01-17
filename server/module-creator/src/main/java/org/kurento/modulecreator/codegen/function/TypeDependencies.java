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
package org.kurento.modulecreator.codegen.function;

import java.util.LinkedList;
import java.util.List;

import org.kurento.modulecreator.definition.ComplexType;
import org.kurento.modulecreator.definition.ComplexType.TypeFormat;
import org.kurento.modulecreator.definition.Event;
import org.kurento.modulecreator.definition.Method;
import org.kurento.modulecreator.definition.Param;
import org.kurento.modulecreator.definition.Property;
import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.Type;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class TypeDependencies implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    Object type = arguments.get(0);

    if (type instanceof StringModel) {
      type = ((StringModel) type).getWrappedObject();
      if (type instanceof Return) {
        type = ((Return) type).getType();
      }
    }

    List<Type> types = new LinkedList<Type>();

    if (type instanceof RemoteClass) {
      RemoteClass remoteClass = (RemoteClass) type;

      if (remoteClass.getConstructor() != null) {
        addMethodTypes(types, remoteClass.getConstructor());
      }

      for (Method method : remoteClass.getMethods()) {
        addMethodTypes(types, method);
      }

      for (Property property : remoteClass.getProperties()) {
        addDependency(types, property.getType().getType());
      }

      if (remoteClass.getExtends() != null) {
        types.remove(remoteClass.getExtends().getType());
      }

      types.remove(remoteClass);
    } else if (type instanceof Event) {
      Event event = (Event) type;

      for (Property property : event.getProperties()) {
        addDependency(types, property.getType().getType());
      }

      if (event.getExtends() != null) {
        types.remove(event.getExtends().getType());
      }
    } else if (type instanceof ComplexType) {
      ComplexType complexType = (ComplexType) type;

      if (complexType.getTypeFormat() == TypeFormat.REGISTER) {
        for (Property property : complexType.getProperties()) {
          addDependency(types, property.getType().getType());
        }

        if (complexType.getExtends() != null) {
          types.remove(complexType.getExtends().getType());
        }
      }
    }

    types = removeDuplicates(types);

    return types;
  }

  private void addDependency(List<Type> dependencies, Type type) {
    if (type instanceof RemoteClass || type instanceof ComplexType) {
      dependencies.add(type);
    }
  }

  private List<Type> removeDuplicates(List<Type> original) {
    List<Type> types = new LinkedList<Type>();

    for (Type t : original) {
      if (!types.contains(t)) {
        types.add(t);
      }
    }

    return types;
  }

  private void addMethodTypes(List<Type> dependencies, Method method) {
    for (Param p : method.getParams()) {
      addDependency(dependencies, p.getType().getType());
    }

    Return ret = method.getReturn();

    if (ret != null) {
      addDependency(dependencies, ret.getType().getType());
    }
  }
}
