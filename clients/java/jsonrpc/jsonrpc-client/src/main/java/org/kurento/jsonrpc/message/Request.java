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
 * Java representation for JSON RPC request. This class holds the information needed to invoke a
 * method on the server
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * @param
 *          <P>
 */
public class Request<P> extends Message {

  public static final String METHOD_FIELD_NAME = "method";

  public static final String POLL_METHOD_NAME = "poll";

  /**
   * Request identifier.
   */
  private Integer id;

  /**
   * Method to be invoked on the server
   */
  protected String method;

  /**
   * Method parameters
   */
  protected P params;

  /**
   * Default constructor.
   */
  public Request() {
  }

  /**
   * Parameterized constructor.
   *
   * @param method
   *          Thrift interface method
   * @param params
   *          Method parameters
   * @param id
   *          Request identifier
   * @param sessionId
   *          The session id associated to this request
   */
  public Request(String sessionId, Integer id, String method, P params) {
    this.sessionId = sessionId;
    this.method = method;
    this.params = params;
    this.id = id;
  }

  /**
   * Parameterized constructor.
   *
   * @param method
   *          Thrift interface method
   * @param params
   *          Method parameters
   * @param id
   *          Request identifier
   */
  public Request(Integer id, String method, P params) {
    this(null, id, method, params);
  }

  public Request(String method, P params) {
    this(null, null, method, params);
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public P getParams() {
    return params;
  }

  public void setParams(P params) {
    this.params = params;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public boolean isNotification() {
    return id == null;
  }

}