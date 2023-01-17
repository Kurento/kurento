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
package org.kurento.modulecreator.json;

import java.lang.reflect.Type;

import org.kurento.modulecreator.definition.TypeRef;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class TypeRefAdapter implements JsonSerializer<TypeRef>, JsonDeserializer<TypeRef> {

  @Override
  public JsonElement serialize(TypeRef src, Type typeOfSrc, JsonSerializationContext context) {
    String name = src.getName();
    if (src.isList()) {
      name += "[]";
    } else if (src.isMap()) {
      name += "<>";
    }
    return new JsonPrimitive(name);
  }

  @Override
  public TypeRef deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {

    String typeRefString = json.getAsString();

    return TypeRef.parseFromJson(typeRefString);
  }

}
