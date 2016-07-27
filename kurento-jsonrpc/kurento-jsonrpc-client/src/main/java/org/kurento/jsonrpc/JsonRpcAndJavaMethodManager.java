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

package org.kurento.jsonrpc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thoughtworks.paranamer.AnnotationParanamer;
import com.thoughtworks.paranamer.Paranamer;

public class JsonRpcAndJavaMethodManager {

  private static final Logger log = LoggerFactory.getLogger(JsonRpcAndJavaMethodManager.class);

  private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  private Paranamer paranamer = new AnnotationParanamer();

  public void executeMethod(Method m, Object object, Transaction transaction,
      Request<JsonObject> request) throws IOException {

    try {

      Response<JsonElement> response =
          execJavaMethod(transaction.getSession(), object, m, transaction, request);

      if (response != null) {
        response.setId(request.getId());
        transaction.sendResponseObject(response);
      } else {
        transaction.sendVoidResponse();
      }

    } catch (InvocationTargetException e) {

      if (e.getCause() instanceof JsonRpcErrorException) {

        JsonRpcErrorException ex = (JsonRpcErrorException) e.getCause();

        transaction.sendError(ex.getError());

      } else {

        log.error(
            "Exception executing request " + request + ": " + e.getCause().getLocalizedMessage(),
            e.getCause());
        transaction.sendError(e.getCause());
      }

    } catch (Exception e) {
      log.error("Exception processing request " + request, e);
      transaction.sendError(e);
    }

  }

  private Response<JsonElement> execJavaMethod(Session session, Object object, Method m,
      Transaction transaction, Request<JsonObject> request)
          throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

    Object[] values = calculateParamValues(session, m, request);

    Object result = m.invoke(object, values);

    if (result == null) {
      return null;
    } else {
      return new Response<>(null, gson.toJsonTree(result));
    }
  }

  private Object[] calculateParamValues(Session session, Method m, Request<JsonObject> request) {

    JsonObject params = request.getParams();

    String[] parameterNames = paranamer.lookupParameterNames(m, true);
    Type[] parameterTypes = m.getGenericParameterTypes();

    Object[] values = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {

      values[i] = getValueFromParam(session, m, params, parameterNames[i], parameterTypes[i]);
    }

    if (log.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < parameterNames.length; i++) {
        sb.append(parameterNames[i] + "(" + parameterTypes[i] + ")=" + values[i] + ",");
      }
      sb.append("]");

      log.debug("Executing method {} with params {}", m.getName(), params);
    }
    return values;
  }

  private Object getValueFromParam(Session session, Method m, JsonObject params,
      String parameterName, Type genericType) {

    if (genericType instanceof Class) {

      Class<?> type = (Class<?>) genericType;

      if (Session.class.isAssignableFrom(type)) {
        return session;
      } else {

        // TODO Allow more types
        JsonElement jsonElement = params.get(parameterName);

        if (jsonElement != null) {
          return getAsJavaType(type, jsonElement);
        } else {
          // TODO Fail in this case
          if (type == boolean.class) {
            return false;
          } else if (type == int.class) {
            return 0;
          }
        }
      }

    } else {

      if (genericType instanceof ParameterizedType) {

        ParameterizedType genericMap = (ParameterizedType) genericType;

        if (Map.class.isAssignableFrom((Class<?>) genericMap.getRawType())
            && (genericMap.getActualTypeArguments()[0] == String.class)
            && (genericMap.getActualTypeArguments()[1] == String.class)) {

          Map<String, String> returnParams = new HashMap<String, String>();
          for (Entry<String, JsonElement> param : params.entrySet()) {
            String valueStr =
                !param.getValue().isJsonNull() ? param.getValue().getAsString() : null;
            returnParams.put(param.getKey(), valueStr);
          }

          return returnParams;
        }
      }
    }

    return null;
  }

  private Object getAsJavaType(Class<?> type, JsonElement jsonElement) {
    if (jsonElement.isJsonNull()) {
      return null;
    } else if (type == String.class) {
      return jsonElement.getClass().equals(JsonObject.class) ? jsonElement.toString()
          : jsonElement.getAsString();
    } else if (type == boolean.class) {
      return jsonElement.getAsBoolean();
    } else if (type.isEnum()) {
      return gson.fromJson(jsonElement, type);
    } else if (type == int.class) {
      return jsonElement.getAsInt();
    } else {
      return null;
    }
  }
}
