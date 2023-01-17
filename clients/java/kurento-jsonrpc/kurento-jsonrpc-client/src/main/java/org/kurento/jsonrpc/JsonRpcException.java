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

package org.kurento.jsonrpc;

import org.kurento.commons.exception.KurentoException;

/**
 * This is a general exception used in the JsonRpcConnector package.
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.3
 */
public class JsonRpcException extends KurentoException {

  private static final long serialVersionUID = -9166377169939591329L;

  public JsonRpcException(String message, Throwable cause) {
    super(message, cause);
  }

  public JsonRpcException(String message) {
    super(message);
  }

  public JsonRpcException(Throwable cause) {
    super(cause);
  }

}
