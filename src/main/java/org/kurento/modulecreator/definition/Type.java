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
package org.kurento.modulecreator.definition;

public abstract class Type extends NamedElement {

  protected transient ModuleDefinition module;

  public Type(String name, String doc) {
    super(name, doc);
  }

  public ModuleDefinition getModule() {
    return module;
  }

  public void setModule(ModuleDefinition module) {
    this.module = module;
  }

  /**
   * Get qualified name mixing In the format module.name
   *
   * @return The qualified name
   */
  public String getQualifiedName() {
    if (this.module == null || this.module.getName() == null) {
      return name;
    }
    if (this.module.getName().equals("core") || this.module.getName().equals("filters")
        || this.module.getName().equals("elements")) {
      return "kurento." + name;
    } else {
      return this.module.getName() + "." + name;
    }
  }

}
