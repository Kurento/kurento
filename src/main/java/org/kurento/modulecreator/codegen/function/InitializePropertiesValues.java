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

import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class InitializePropertiesValues implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    if (arguments.size() != 1) {
      throw new TemplateModelException("This function expects 1 parameter");
    }
    Object type = arguments.get(0);
    String ret = "";

    if (type instanceof StringModel) {
      type = ((StringModel) type).getWrappedObject();
      if (type instanceof Return) {
        type = ((Return) type).getType();
      }
    }

    if (type instanceof TypeRef) {
      TypeRef typeRef = (TypeRef) type;
      if (typeRef.getName().equals("boolean")) {
        ret = "= false";
      } else if (typeRef.getName().equals("float") || typeRef.getName().equals("int")) {
        ret = "= 0";
      }
    } else {
      throw new TemplateModelException("Class not expected " + type);
    }
    return ret;
  }

}
