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

public class Import {

  private String name;
  private String version;
  private String mavenVersion;
  private String npmVersion;

  private transient ModuleDefinition module;

  public Import(String name, String version) {
    super();
    this.name = name;
    this.version = version;
  }

  public Import(String name, String version, String mavenVersion, String npmVersion) {
    super();
    this.name = name;
    this.version = version;
    this.mavenVersion = mavenVersion;
    this.npmVersion = npmVersion;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public void setModule(ModuleDefinition module) {
    this.module = module;
  }

  public ModuleDefinition getModule() {
    return module;
  }

  @Override
  public String toString() {
    return name + "(" + version + ")";
  }

  public String getMavenVersion() {
    return mavenVersion;
  }

  public String getNpmVersion() {
    return npmVersion;
  }

  public void setMavenVersion(String mavenVersion) {
    this.mavenVersion = mavenVersion;
  }

  public void setNpmVersion(String npmVersion) {
    this.npmVersion = npmVersion;
  }
}