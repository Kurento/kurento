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

package org.kurento.client.internal.transport.serialization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.internal.server.ProtocolException;

public class ModuleClassesManager {

  private final ConcurrentHashMap<String, String> pkgNamesByModuleName = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Class<?>> classesByClassName = new ConcurrentHashMap<>();

  public Class<?> getClassFor(String fullyTypeName) {
    String[] parts = fullyTypeName.split("\\.");
    return getClassFor(parts[0], parts[1]);
  }

  public Class<?> getClassFor(String moduleName, String typeName) {

    Objects.requireNonNull(typeName, "typeName must not be null");
    Objects.requireNonNull(moduleName, "moduleName must not be null");

    try {

      String packageName = pkgNamesByModuleName.get(moduleName);

      if (packageName == null) {

        packageName = getPackageNameWithModuleInfoClass(moduleName);

        pkgNamesByModuleName.put(moduleName, packageName);
      }

      String className = packageName + "." + typeName;

      Class<?> clazz = classesByClassName.get(className);

      if (clazz == null) {
        clazz = Class.forName(className);
        classesByClassName.put(className, clazz);
      }

      return clazz;

    } catch (Exception e) {
      throw new ProtocolException(
          "Exception creating Java Class for '" + moduleName + "." + typeName + "'", e);
    }
  }

  private String getPackageNameWithModuleInfoClass(String moduleName) throws ClassNotFoundException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    String moduleInfoClassName = getModuleInfoClassName(moduleName);
    Class<?> clazzPackage = Class.forName(moduleInfoClassName);
    Method method = clazzPackage.getMethod("getPackageName");
    return (String) method.invoke(clazzPackage);
  }

  private String getModuleInfoClassName(String moduleName) {

    String moduleNameWithFirstUpper = moduleName.substring(0, 1).toUpperCase()
        + moduleName.substring(1, moduleName.length());

    String classPackageName = "org.kurento.module." + moduleNameWithFirstUpper + "ModuleInfo";

    return classPackageName;
  }

}
