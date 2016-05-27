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

/**
 *
 * Java representation for JSON media connector request.
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * @param <R>
 */
public class Response<R> extends Message {

  /**
   * Request identifier related to this response
   */
  private Integer id;

  /**
   * Method result
   */
  private R result;

  /**
   * Error produced executing method
   */
  private ResponseError error;

  /**
   * Default constructor.
   */
  public Response() {
  }

  public Response(R result) {
    this(null, null, result);
  }

  public Response(ResponseError error) {
    this(null, null, error);
  }

  public Response(Integer id, R result) {
    this(null, id, result);
  }

  public Response(Integer id, ResponseError error) {
    this(null, id, error);
  }

  public Response(Integer id) {
    super(null);
    this.id = id;
  }

  public Response(String sessionId, Integer id, R result) {
    super(sessionId);
    this.id = id;
    this.result = result;
  }

  public Response(String sessionId, Integer id, ResponseError error) {
    super(sessionId);
    this.id = id;
    this.error = error;
  }

  public R getResult() {
    return result;
  }

  public void setResult(R result) {
    this.result = result;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ResponseError getError() {
    return error;
  }

  public void setError(ResponseError error) {
    this.error = error;
  }

  public boolean isError() {
    return error != null;
  }
}
