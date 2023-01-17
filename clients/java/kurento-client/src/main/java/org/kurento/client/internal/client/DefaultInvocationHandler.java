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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public abstract class DefaultInvocationHandler implements InvocationHandler {

  private static final Set<String> DEFAULT_METHODS = ImmutableSet.of("toString", "notify",
      "notifyAll", "wait", "getClass", "clone", "equals", "hashCode");

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    String methodName = method.getName();
    if (DEFAULT_METHODS.contains(methodName)) {
      return findMethod(this, methodName, args).invoke(this, args);
    }
    return internalInvoke(proxy, method, args);
  }

  protected Method findMethod(Object object, String methodName, Object[] args)
      throws NoSuchMethodException {

    methods: for (Method method : object.getClass().getMethods()) {
      if (method.getName().equals(methodName) && sameParams(method, args)) {
        int numParam = 0;
        for (Class<?> type : method.getParameterTypes()) {
          if (numParam < args.length && !type.isAssignableFrom(args[numParam].getClass())) {
            continue methods;
          }
          numParam++;
        }
        return method;
      }
    }

    throw new NoSuchMethodException(
        object.getClass().getName() + ":" + methodName + " Params: " + Arrays.toString(args));
  }

  private boolean sameParams(Method method, Object[] args) {
    if (args == null) {
      return method.getParameterAnnotations().length == 0;
    } else {
      return method.getParameterAnnotations().length == args.length;
    }
  }

  protected abstract Object internalInvoke(Object proxy, Method method, Object[] args)
      throws Throwable;

}