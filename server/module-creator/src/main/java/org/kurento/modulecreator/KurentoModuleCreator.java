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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kurento.modulecreator.codegen.CodeGen;
import org.kurento.modulecreator.codegen.Error;
import org.kurento.modulecreator.definition.ComplexType;
import org.kurento.modulecreator.definition.Event;
import org.kurento.modulecreator.definition.Import;
import org.kurento.modulecreator.definition.ModuleDefinition;
import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.json.JsonModuleSaverLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class KurentoModuleCreator {

  private static final Logger log = LoggerFactory.getLogger(KurentoModuleCreator.class);

  private static final String CONFIG_FILE_NAME = "config.json";

  private Path codegenDir;
  private JsonObject config = new JsonObject();
  private Path templatesDir;
  private boolean verbose;
  private boolean deleteGenDir;
  private boolean overwrite;
  private List<Path> dependencyKmdFiles = new ArrayList<Path>();
  private List<Path> dependencyKmdFilesToGen = new ArrayList<Path>();
  private List<Path> kmdFilesToGen = new ArrayList<Path>();

  private boolean listGeneratedFiles = false;
  private String internalTemplates = null;
  private Path outputModuleFile = null;
  private boolean hasToGenerateCode = true;

  private ModuleManager moduleManager;
  private ModuleManager depModuleManager;

  private boolean generateMavenPom = false;
  private boolean generateNpmPackage = false;

  public ModuleManager getModuleManager() {
    return moduleManager;
  }

  public ModuleManager getDepModuleManager() {
    return depModuleManager;
  }

  public void setInternalTemplates(String internalTemplates) {
    this.internalTemplates = internalTemplates;
  }

  public String getInternalTemplates() {
    return internalTemplates;
  }

  public void setKmdFilesToGen(List<Path> kmdFiles) {
    this.kmdFilesToGen = kmdFiles;
  }

  public void addKmdFileToGen(Path kmdFile) {
    this.kmdFilesToGen.add(kmdFile);
  }

  public void setDependencyKmdFilesToGen(List<Path> dependencyFilesToGen) {
    this.dependencyKmdFilesToGen = dependencyFilesToGen;
  }

  public void addDependencyKmdFileToGen(Path kmdFile) {
    this.dependencyKmdFilesToGen.add(kmdFile);
  }

  public void setConfig(JsonObject config) {
    this.config = config;
  }

  public void setCodeGenDir(Path codegenDir) {
    this.codegenDir = codegenDir;
  }

  public void setTemplatesDir(Path templatesDir) {
    this.templatesDir = templatesDir;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setDeleteGenDir(boolean deleteGenDir) {
    this.deleteGenDir = deleteGenDir;
  }

  public void setDependencyKmdFiles(List<Path> dependencyKmdFiles) {
    this.dependencyKmdFiles = dependencyKmdFiles;
  }

  public void addDependencyKmdFile(Path dependencyKmdFile) {
    this.dependencyKmdFiles.add(dependencyKmdFile);
  }

  public void setListGeneratedFiles(boolean listGeneratedFiles) {
    this.listGeneratedFiles = listGeneratedFiles;
  }

  public void setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
  }

  public boolean hasToGenerateCode() {
    return hasToGenerateCode;
  }

  private Path getInternalTemplatesDir(String internalTemplates) throws IOException {

    URL internalTemplatesAsUrl = this.getClass().getResource("/" + internalTemplates);

    if (internalTemplatesAsUrl != null) {

      try {
        return PathUtils.getPathInClasspath(internalTemplatesAsUrl);

      } catch (URISyntaxException e) {
        throw new KurentoModuleCreatorException(
            "Error trying to load internal templates folder '" + internalTemplates + "'", e);
      }

    } else {
      throw new KurentoModuleCreatorException(
          "The internal templates folder '" + internalTemplates + "' doesn't exist");
    }
  }

  /**
   * Genarates the code.
   * 
   * @return The result of the code generation
   * @throws JsonIOException
   *           if there is an error parsing kmd's
   * @throws IOException
   *           if there is an error reading kmd's
   */
  public Result generateCode() throws JsonIOException, IOException {

    if (moduleManager == null) {
      loadModulesFromKmdFiles();
    }

    if (internalTemplates != null) {
      templatesDir = getInternalTemplatesDir(internalTemplates);
    }

    if (templatesDir != null) {
      Path configFile = templatesDir.resolve(CONFIG_FILE_NAME);
      if (Files.exists(configFile)) {
        JsonObject internalConfig = loadConfigFile(configFile);
        overrideConfig(internalConfig, config);
        config = internalConfig;
      }
    }

    try {

      if (deleteGenDir) {
        PathUtils.delete(codegenDir, loadNoDeleteFiles(config));
      }

      if (codegenDir != null && !Files.exists(codegenDir)) {
        Files.createDirectories(codegenDir);
      }

      CodeGen codeGen = new CodeGen(templatesDir, codegenDir, verbose, listGeneratedFiles,
          overwrite, config);

      for (ModuleDefinition module : moduleManager.getModules()) {
        if (config.has("expandMethodsWithOpsParams")
            && config.get("expandMethodsWithOpsParams").getAsBoolean()) {
          module.expandMethodsWithOpsParams();
        }

        if (templatesDir != null && codegenDir != null) {
          codeGen.generateCode(module);
        }

        if (outputModuleFile != null) {
          JsonModuleSaverLoader.getInstance().writeToFile(module,
              new File(outputModuleFile.toFile(), module.getName() + ".kmd.json"));
        }

        if (generateMavenPom) {
          codeGen.setTemplatesDir(getInternalTemplatesDir("maven"));
          codeGen.generateMavenPom(module, searchFiles(this.kmdFilesToGen, "pom.xml"));
        }

        if (generateNpmPackage) {
          codeGen.setTemplatesDir(getInternalTemplatesDir("npm"));
          codeGen.generateNpmPackage(module, searchFiles(this.kmdFilesToGen, "package.json"),
              searchFiles(this.kmdFilesToGen, "bower.json"));
        }
      }

      return new Result();

    } catch (KurentoModuleCreatorException e) {
      return new Result(new Error("Error: " + e.getMessage()));
    } catch (Exception e) {
      e.printStackTrace();
      return new Result(
          new Error("Unexpected error: " + e.getClass().getName() + " " + e.getMessage()));
    }
  }

  private Path searchFiles(List<Path> kmdFiles, String fileName) throws IOException {

    List<Path> pomFiles = new ArrayList<Path>();
    for (Path kmdFile : kmdFiles) {
      pomFiles.addAll(PathUtils.searchFiles(kmdFile.getParent(), fileName));
    }

    if (pomFiles.isEmpty()) {
      return null;
    } else {
      if (pomFiles.size() > 1) {
        log.warn("There are several '" + fileName + "' files in kmd.json folders."
            + " Picking the first one");
      }
      return pomFiles.get(0);
    }
  }

  private List<String> loadNoDeleteFiles(JsonObject configContent) {
    List<String> noDeleteFiles = new ArrayList<String>();
    if (configContent != null) {
      JsonArray array = configContent.getAsJsonArray("no_delete");
      if (array != null) {
        for (JsonElement elem : array) {
          if (elem instanceof JsonPrimitive) {
            noDeleteFiles.add(((JsonPrimitive) elem).getAsString());
          }
        }
      }
    }
    return noDeleteFiles;
  }

  public static JsonObject loadConfigFile(Path configFile) throws JsonIOException, IOException {

    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();
    try {
      JsonElement element = gson.fromJson(
          Files.newBufferedReader(configFile, Charset.forName("UTF-8")), JsonElement.class);
      return element.getAsJsonObject();

    } catch (JsonSyntaxException e) {
      throw new KurentoModuleCreatorException("Config file '" + configFile
          + "' has the following formatting error:" + e.getLocalizedMessage());
    }
  }

  private static void overrideConfig(JsonObject configContents, JsonObject newConfigContents) {

    for (Entry<String, JsonElement> e : newConfigContents.entrySet()) {
      configContents.add(e.getKey(), e.getValue());
    }
  }

  public void loadModulesFromKmdFiles() throws FileNotFoundException, IOException {

    log.debug("Loading dependencies");
    depModuleManager = new ModuleManager();
    depModuleManager.addModules(loadModuleDescriptors(dependencyKmdFiles));
    depModuleManager.resolveModules();

    ModuleDefinition module = fusionModuleDescriptors(loadModuleDescriptors(kmdFilesToGen));
    if (module != null) {
      module.validateModule();
    }

    log.debug("Loading dependency kmd files to generate code");
    moduleManager = new ModuleManager();
    moduleManager.addModules(loadModuleDescriptors(dependencyKmdFilesToGen));
    if (module != null) {
      moduleManager.addModule(module);
    }
    moduleManager.setDependencies(depModuleManager);
    moduleManager.resolveModules();

    hasToGenerateCode = (module != null) && !module.hasKmdSection()
        || !dependencyKmdFilesToGen.isEmpty();

  }

  private ModuleDefinition fusionModuleDescriptors(List<ModuleDefinition> modules) {

    if (modules.isEmpty()) {
      return null;
    }

    ModuleDefinition module = modules.get(0);
    for (int i = 1; i < modules.size(); i++) {
      module.fusionModules(modules.get(i));
    }

    return module;
  }

  private List<ModuleDefinition> loadModuleDescriptors(List<Path> kmdFiles)
      throws FileNotFoundException, IOException {

    List<ModuleDefinition> modules = new ArrayList<>();

    for (Path kmdFile : kmdFiles) {

      log.debug("Loading kmdFile " + kmdFile);

      ModuleDefinition module = JsonModuleSaverLoader.getInstance().loadFromFile(kmdFile);

      modules.add(module);
    }

    return modules;
  }

  public void printValues(String[] keys) throws FileNotFoundException, IOException {
    if (moduleManager == null) {
      loadModulesFromKmdFiles();
    }

    for (ModuleDefinition module : moduleManager.getModules()) {
      for (String key : keys) {
        System.out.println("Value: " + key + " = " + getValue(module, key));
      }
    }
  }

  public void printSimpleKmd() throws FileNotFoundException, IOException, NoSuchAlgorithmException {
    if (moduleManager == null) {
      loadModulesFromKmdFiles();
    }

    MessageDigest digest = MessageDigest.getInstance("MD5");

    for (ModuleDefinition module : moduleManager.getModules()) {
      for (RemoteClass klass : module.getRemoteClasses()) {
        System.out.println("RemoteClass:\t" + klass.getName());
        digest.update(klass.getName().getBytes());
      }
      for (Event event : module.getEvents()) {
        System.out.println("Event:\t" + event.getName());
        digest.update(event.getName().getBytes());
      }
      for (ComplexType complexType : module.getComplexTypes()) {
        System.out.println("ComplexType:\t" + complexType.getName());
        digest.update(complexType.getName().getBytes());
      }
      for (Import dep : module.getImports()) {
        String depDesc = dep.getName() + " " + dep.getVersion();
        System.out.println("Dep: " + depDesc);
        digest.update(depDesc.getBytes());
      }
    }

    System.out.print("Digest: ");
    for (byte b : digest.digest()) {
      String hexStr = Integer.toHexString(b & 0xFF);
      if (hexStr.length() < 1) {
        hexStr = "00";
      } else if (hexStr.length() < 2) {
        hexStr = "0" + hexStr;
      }

      System.out.print(hexStr);
    }

    System.out.println("");
  }

  private static String getValue(Object object, String key) {
    int index = key.indexOf('.');
    String currentKey;
    Object value;

    if (index == -1) {
      currentKey = key;
    } else {
      currentKey = key.substring(0, index);
    }

    if (object instanceof Map) {
      value = ((Map<?, ?>) object).get(key);
      if (value != null) {
        return "" + value;
      }

      value = ((Map<?, ?>) object).get(currentKey);
    } else if (object instanceof List) {
      value = ((List<?>) object).get(Integer.valueOf(currentKey));
    } else {
      try {
        Method method = object.getClass().getMethod(
            "get" + Character.toUpperCase(currentKey.charAt(0)) + currentKey.substring(1));

        value = method.invoke(object);
      } catch (Exception e) {
        try {
          Method method = object.getClass().getMethod(currentKey);

          value = method.invoke(object);
        } catch (Exception ex) {
          e.printStackTrace();
          return null;
        }
      }
    }

    if (index == -1) {
      return "" + value;
    } else {
      String nextStep = key.substring(index + 1);

      return getValue(value, nextStep);
    }
  }

  public void setOutputFile(Path outputModuleFile) {
    this.outputModuleFile = outputModuleFile;
  }

  public void setGenerateMavenPom(boolean hasOption) {
    this.generateMavenPom = hasOption;
  }

  public void setGenerateNpmPackage(boolean hasOption) {
    this.generateNpmPackage = hasOption;
  }
}
