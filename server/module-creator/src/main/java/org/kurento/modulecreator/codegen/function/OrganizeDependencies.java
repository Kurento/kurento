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
package org.kurento.modulecreator.codegen.function;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Type;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class OrganizeDependencies implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    Map<String, String> text = new HashMap<String, String>();

    if (arguments.size() != 2) {
      throw new TemplateModelException("2 argument is required");
    }

    if (!(arguments.get(0) instanceof SimpleSequence)) {
      throw new TemplateModelException("Class not expected");
    }
    SimpleSequence seq = (SimpleSequence) arguments.get(0);
    boolean isImpl = ((TemplateBooleanModel) arguments.get(1)).getAsBoolean();

    for (Object argument : seq.toList()) {
      if (argument instanceof Type) {
        Type dependency = (Type) argument;
        String namespace = dependency.getModule().getCode().getImplementation().get("cppNamespace");
        String classesText = "";
        if (text.containsKey(namespace)) {
          // insert new class in existing namespace
          classesText = text.get(namespace);
          text.remove(namespace);
          classesText += "\n";
        } else {
          // create new namespace and insert new class
          for (String a : namespace.split("\\::")) {
            classesText += "namespace " + a + "\n{\n";
          }
        }

        if (isImpl && (dependency instanceof RemoteClass)) {
          classesText += "class " + dependency.getName() + "Impl;";
        } else {
          classesText += "class " + dependency.getName() + ";";
        }
        text.put(namespace, classesText);
      }
    }

    Iterator<String> it = text.keySet().iterator();
    String ret = "";
    while (it.hasNext()) {
      ret += "\n";
      String key = it.next();
      ret += text.get(key);
      String[] split = key.split("\\::");
      ret += "\n";
      for (int i = split.length - 1; i >= 0; i--) {
        ret += "} /* " + split[i] + " */\n";
      }
    }

    return ret;
  }

}
