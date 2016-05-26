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

import java.util.List;

import org.kurento.modulecreator.definition.Method;
import org.kurento.modulecreator.definition.Param;
import org.kurento.modulecreator.definition.RemoteClass;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class IsFirstConstructorParam implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    RemoteClass thisRemoteClass = (RemoteClass) ((StringModel) arguments.get(0)).getWrappedObject();
    RemoteClass otherRemoteClass = (RemoteClass) ((StringModel) arguments.get(1))
        .getWrappedObject();

    if (otherRemoteClass.getConstructor() != null) {

      Method method = otherRemoteClass.getConstructor();

      List<Param> params = method.getParams();

      if (params.isEmpty()) {
        return false;
      } else {
        return params.get(0).getType().getType() == thisRemoteClass;
      }

    } else {
      return false;
    }
  }

}
