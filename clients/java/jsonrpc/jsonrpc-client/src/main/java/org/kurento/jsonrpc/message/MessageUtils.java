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

package org.kurento.jsonrpc.message;

import java.util.Map.Entry;

import static org.kurento.jsonrpc.JsonUtils.getGson;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageUtils {

  private static Logger log = LoggerFactory.getLogger(MessageUtils.class);

  public static <R> Response<R> convertResponse(Response<JsonElement> response,
      Class<R> resultClass) {

    R resultR = convertJsonTo(response.getResult(), resultClass);

    @SuppressWarnings("unchecked")
    Response<R> responseR = (Response<R>) response;

    responseR.setResult(resultR);

    return responseR;
  }

  @SuppressWarnings("unchecked")
  public static <P> Request<P> convertRequest(Request<? extends Object> request,
      Class<P> paramsClass) {

    P paramsP = null;
    Object params = request.getParams();
    if (params != null) {
      if (paramsClass.isAssignableFrom(params.getClass())) {
        paramsP = (P) params;
      } else if (params instanceof JsonElement) {
        paramsP = convertJsonTo((JsonElement) request.getParams(), paramsClass);
      } else {
        throw new ClassCastException();
      }
    }

    Request<P> requestP = (Request<P>) request;

    requestP.setParams(paramsP);

    return requestP;
  }

  private static <R> R convertJsonTo(JsonElement resultElement, Class<R> resultClass) {

    if (resultElement == null) {
      return null;
    }

    if (resultClass == null) {
      return null;
    }

    R resultR = null;
    if (resultClass == String.class || resultClass == Boolean.class
        || resultClass == Character.class || Number.class.isAssignableFrom(resultClass)
        || resultClass.isPrimitive()) {

      JsonElement value;
      if (resultElement.isJsonObject()) {
        JsonObject resultObject = resultElement.getAsJsonObject();

        Set<Entry<String, JsonElement>> properties = resultObject.entrySet();

        if (properties.size() > 1) {
          if (resultObject.has("value")) {
            value = resultObject.get("value");
          }
          else {
            Entry<String, JsonElement> prop = properties.iterator().next();

            log.warn(
                "Converting a result with {} properties into a value"
                    + " of type {}. Selecting property '{}'",
                Integer.valueOf(properties.size()), resultClass, prop.getKey());

            value = prop.getValue();
          }

        } else if (properties.size() == 1) {
          value = properties.iterator().next().getValue();
        } else {
          value = null;
        }

      } else if (resultElement.isJsonArray()) {
        JsonArray resultArray = resultElement.getAsJsonArray();

        if (resultArray.size() > 1) {
          log.warn(
              "Converting an array with {} elements into a value "
                  + "of type {}. Selecting first element",
              Integer.valueOf(resultArray.size()), resultClass);

        }

        value = resultArray.get(0);
      } else {
        value = resultElement;
      }

      resultR = getGson().fromJson(value, resultClass);
    } else {
      resultR = getGson().fromJson(resultElement, resultClass);
    }
    return resultR;
  }

}
