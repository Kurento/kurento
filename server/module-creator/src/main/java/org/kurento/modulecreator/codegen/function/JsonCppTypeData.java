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

import org.kurento.modulecreator.definition.ComplexType;
import org.kurento.modulecreator.definition.ComplexType.TypeFormat;
import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class JsonCppTypeData implements TemplateMethodModelEx {

  public class JsonTypeData {
    String jsonMethod;
    String jsonValueCheck;
    String jsonValueType;
    String typeDescription;

    public String getJsonMethod() {
      return jsonMethod;
    }

    public String getJsonValueCheck() {
      return jsonValueCheck;
    }

    public String getJsonValueType() {
      return jsonValueType;
    }

    public String getTypeDescription() {
      return typeDescription;
    }
  }

  public JsonCppTypeData() {
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

    if (type == null) {
      throw new TemplateModelException("Received invalid type");
    }

    if (type instanceof TypeRef) {
      TypeRef typeRef = (TypeRef) type;
      if (typeRef.isList()) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "List";
        data.jsonValueCheck = "isArray";
        data.jsonValueType = "arrayValue";
        data.typeDescription = "list";
        return data;
      } else if (typeRef.isMap()) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "Map";
        data.jsonValueCheck = "isObject";
        data.jsonValueType = "objectValue";
        data.typeDescription = "map";
        return data;
      } else if (typeRef.getName().equals("String")) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "String";
        data.jsonValueCheck = "isString";
        data.jsonValueType = "stringValue";
        data.typeDescription = "string";
        return data;
      } else if (typeRef.getName().equals("int")) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "Int";
        data.jsonValueCheck = "isInt";
        data.jsonValueType = "intValue";
        data.typeDescription = "integer";
        return data;
      } else if (typeRef.getName().equals("boolean")) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "Bool";
        data.jsonValueCheck = "isBool";
        data.jsonValueType = "booleanValue";
        data.typeDescription = "boolean";
        return data;
      } else if (typeRef.getName().equals("double") || typeRef.getName().equals("float")) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "Double";
        data.jsonValueCheck = "isDouble";
        data.jsonValueType = "realValue";
        data.typeDescription = "double";
        return data;
      } else if (typeRef.getName().equals("int64")) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "Int64";
        data.jsonValueCheck = "isInt64";
        data.jsonValueType = "intValue";
        data.typeDescription = "int64";
        return data;
      } else if (typeRef.getType() instanceof ComplexType) {
        ComplexType complexType = (ComplexType) typeRef.getType();

        if (complexType.getTypeFormat() == TypeFormat.ENUM) {
          JsonTypeData data = new JsonTypeData();
          data.jsonMethod = "String";
          data.jsonValueCheck = "isString";
          data.jsonValueType = "stringValue";
          data.typeDescription = "string";
          return data;
        } else if (complexType.getTypeFormat() == TypeFormat.REGISTER) {
          JsonTypeData data = new JsonTypeData();
          data.jsonMethod = "Object";
          data.jsonValueCheck = "isObject";
          data.jsonValueType = "objectValue";
          data.typeDescription = "object";
          return data;
        }
      } else if (typeRef.getType() instanceof RemoteClass) {
        JsonTypeData data = new JsonTypeData();
        data.jsonMethod = "String";
        data.jsonValueCheck = "isString";
        data.jsonValueType = "stringValue";
        data.typeDescription = "string";
        return data;
      }

      throw new TemplateModelException("Unexpected type: " + type);
    }

    throw new TemplateModelException("Received invalid type");
  }
}
