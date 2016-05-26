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
package org.kurento.modulecreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kurento.modulecreator.definition.ModuleDefinition;

public class ModuleManager {

  private final Map<String, ModuleDefinition> modules;
  private ModuleManager dependencies;

  public ModuleManager() {
    this.modules = new HashMap<>();
  }

  public void resolveModules() {
    for (ModuleDefinition module : modules.values()) {
      module.resolveModule(this);
    }
  }

  public ModuleDefinition getModule(String name) {
    ModuleDefinition module = modules.get(name);
    if (module != null) {
      return module;
    } else {
      if (dependencies != null) {
        return dependencies.getModule(name);
      }
    }
    return null;
  }

  public ModuleDefinition getModule(String name, String version) {
    ModuleDefinition module = modules.get(name);
    if (module != null) {
      if (module.getVersion().equals(version)) {
        return module;
      }
    } else {
      if (dependencies != null) {
        return dependencies.getModule(name, version);
      }
    }

    return null;
  }

  private void removeModule(String name) {
    ModuleDefinition module = modules.get(name);
    if (module != null) {
      modules.remove(module.getName());
    }

    if (dependencies != null) {
      dependencies.removeModule(name);
    }
  }

  public void setDependencies(ModuleManager dependencies) {
    this.dependencies = dependencies;
    List<String> toRemove = new ArrayList<String>();

    for (ModuleDefinition module : dependencies.getModules()) {
      if (modules.get(module.getName()) != null) {
        toRemove.add(module.getName());
      }
    }

    for (String name : toRemove) {
      this.dependencies.removeModule(name);
    }
  }

  public Collection<ModuleDefinition> getModules() {
    return modules.values();
  }

  public void addModules(List<ModuleDefinition> modules) {
    for (ModuleDefinition module : modules) {
      module.validateModule();
      addModule(module);
    }
  }

  public void addModule(ModuleDefinition module) {
    this.modules.put(module.getName(), module);
  }

  public void addModuleInSeveralKmdFiles(List<ModuleDefinition> modules) {
    ModuleDefinition module = modules.get(0);
    for (int i = 1; i < modules.size(); i++) {
      module.fusionModules(modules.get(i));
    }
    addModule(module);
  }
}
