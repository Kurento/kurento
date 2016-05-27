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

package org.kurento.client.internal.client.operation;

import org.kurento.client.TransactionExecutionException;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;
import org.kurento.jsonrpc.Props;

public class MediaObjectCreationOperation extends Operation {

  public String className;
  public Props constructorParams;
  private Props genericProps;
  private RemoteObject remoteObject;

  public MediaObjectCreationOperation(String className, Props constructorParams, Props genericProps,
      RemoteObject remoteObject) {
    this.className = className;
    this.constructorParams = constructorParams;
    this.genericProps = genericProps;
    this.remoteObject = remoteObject;
  }

  @Override
  public RequestAndResponseType createRequest(RomClientJsonRpcClient romClientJsonRpcClient) {
    return romClientJsonRpcClient.createCreateRequest(className, constructorParams, genericProps,
        true);
  }

  @Override
  public void processResponse(Object response) {
    remoteObject.setCreatedObjectRef((String) response);
  }

  @Override
  public String getDescription() {
    return "Object creation of type '" + className + "' with params " + constructorParams;
  }

  @Override
  public void rollback(TransactionExecutionException ex) {
    super.rollback(ex);
    remoteObject.rollbackTransaction(ex);
  }
}
