
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
