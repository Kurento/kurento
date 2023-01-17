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

package org.kurento.client.internal.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.Props;

public class RomServer {

  private final RemoteObjectManager manager = new RemoteObjectManager();

  private static ParamsFlattener FLATTENER = ParamsFlattener.getInstance();

  private final String packageName;
  private final String classSuffix;

  public RomServer(String packageName, String classSuffix) {
    this.packageName = packageName;
    this.classSuffix = classSuffix;
  }

  public String create(String remoteClassType, Props constructorParams) {

    try {

      Class<?> clazz = Class.forName(packageName + "." + remoteClassType + classSuffix);

      if (clazz.getAnnotation(RemoteClass.class) == null) {
        throw new ProtocolException("Remote classes must be annotated with @RemoteClass");
      }

      Constructor<?> constructor = clazz.getConstructors()[0];

      Object[] unflattenedConstParams = FLATTENER.unflattenParams(
          constructor.getParameterAnnotations(), constructor.getGenericParameterTypes(),
          constructorParams, manager);

      Object object = constructor.newInstance(unflattenedConstParams);

      return manager.putObject(object);

    } catch (Exception e) {
      // TODO Improve exception reporting
      throw new ProtocolException("Exception while creating an object with remoteClass='"
          + remoteClassType + "' and params=" + constructorParams, e);
    }
  }

  @SuppressWarnings("unchecked")
  public <E> E invoke(String objectRef, String methodName, Props params, Class<E> clazz) {
    return (E) invoke(objectRef, methodName, params, (Type) clazz);
  }

  public Object invoke(String objectRef, String methodName, Props params, Type type) {

    Object remoteObject = manager.getObject(objectRef);

    if (remoteObject == null) {
      throw new KurentoException("No object found with reference " + objectRef);
    }

    Class<?> remoteObjClass = remoteObject.getClass();

    try {

      Method method = getMethod(remoteObjClass, methodName);

      Object[] unflattenParams = FLATTENER.unflattenParams(method.getParameterAnnotations(),
          method.getGenericParameterTypes(), params, manager);

      Object result = method.invoke(remoteObject, unflattenParams);

      return FLATTENER.flattenResult(result, manager);

    } catch (Exception e) {
      // TODO Improve exception reporting
      throw new ProtocolException("Invocation exception of object with remoteClass='"
          + remoteObjClass.getSimpleName() + "', method=" + methodName + " and params=" + params,
          e);
    }
  }

  private Method getMethod(Class<?> remoteObjClass, String methodName) {
    for (Method method : remoteObjClass.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    throw new ProtocolException("Method '" + methodName + "' not found in class '"
        + remoteObjClass.getClass().getSimpleName() + "'");
  }

  public void release(String objectRef) {
    this.manager.releaseObject(objectRef);
  }
}
