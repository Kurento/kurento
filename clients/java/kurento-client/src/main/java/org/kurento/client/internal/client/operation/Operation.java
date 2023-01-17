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
import org.kurento.client.internal.TFutureImpl;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RemoteObjectInvocationHandler;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public abstract class Operation {

  protected RomManager manager;
  protected TFutureImpl<Object> future;

  public abstract RequestAndResponseType createRequest(
      RomClientJsonRpcClient romClientJsonRpcClient);

  public void setManager(RomManager manager) {
    this.manager = manager;
  }

  public TFutureImpl<Object> getFuture() {
    if (future == null) {
      future = new TFutureImpl<>(this);
    }
    return future;
  }

  protected String getObjectRef(Object object) {
    return getRemoteObject(object).getObjectRef();
  }

  protected RemoteObject getRemoteObject(Object object) {
    return RemoteObjectInvocationHandler.getFor(object).getRemoteObject();
  }

  public void rollback(TransactionExecutionException ex) {
    if (future != null) {
      if (ex != null) {
        future.getFuture().setException(ex);
      } else {
        future.getFuture().cancel(true);
      }
    }
  }

  public abstract String getDescription();

  public abstract void processResponse(Object response);

}
