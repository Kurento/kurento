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

package org.kurento.client.internal.transport.jsonrpc;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.kurento.client.internal.server.ProtocolException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.Props;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonResponseUtils {

  public static <E> E convertFromResult(JsonElement result, Type type) {

    if (type == Void.class || type == void.class) {
      return null;
    }

    JsonElement extractResult = extractValueFromResponse(result, type);

    return JsonUtils.fromJson(extractResult, type);
  }

  private static JsonElement extractValueFromResponse(JsonElement result, Type type) {

    if (result == null) {
      return null;
    }

    if (result instanceof JsonNull) {
      return null;
    }

    if (isPrimitiveClass(type) || isEnum(type)) {

      if (result instanceof JsonPrimitive) {
        return result;

      } else if (result instanceof JsonArray) {
        throw new ProtocolException(
            "Json array '" + result + " cannot be converted to " + getTypeName(type));
      } else if (result instanceof JsonObject) {
        return extractSimpleValueFromJsonObject((JsonObject) result, type);
      } else {
        throw new ProtocolException("Unrecognized json element: " + result);
      }

    } else if (isComplexType(type)) {
      if (result instanceof JsonObject) {
        if (((JsonObject) result).has("value")) {
          return ((JsonObject) result).get("value");
        }
      }
      return result;
    } else if (isList(type)) {

      if (result instanceof JsonArray) {
        return result;
      }

      return extractSimpleValueFromJsonObject((JsonObject) result, type);
    } else {
      return result;
    }
  }

  private static JsonElement extractSimpleValueFromJsonObject(JsonObject result, Type type) {

    if (!result.has("value")) {
      throw new ProtocolException("Json object " + result + " cannot be converted to "
          + getTypeName(type) + " without a 'value' property");
    }

    return result.get("value");
  }

  private static boolean isEnum(Type type) {

    if (type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      return clazz.isEnum();
    }

    return false;
  }

  private static boolean isComplexType(Type type) {
    return type == Props.class;
  }

  private static boolean isPrimitiveClass(Type type) {
    return type == String.class
        || type == Void.class
        || type == void.class
        || type == Boolean.class
        || type == boolean.class
        || type == Integer.class
        || type == int.class
        || type == Long.class
        || type == long.class
        || type == Float.class
        || type == float.class
        || type == Double.class
        || type == double.class;
  }

  private static boolean isList(Type type) {

    if (type == List.class) {
      return true;
    }

    if (type instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) type;
      if (paramType.getRawType() instanceof Class) {
        return ((Class<?>) paramType.getRawType()).isAssignableFrom(List.class);
      }
    }

    return false;
  }

  private static String getTypeName(Type type) {

    if (type instanceof Class) {

      Class<?> clazz = (Class<?>) type;
      return clazz.getSimpleName();

    } else if (type instanceof ParameterizedType) {

      StringBuilder sb = new StringBuilder();

      ParameterizedType paramType = (ParameterizedType) type;
      Class<?> rawClass = (Class<?>) paramType.getRawType();

      sb.append(rawClass.getSimpleName());

      Type[] arguments = paramType.getActualTypeArguments();
      if (arguments.length > 0) {
        sb.append('<');
        for (Type argType : arguments) {
          sb.append(getTypeName(argType));
          sb.append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append('>');
      }

      return sb.toString();
    }

    return type.toString();
  }

}
