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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.kurento.jsonrpc.JsonRpcErrorException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ResponseError {

  private static final String TYPE_PROPERTY = "type";

  /**
   * Error status code.
   */
  private Integer code;

  /**
   * Error message.
   */
  private String message;

  /**
   * Error data.
   */
  private JsonElement data;

  /**
   * Error type.
   */
  private String type;

  public static ResponseError newFromException(Throwable e) {
    return newFromException(-1, null, e);
  }

  public static ResponseError newFromException(String message, Throwable e) {
    return newFromException(-1, message, e);
  }

  public static ResponseError newFromException(int requestId, Throwable e) {
    return newFromException(requestId, null, e);
  }

  public static ResponseError newFromException(int requestId, String message, Throwable e) {

    if (e instanceof JsonRpcErrorException) {

      JsonRpcErrorException jsonRpcError = (JsonRpcErrorException) e;

      return new ResponseError(jsonRpcError.getCode(), jsonRpcError.getMessage(),
          jsonRpcError.getData());

    } else {

      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));

      String exceptionMessage = "";

      if (message != null) {
        exceptionMessage += message;
      }

      exceptionMessage += e.getClass().getName() + ":" + e.getMessage();

      return new ResponseError(requestId, exceptionMessage, writer.toString());
    }
  }

  /**
   * Default constructor.
   */
  public ResponseError() {
  }

  /**
   * Parameterised constructor.
   *
   * @param code
   *          Error status code
   * @param message
   *          Error message
   * @param data
   *          Error data
   */
  public ResponseError(int code, String message, String data) {
    this.code = Integer.valueOf(code);
    this.message = message;
    if (data != null) {
      this.data = new JsonPrimitive(data);
    }
  }

  public ResponseError(int code, String message, JsonElement data) {
    this.code = Integer.valueOf(code);
    this.message = message;
    this.data = data;
    this.type = getErrorType(data);
  }

  public ResponseError(int code, String type, String message, String data) {
    this.code = Integer.valueOf(code);
    this.message = message;
    if (data != null) {
      this.data = new JsonPrimitive(data);
    }
    this.type = type;
  }

  public ResponseError(int code, String message) {
    this.code = Integer.valueOf(code);
    this.message = message;
  }

  /**
   * Error status code accessor (getter).
   *
   * @return Error status code
   */
  public int getCode() {
    return code != null ? code.intValue() : 0;
  }

  /**
   * Error status code mutator (setter).
   *
   * @param code
   *          Error status code
   */
  void setCode(int code) {
    this.code = Integer.valueOf(code);
  }

  /**
   * Error message accessor (getter).
   *
   * @return Error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Error message mutator (setter).
   *
   * @param message
   *          Error message
   */
  void setMessage(String message) {
    this.message = message;
  }

  /**
   * Error data accessor (getter).
   *
   * @return Error data
   */
  public String getData() {
    if (data instanceof JsonPrimitive) {
      return ((JsonPrimitive) data).getAsString();
    } else if (data != null) {
      return data.toString();
    } else {
      return null;
    }
  }

  /**
   * Error data mutator (setter).
   *
   * @param data
   *          Error data
   */
  void setData(String data) {
    this.data = new JsonPrimitive(data);
  }

  public String getType() {
    return type;
  }

  public String getCompleteMessage() {
    return message + " (Code:" + code + ", Type:" + type + ", Data: " + data + ")";
  }

  private static String getErrorType(JsonElement data) {
    if (data != null) {
      if (data instanceof JsonObject) {
        JsonObject dataObject = (JsonObject) data;
        JsonElement typeProp = dataObject.get(TYPE_PROPERTY);
        if (typeProp instanceof JsonPrimitive) {
          return ((JsonPrimitive) typeProp).getAsString();
        }
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return "ResponseError [code=" + code + ", message=" + message + ", data=" + data + ", type="
        + type + "]";
  }
}