/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.commons;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class BasicJsonUtils {

  public static Map<String, Object> toPropertiesMap(JsonObject jsonProps) {
    Map<String, Object> map;
    if (jsonProps != null) {
      map = new HashMap<>();
      for (Entry<String, JsonElement> e : jsonProps.entrySet()) {
        map.put(e.getKey(), convertValue(e.getValue()));
      }
    } else {
      map = Collections.emptyMap();
    }
    return map;
  }

  private static Object convertValue(JsonElement value) {
    if (value.isJsonNull()) {
      return null;
    } else if (value.isJsonPrimitive()) {
      JsonPrimitive prim = value.getAsJsonPrimitive();
      if (prim.isBoolean()) {
        return prim.getAsBoolean();
      } else if (prim.isNumber()) {
        Number n = prim.getAsNumber();
        if (n.doubleValue() == n.intValue()) {
          return n.intValue();
        } else {
          return n.doubleValue();
        }
      } else if (prim.isString()) {
        return prim.getAsString();
      } else {
        throw new RuntimeException("Unrecognized value: " + value);
      }
    } else {
      return value.toString();
    }
  }
}
