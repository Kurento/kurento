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

/**
 * This exception occurs when there is a communication error. This could happen either when trying
 * to reach KMS, or when the server is trying to send a response to the client.
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.1
 *
 */
public class TransportException extends JsonRpcException {

  private static final long serialVersionUID = -9166377169939591329L;

  public TransportException(String message, Throwable cause) {
    super(message, cause);
  }

  public TransportException(String message) {
    super(message);
  }

  public TransportException(Throwable cause) {
    super(cause);
  }

}
