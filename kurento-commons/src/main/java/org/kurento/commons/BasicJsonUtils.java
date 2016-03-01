/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
