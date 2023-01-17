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

package org.kurento.test.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.kurento.test.base.KurentoTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test watcher for tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTestWatcher extends TestWatcher {

  public static Logger log = LoggerFactory.getLogger(KurentoTestWatcher.class);

  private static boolean succees = false;

  @Override
  protected void succeeded(Description description) {
    KurentoTest.logMessage("|       TEST SUCCEEDED: " + description.getClassName() + "."
        + description.getMethodName());

    invokeMethodsAnnotatedWith(SucceededTest.class, description.getTestClass(), null, description);
    succees = true;
  }

  @Override
  protected void failed(Throwable e, Description description) {
    KurentoTest.logMessage(
        "|       TEST FAILED: " + description.getClassName() + "." + description.getMethodName());

    invokeMethodsAnnotatedWith(FailedTest.class, description.getTestClass(), e, description);
    succees = false;
  }

  public static void invokeMethodsAnnotatedWith(Class<? extends Annotation> annotation,
      Class<?> testClass, Throwable throwable, Description description) {
    List<Method> methods = getMethodsAnnotatedWith(testClass, annotation);
    invokeMethods(methods, annotation, throwable, description);
  }

  public static boolean isSuccees() {
    return succees;
  }

  public static void invokeMethods(List<Method> methods, Class<? extends Annotation> annotation,
      Throwable throwable, Description description) {
    for (Method method : methods) {
      log.debug("Invoking method {} annotated with {}", method, annotation);

      try {
        if (!Modifier.isPublic(method.getModifiers())) {
          log.warn("Method {} is not public and it cannot be invoked", method);
          continue;
        }

        if (!Modifier.isStatic(method.getModifiers())) {
          log.warn("Method {} is not static and it cannot be invoked", method);
          continue;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();

        switch (parameterTypes.length) {
          case 0:
            method.invoke(null);
            break;

          case 1:
            if (parameterTypes[0].equals(Throwable.class)) {
              method.invoke(null, throwable);
            } else if (parameterTypes[0].equals(Description.class)) {
              method.invoke(null, description);
            } else {
              log.warn("Method {} annotated with {} cannot be invoked." + " Incorrect argument: {}",
                  method, annotation, parameterTypes[0]);
            }
            break;

          case 2:
            Object param1 = parameterTypes[0].equals(Throwable.class) ? throwable
                : parameterTypes[0].equals(Description.class) ? description : null;
            Object param2 = parameterTypes[1].equals(Throwable.class) ? throwable
                : parameterTypes[1].equals(Description.class) ? description : null;

            if (param1 != null && param2 != null) {
              method.invoke(null, param1, param2);
            } else {
              log.warn(
                  "Method {} annotated with {} cannot be invoked." + " Incorrect arguments: {}, {}",
                  method, annotation, parameterTypes[0], parameterTypes[1]);
            }
            break;

          default:
            log.warn("Method {} annotated with {} cannot be invoked." + " Incorrect arguments: {}",
                method, annotation, Arrays.toString(parameterTypes));
            break;
        }

      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        log.warn("Exception invoking method {} annotated with {}: {} {}", method, e.getClass(),
            e.getMessage());
      }
    }
  }

  public static List<Method> getMethodsAnnotatedWith(Class<?> clazz,
      Class<? extends Annotation> annotation) {
    List<Method> methods = new ArrayList<>();
    while (clazz != Object.class) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(annotation)) {
          methods.add(method);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return methods;
  }

}
