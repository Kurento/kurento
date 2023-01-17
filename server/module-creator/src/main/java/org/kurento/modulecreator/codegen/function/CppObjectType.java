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

import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class CppObjectType implements TemplateMethodModelEx {

  private final Set<String> nativeTypes;

  public CppObjectType() {
    nativeTypes = new HashSet<String>();

    nativeTypes.add("float");
    nativeTypes.add("int");
    nativeTypes.add("double");
    nativeTypes.add("int64");
  }

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    Object type = arguments.get(0);

    if (type instanceof StringModel) {
      type = ((StringModel) type).getWrappedObject();
      if (type instanceof Return) {
        type = ((Return) type).getType();
      }
    }

    boolean isParam = true;
    String prefix = "";
    String suffix = "";

    if (arguments.size() > 1) {
      isParam = ((TemplateBooleanModel) arguments.get(1)).getAsBoolean();
    }

    if (arguments.size() > 2) {
      prefix = arguments.get(2).toString();
    }

    if (arguments.size() > 3) {
      suffix = arguments.get(3).toString();
    }

    if (type == null) {
      if (isParam) {
        return "void ";
      } else {
        return "void";
      }
    }

    if (type instanceof TypeRef) {
      TypeRef typeRef = (TypeRef) type;
      if (typeRef.isList()) {
        if (isParam) {
          return "const std::vector<" + getTypeAsString(typeRef.getName(), false, prefix, suffix)
              + "> &";
        } else {
          return "std::vector<" + getTypeAsString(typeRef.getName(), false, prefix, suffix) + ">";
        }
      } else if (typeRef.isMap()) {
        if (isParam) {
          return "const std::map <std::string,"
              + getTypeAsString(typeRef.getName(), false, prefix, suffix) + "> &";
        } else {
          return "std::map <std::string,"
              + getTypeAsString(typeRef.getName(), false, prefix, suffix) + ">";
        }
      } else {
        return getTypeAsString(typeRef.getName(), isParam, prefix, suffix);
      }
    }

    return getTypeAsString(type.toString(), isParam, prefix, suffix);
  }

  private String getTypeAsString(String typeName, boolean isParam, String prefix, String suffix) {
    if (typeName.equals("boolean")) {
      if (isParam) {
        return "bool ";
      } else {
        return "bool";
      }
    } else if (typeName.equals("String")) {
      if (isParam) {
        return "const std::string &";
      } else {
        return "std::string";
      }
    } else if (nativeTypes.contains(typeName)) {
      if (isParam) {
        if (typeName.equals("int64")) {
          return "int64_t ";
        } else {
          return typeName + " ";
        }
      } else {
        if (typeName.equals("int64")) {
          return "int64_t";
        } else {
          return typeName;
        }
      }
    } else {
      if (isParam) {
        return "std::shared_ptr<" + prefix + typeName + suffix + "> ";
      } else {
        return "std::shared_ptr<" + prefix + typeName + suffix + ">";
      }
    }
  }
}
