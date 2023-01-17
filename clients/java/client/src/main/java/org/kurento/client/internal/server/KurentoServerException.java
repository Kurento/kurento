/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.client.internal.server;

import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.message.ResponseError;

/**
 * This exception represents errors that take place in Kurento Server, while operating with
 * pipelines and media elements
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.1
 *
 */
public class KurentoServerException extends KurentoException {

  private static final long serialVersionUID = -4925041543188451274L;

  private ResponseError error;

  protected KurentoServerException(String message, ResponseError error) {
    super(message);
    this.error = error;
  }

  public KurentoServerException(ResponseError error) {
    super(error.getCompleteMessage());
    this.error = error;
  }

  public String getServerMessage() {
    return error.getMessage();
  }

  public String getData() {
    return error.getData();
  }

  public String getErrorType() {
    return error.getType();
  }

  public int getCode() {
    return error.getCode();
  }

  public ResponseError getError() {
    return error;
  }
}
