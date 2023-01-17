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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class JsNamespace implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    RemoteClass remoteClass = (RemoteClass) ((StringModel) arguments.get(0)).getWrappedObject();

    Set<String> baseClassNames = new HashSet<String>();
    baseClassNames.add("Filter");
    baseClassNames.add("Endpoint");
    baseClassNames.add("Hub");

    TypeRef extendsRef = remoteClass.getExtends();
    while (extendsRef != null) {

      RemoteClass parentRemoteClass = (RemoteClass) extendsRef.getType();
      if (baseClassNames.contains(parentRemoteClass.getName())) {
        return parentRemoteClass.getName();
      }

      extendsRef = parentRemoteClass.getExtends();
    }

    return "None";
  }

}
