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

import org.kurento.modulecreator.definition.DataItem;
import org.kurento.modulecreator.definition.Property;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DataItemAdapter implements JsonSerializer<DataItem> {

  @Override
  public JsonElement serialize(DataItem src, Type typeOfSrc, JsonSerializationContext context) {

    JsonObject object = new JsonObject();

    if (src.getName() != null) {
      object.add("name", context.serialize(src.getName()));
    }

    if (src.getDoc() != null) {
      object.addProperty("doc", src.getDoc());
    }

    if (src.getType() != null) {
      object.add("type", context.serialize(src.getType()));
    }

    if (src.isOptional()) {
      object.addProperty("optional", src.isOptional());
      if (src.getDefaultValue() != null) {
        object.add("defaultValue", src.getDefaultValue());
      }
    }

    if (src instanceof Property) {
      Property prop = (Property) src;
      if (prop.isReadOnly()) {
        object.addProperty("readOnly", true);
      }
      if (prop.isFinal()) {
        object.addProperty("final", true);
      }
    }

    return object;
  }
}
