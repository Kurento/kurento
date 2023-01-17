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

import java.util.ArrayList;
import java.util.List;

import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class TypeHierarchy implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    Object type = arguments.get(0);

    if (type instanceof StringModel) {
      type = ((StringModel) type).getWrappedObject();
      if (type instanceof Return) {
        type = ((Return) type).getType();
      }
    }

    if (type instanceof TypeRef) {
      type = ((TypeRef) type).getType();
    }

    if (type instanceof RemoteClass) {
      RemoteClass remoteClass = (RemoteClass) type;
      TypeRef parent = remoteClass.getExtends();
      ArrayList<String> list = new ArrayList<String>();

      while (parent != null) {
        String moduleName = parent.getModule().getName();

        if (moduleName == null) {
          moduleName = "";
        }

        if (moduleName.equals("core") || moduleName.equals("elements")
            || moduleName.equals("filters")) {
          moduleName = "kurento";
        }

        if (!moduleName.isEmpty()) {
          moduleName += ".";
        }

        list.add(moduleName + parent.getName());

        if (parent.getType() instanceof RemoteClass) {
          parent = ((RemoteClass) parent.getType()).getExtends();
        }
      }

      return list;
    } else {
      throw new TemplateModelException("Received invalid type, only RemoteClass is accepted");
    }

  }
}