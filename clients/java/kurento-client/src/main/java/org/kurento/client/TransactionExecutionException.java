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

package org.kurento.client;

import org.kurento.client.internal.client.operation.Operation;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.jsonrpc.message.ResponseError;

public class TransactionExecutionException extends KurentoServerException {

  private static final long serialVersionUID = 6694105597823767195L;

  public TransactionExecutionException(Operation operation, ResponseError error) {
    super(createExceptionMessage(operation, error), error);
  }

  private static String createExceptionMessage(Operation operation, ResponseError error) {
    return "Error '" + error.getCompleteMessage() + "' executing operation '"
        + operation.getDescription() + "'";
  }

}
