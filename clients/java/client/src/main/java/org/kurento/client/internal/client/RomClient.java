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

package org.kurento.client.internal.client;

import java.lang.reflect.Type;
import java.util.List;

import org.kurento.client.Continuation;
import org.kurento.client.internal.client.operation.Operation;
import org.kurento.jsonrpc.Props;

public interface RomClient {

  public abstract String create(String remoteClassName, Props constructorParams,
      Props genericProps);

  public abstract String create(String remoteClassName, Props constructorParams, Props genericProps,
      Continuation<String> cont);

  public abstract <E> E invoke(String objectRef, String methodName, Props params, Class<E> clazz);

  public abstract Object invoke(String objectRef, String operationName, Props operationParams,
      Type type);

  public abstract Object invoke(String objectRef, String operationName, Props operationParams,
      Type type, Continuation<?> cont);

  public abstract void release(String objectRef);

  public abstract void release(String objectRef, Continuation<Void> cont);

  public abstract String subscribe(String objectRef, String eventType);

  public abstract String subscribe(String objectRef, String type, Continuation<String> cont);

  public abstract void unsubscribe(String objectRef, String listenerSubscription);

  public abstract void unsubscribe(String objectRef, String listenerSubscription,
      Continuation<Void> cont);

  public abstract void transaction(List<Operation> operations);

  public abstract void transaction(List<Operation> operations, Continuation<Void> continuation);

  // Other methods --------------------------------------

  public abstract void addRomEventHandler(RomEventHandler eventHandler);

  public abstract void destroy();

  public abstract boolean isClosed();

}
