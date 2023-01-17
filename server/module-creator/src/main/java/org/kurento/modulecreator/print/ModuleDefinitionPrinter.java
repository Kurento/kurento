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
package org.kurento.modulecreator.print;

import java.util.List;

import org.kurento.modulecreator.definition.ComplexType;
import org.kurento.modulecreator.definition.DataItem;
import org.kurento.modulecreator.definition.Event;
import org.kurento.modulecreator.definition.Method;
import org.kurento.modulecreator.definition.ModuleDefinition;
import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;

public class ModuleDefinitionPrinter {

  public static String printModel(ModuleDefinition model) {

    StringBuilder sb = new StringBuilder();

    sb.append("Media objects (remote)\n");
    sb.append("----------------------------------------\n\n");
    for (RemoteClass remoteClass : model.getRemoteClasses()) {
      printRemoteClass(sb, remoteClass);
      sb.append("\n");
      sb.append("------------------------------------\n");
    }
    sb.append("\n");

    sb.append("Event types\n");
    sb.append("----------------------------------------\n\n");

    for (Event event : model.getEvents()) {
      sb.append("    ");
      printEvent(sb, event);
      sb.append("\n");
    }
    sb.append("\n");
    sb.append("\n");

    sb.append("Method parameter and return types\n");
    sb.append("-----------------------------------------\n\n");
    for (ComplexType type : model.getComplexTypes()) {
      sb.append("    ");
      printType(sb, type);
      sb.append("\n");
    }

    return sb.toString();
  }

  public static void printType(StringBuilder sb, ComplexType type) {

    sb.append(type.getName()).append(" ");
    switch (type.getTypeFormat()) {
      case ENUM:
        sb.append(type.getValues());
        break;
      case REGISTER:
        sb.append("{ ");
        printDataItems(sb, type.getProperties());
        sb.append(" }");
        break;
      default:
        sb.append("Unknown");
        break;
    }
  }

  public static void printEvent(StringBuilder sb, Event event) {
    sb.append(event.getName()).append(" ");
    if (event.getExtends() != null) {
      sb.append("extends ");
      printTypeRef(sb, event.getExtends());
      sb.append(" ");
    }

    sb.append("{ ");
    printDataItems(sb, event.getProperties());
    sb.append(" }");
  }

  public static void printRemoteClass(StringBuilder sb, RemoteClass remoteClass) {

    if (remoteClass.isAbstract()) {
      sb.append("abstract ");
    }

    sb.append("class ");
    sb.append(remoteClass.getName());
    if (remoteClass.getExtends() != null) {
      sb.append(" extends ");
      sb.append(remoteClass.getExtends().getName());
    }
    sb.append("\n");
    sb.append("\n");

    if (remoteClass.getConstructor() != null) {
      sb.append("   Declared Constructor: \n");
      sb.append("       ");
      printMethod(sb, remoteClass.getConstructor(), true);
      sb.append("\n");
    }

    if (!remoteClass.getMethods().isEmpty()) {
      sb.append("   Declared Methods: \n");
      for (Method method : remoteClass.getMethods()) {
        sb.append("       ");
        printMethod(sb, method, false);
      }
      sb.append("\n");
    }

    if (!remoteClass.getEvents().isEmpty()) {
      sb.append("   Declared Events: \n");
      for (TypeRef event : remoteClass.getEvents()) {
        sb.append("       ");
        sb.append(event.getName());
        sb.append("\n");
      }
      sb.append("\n");
    }
  }

  public static void printMethod(StringBuilder sb, Method method, boolean constructor) {

    Return methodReturn = method.getReturn();

    if (methodReturn != null) {
      printTypeRef(sb, methodReturn.getType());
      sb.append(" ");
    } else {
      if (!constructor) {
        sb.append("void ");
      }
    }

    if (method.getName() != null) {
      sb.append(method.getName());
    }

    sb.append("(");

    printDataItems(sb, method.getParams());

    sb.append(")\n");
  }

  public static void printDataItems(StringBuilder sb, List<? extends DataItem> items) {
    for (DataItem item : items) {
      printTypeRef(sb, item.getType());
      sb.append(" ");
      sb.append(item.getName());
      if (item.isOptional()) {
        sb.append("?");
      }
      sb.append(", ");
    }

    if (!items.isEmpty()) {
      sb.deleteCharAt(sb.length() - 1);
      sb.deleteCharAt(sb.length() - 1);
    }
  }

  private static void printTypeRef(StringBuilder sb, TypeRef type) {
    sb.append(type.getName());
    if (type.isList()) {
      sb.append("[]");
    }
  }

}
