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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.ModuleManager;
import org.kurento.modulecreator.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleDefinition {

  private static final String FILTERS_MODULE = "filters";
  private static final String ELEMENTS_MODULE = "elements";
  private static final String CORE_MODULE = "core";

  private static final Set<String> AUTO_IMPORTED_MODULES = new HashSet<>(
      Arrays.asList(CORE_MODULE, ELEMENTS_MODULE, FILTERS_MODULE));

  private static enum ResolutionState {
    NO_RESOLVED, IN_PROCESS, RESOLVED
  }

  public static final PrimitiveType STRING = new PrimitiveType("String");
  public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean");
  public static final PrimitiveType INT = new PrimitiveType("int");
  public static final PrimitiveType FLOAT = new PrimitiveType("float");
  public static final PrimitiveType DOUBLE = new PrimitiveType("double");
  public static final PrimitiveType INT64 = new PrimitiveType("int64");

  private static Logger log = LoggerFactory.getLogger(ModuleDefinition.class);

  /* Kmd file info */
  private String name;
  private String version;

  private String kurentoVersion;
  private String kurentoMavenVersion;
  private String kurentoNpmVersion;

  private List<Import> imports;
  private String repository;

  private Code code;

  private List<RemoteClass> remoteClasses;
  private List<ComplexType> complexTypes;
  private List<Event> events;

  /* Derived properties */
  private transient Map<String, RemoteClass> remoteClassesMap;
  private transient Map<String, Event> eventsMap;
  private transient Map<String, ComplexType> complexTypesMap;

  private transient Map<String, Type> types;

  private transient ResolutionState resolutionState = ResolutionState.NO_RESOLVED;
  private transient Map<String, Type> allTypes;

  public ModuleDefinition(String name, String version) {
    this();
    this.name = name;
    this.version = version;
  }

  public ModuleDefinition() {
    this.remoteClasses = new ArrayList<>();
    this.complexTypes = new ArrayList<>();
    this.events = new ArrayList<>();
    this.imports = new ArrayList<>();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ModuleDefinition other = (ModuleDefinition) obj;
    if (events == null) {
      if (other.events != null) {
        return false;
      }
    } else if (!events.equals(other.events)) {
      return false;
    }
    if (remoteClasses == null) {
      if (other.remoteClasses != null) {
        return false;
      }
    } else if (!remoteClasses.equals(other.remoteClasses)) {
      return false;
    }
    if (complexTypes == null) {
      if (other.complexTypes != null) {
        return false;
      }
    } else if (!complexTypes.equals(other.complexTypes)) {
      return false;
    }
    return true;
  }

  public List<Event> getEvents() {
    return events;
  }

  public List<RemoteClass> getRemoteClasses() {
    return remoteClasses;
  }

  public List<ComplexType> getComplexTypes() {
    return complexTypes;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((events == null) ? 0 : events.hashCode());
    result = prime * result + ((remoteClasses == null) ? 0 : remoteClasses.hashCode());
    result = prime * result + ((complexTypes == null) ? 0 : complexTypes.hashCode());
    return result;
  }

  public void addEvent(Event event) {
    this.events.add(event);
  }

  public void addRemoteClass(RemoteClass remoteClass) {
    this.remoteClasses.add(remoteClass);
  }

  public void addType(ComplexType type) {
    this.complexTypes.add(type);
  }

  public void setEvents(List<Event> events) {
    this.events = events;
  }

  public void setRemoteClasses(List<RemoteClass> remoteClasses) {
    this.remoteClasses = remoteClasses;
  }

  public void setComplexTypes(List<ComplexType> types) {
    this.complexTypes = types;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKurentoVersion() {
    return kurentoVersion;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public RemoteClass getRemoteClass(String remoteClassName) {
    return remoteClassesMap.get(remoteClassName);
  }

  public ComplexType getType(String typeName) {
    return complexTypesMap.get(typeName);
  }

  public Event getEvent(String eventName) {
    return eventsMap.get(eventName);
  }

  public String getRepository() {
    return repository;
  }

  public Code getCode() {
    return code;
  }

  public List<Import> getImports() {
    return imports;
  }

  public Collection<Import> getAllImports() {

    Map<String, Import> allImports = new HashMap<String, Import>();
    getAllImports(allImports);
    return allImports.values();
  }

  private void getAllImports(Map<String, Import> allImports) {
    for (Import importInfo : imports) {
      if (allImports.get(importInfo.getName()) == null) {
        allImports.put(importInfo.getName(), importInfo);
        importInfo.getModule().getAllImports(allImports);
      }
    }
  }

  @Override
  public String toString() {
    return "Model [name=" + name + ", version=" + version + ", remoteClasses=" + remoteClasses
        + ", types=" + complexTypes + ", events=" + events + "]";
  }

  public void validateModule() {

    if (name == null) {
      throw new KurentoModuleCreatorException("Name is mandatory at least in one of the files");
    }

    if (kurentoVersion == null) {
      if (AUTO_IMPORTED_MODULES.contains(name)) {
        kurentoVersion = version;
      } else {
        throw new KurentoModuleCreatorException(
            "Kurento version is mandatory at least in one of the files describing: " + name);
      }
    }

    if (version == null) {
      throw new KurentoModuleCreatorException("Version is mandatory at least in one of the files");
    }

    if (VersionManager.isReleaseVersion(version)) {
      for (Import importInfo : this.imports) {
        if (!VersionManager.isReleaseVersion(importInfo.getVersion())) {
          throw new KurentoModuleCreatorException(
              "All dependencies of a release version must be also release versions. Import '"
                  + importInfo.getName() + "' is in non release version "
                  + importInfo.getVersion());
        }
      }
    }
  }

  public void resolveModel() {
    resolveModule(null);
  }

  public void resolveModule(ModuleManager moduleManager) {

    setModuleToTypes();

    if (resolutionState == ResolutionState.IN_PROCESS) {
      throw new KurentoModuleCreatorException(
          "Found a dependency cycle in plugin '" + this.name + "'");
    }

    if (resolutionState == ResolutionState.RESOLVED) {
      log.debug("Module '" + name + "' yet resolved");
      return;
    }

    log.debug("Resolving module '" + name + "'");

    this.resolutionState = ResolutionState.IN_PROCESS;

    if (kurentoVersion == null && version != null) {
      kurentoVersion = version;
    }

    resolveImports(moduleManager);
    resolveTypes(moduleManager);
    addInfoForGeneration(moduleManager);

    log.debug("Module '" + name + "' resolved");

    this.resolutionState = ResolutionState.RESOLVED;
  }

  private void setModuleToTypes() {
    for (ComplexType complexType : complexTypes) {
      complexType.setModule(this);
    }

    for (RemoteClass remoteClass : remoteClasses) {
      remoteClass.setModule(this);
    }

    for (Event event : events) {
      event.setModule(this);
    }
  }

  private void addInfoForGeneration(ModuleManager moduleManager) {
    if (this.code == null) {
      this.code = new Code();
    }
    this.code.completeInfo(this, moduleManager);
  }

  private void resolveTypes(ModuleManager moduleManager) {

    remoteClassesMap = resolveNamedElements(this.remoteClasses);
    eventsMap = resolveNamedElements(this.events);
    complexTypesMap = resolveNamedElements(this.complexTypes);

    types = new HashMap<String, Type>();
    types.putAll(remoteClassesMap);
    types.putAll(eventsMap);
    types.putAll(complexTypesMap);

    put(types, BOOLEAN);
    put(types, STRING);
    put(types, INT);
    put(types, FLOAT);
    put(types, DOUBLE);
    put(types, INT64);

    allTypes = new HashMap<String, Type>(types);

    for (Import importEntry : this.imports) {
      allTypes.putAll(importEntry.getModule().getAllTypes());
    }

    resolveTypeRefs(remoteClasses, allTypes);
    resolveTypeRefs(events, allTypes);
    resolveTypeRefs(complexTypes, allTypes);
  }

  private void resolveImports(ModuleManager moduleManager) {

    autoImportModules(moduleManager);

    for (Import importEntry : this.imports) {

      ModuleDefinition dependencyModule = null;

      if (moduleManager != null) {
        dependencyModule = moduleManager.getModule(importEntry.getName());
      }

      if (dependencyModule == null) {
        throw new KurentoModuleCreatorException(
            "Import '" + importEntry.getName() + "' not found in dependencies in any version");
      } else {

        if (!VersionManager.compatibleVersion(importEntry.getVersion(),
            dependencyModule.getVersion())) {

          if (VersionManager.devCompatibleVersion(importEntry.getVersion(),
              dependencyModule.getVersion())) {

            log.info("[WARNING] Dependency on module '" + importEntry.getName() + "' version '"
                + importEntry.getVersion() + "' is satisfied with version '"
                + dependencyModule.getVersion() + "'");
          } else {

            throw new KurentoModuleCreatorException("Import '" + importEntry.getName()
                + "' with version " + importEntry.getVersion()
                + " not found in dependencies, found version: " + dependencyModule.getVersion());
          }
        }
      }

      dependencyModule.resolveModule(moduleManager);
      importEntry.setModule(dependencyModule);

      if (importEntry.getMavenVersion() == null) {
        importEntry.setMavenVersion(VersionManager.convertToMavenImport(importEntry.getVersion()));
      }

      if (importEntry.getNpmVersion() == null) {
        importEntry.setNpmVersion(VersionManager.convertToNpmImport(
            dependencyModule.getCode().getApi().get("js").get("npmGit"), importEntry.getVersion()));
      }
    }
  }

  private void autoImportModules(ModuleManager moduleManager) {

    if (!CORE_MODULE.equals(this.name)) {
      this.imports
          .add(new Import(CORE_MODULE, kurentoVersion, kurentoMavenVersion, kurentoNpmVersion));

      if (!ELEMENTS_MODULE.equals(this.name)) {
        this.imports.add(
            new Import(ELEMENTS_MODULE, kurentoVersion, kurentoMavenVersion, kurentoNpmVersion));

        if (!FILTERS_MODULE.equals(this.name)) {
          this.imports.add(
              new Import(FILTERS_MODULE, kurentoVersion, kurentoMavenVersion, kurentoNpmVersion));
        }
      }
    }
  }

  private Map<String, ? extends Type> getAllTypes() {
    return allTypes;
  }

  private void put(Map<String, ? super Type> types, Type type) {
    types.put(type.getName(), type);
  }

  private void resolveTypeRefs(List<? extends ModelElement> moduleElements,
      Map<String, Type> types) {

    for (ModelElement moduleElement : moduleElements) {

      if (moduleElement instanceof TypeRef) {
        resolveTypeRef((TypeRef) moduleElement, types);
      } else {
        // A moduleElement can be null if in original Json there is a
        // comma in the last element of an array
        if (moduleElement != null) {
          resolveTypeRefs(moduleElement.getChildren(), types);
        }
      }
    }
  }

  private void resolveTypeRef(TypeRef typeRef, Map<String, Type> types) {

    Type type = types.get(typeRef.getQualifiedName());

    if (type == null) {

      if (typeRef.getModuleName() == null) {

        // Maybe this type is declared in this module
        typeRef.setModuleName(this.name);
        type = types.get(typeRef.getQualifiedName());

        if (type == null) {
          throw new KurentoModuleCreatorException("The type '" + typeRef.getName()
              + "' is not defined in module " + this.name + " neither in kurento. Used in module: "
              + name + ".\nThe available types are: " + types.keySet());
        }

      } else {

        throw new KurentoModuleCreatorException("The type '" + typeRef.getName()
            + "' is not defined in module " + typeRef.getModuleName() + ". Used in module: " + name
            + ".\nThe available types are: " + types.keySet());

      }
    }

    typeRef.setType(type);
  }

  @SuppressWarnings("unchecked")
  private <T extends NamedElement> Map<String, T> resolveNamedElements(List<? extends T> elements) {

    Map<String, T> elementsMap = new HashMap<String, T>();
    for (NamedElement element : elements) {

      if (AUTO_IMPORTED_MODULES.contains(this.name)) {
        // Only types of imported modules can be referenced without
        // module name
        elementsMap.put(element.getName(), (T) element);
      } else {
        elementsMap.put(this.name + "." + element.getName(), (T) element);
      }
    }
    return elementsMap;
  }

  public void expandMethodsWithOpsParams() {
    for (RemoteClass remoteClass : remoteClassesMap.values()) {
      remoteClass.expandMethodsWithOpsParams();
    }
  }

  public void fusionModules(ModuleDefinition module) {

    // TODO Generalize this

    if (this.name == null) {
      this.name = module.name;
    } else {
      if (module.name != null) {
        throw new KurentoModuleCreatorException("Name can only be set in one kmd file");
      }
    }

    if (this.kurentoVersion == null) {
      this.kurentoVersion = module.kurentoVersion;
    } else {
      if (module.kurentoVersion != null) {
        throw new KurentoModuleCreatorException("Kurento version can only be set in one kmd file");
      }
    }

    if (this.version == null) {
      this.version = module.version;
    } else {
      if (module.version != null) {
        throw new KurentoModuleCreatorException("Version can only be set in one kmd file");
      }
    }

    if (this.imports.isEmpty()) {
      this.imports = module.imports;
    } else {
      if (!module.imports.isEmpty()) {
        throw new KurentoModuleCreatorException("Imports section can only be set in one kmd file");
      }
    }

    if (this.code == null) {
      this.code = module.code;
    } else {
      if (module.code != null) {
        throw new KurentoModuleCreatorException("Code section can only be set in one kmd file");
      }
    }

    this.complexTypes.addAll(module.complexTypes);
    this.remoteClasses.addAll(module.remoteClasses);
    this.events.addAll(module.events);
  }

  public boolean hasKmdSection() {
    if (code == null) {
      return false;
    }

    return code.getKmd() != null;
  }

}
