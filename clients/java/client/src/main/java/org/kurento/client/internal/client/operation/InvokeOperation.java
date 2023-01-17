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

import java.lang.reflect.Type;

import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.RemoteObjectInvocationHandler;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.jsonrpc.Props;

public class InvokeOperation extends Operation {

  private static ParamsFlattener FLATTENER = ParamsFlattener.getInstance();

  private KurentoObject kurentoObject;
  private String method;
  private Props params;
  private Type returnType;

  public InvokeOperation(KurentoObject object, String method, Props params, Type returnType) {
    super();
    this.kurentoObject = object;
    this.method = method;
    this.params = params;
    this.returnType = returnType;
  }

  @Override
  public RequestAndResponseType createRequest(RomClientJsonRpcClient romClientJsonRpcClient) {

    Type flattenType = FLATTENER.calculateFlattenType(returnType);

    return romClientJsonRpcClient.createInvokeRequest(
        RemoteObjectInvocationHandler.getFor(kurentoObject).getRemoteObject().getObjectRef(),
        method, params, flattenType, true);
  }

  @Override
  public void processResponse(Object result) {

    if (returnType != Void.class && returnType != void.class) {

      future.getFuture().set(FLATTENER.unflattenValue("return", returnType, result, manager));
    }
  }

  @Override
  public String getDescription() {
    return "Invoking method '" + method + "' in object " + getObjectRef(kurentoObject)
        + "' with params " + params;
  }
}
