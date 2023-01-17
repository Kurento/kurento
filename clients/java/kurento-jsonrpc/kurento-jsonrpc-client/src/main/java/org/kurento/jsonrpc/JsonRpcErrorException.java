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

package org.kurento.jsonrpc;

import org.kurento.jsonrpc.message.ResponseError;

import com.google.gson.JsonElement;

public class JsonRpcErrorException extends JsonRpcException {

  private static final long serialVersionUID = 1584953670536766280L;

  private final ResponseError error;

  public JsonRpcErrorException(int code, String message) {
    this(new ResponseError(code, message));
  }

  public JsonRpcErrorException(int code, String message, JsonElement data) {
    this(new ResponseError(code, message, data));
  }

  public JsonRpcErrorException(int code, String message, Exception e) {
    this(ResponseError.newFromException(e));
  }

  public JsonRpcErrorException(ResponseError error) {
    super(createExceptionMessage(error));
    this.error = error;
  }

  private static String createExceptionMessage(ResponseError error) {

    String message = error.getMessage();

    if (error.getCode() != 0) {
      message += ". Code: " + error.getCode();
    }

    if (error.getData() != null) {
      message += ". Data: " + error.getData();
    }

    return message;
  }

  public ResponseError getError() {
    return error;
  }

  public String getData() {
    return error.getData();
  }

  public int getCode() {
    return error.getCode();
  }

  public String getServerMessage() {
    return error.getMessage();
  }

}
