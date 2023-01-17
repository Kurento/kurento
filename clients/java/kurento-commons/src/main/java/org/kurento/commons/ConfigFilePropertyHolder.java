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

package org.kurento.commons;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kurento.commons.PropertiesManager.PropertyHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

public class ConfigFilePropertyHolder implements PropertyHolder {

  private static Logger log = LoggerFactory.getLogger(ConfigFilePropertyHolder.class);

  private static final String SINGLE_CONFIG_FILES_PROPERTY = "single.config.file";

  private static Path lastLoadedConfigFilePath;

  private List<ConfigFileObject> loadedConfigFiles;

  private Set<Path> loadedPaths;

  private static final Gson gson = new GsonBuilder().create();

  public static synchronized void configurePropertiesFromConfigFile(Path configFilePath)
      throws JsonSyntaxException, JsonIOException, IOException {

    boolean singleConfigFile = Boolean.getBoolean(SINGLE_CONFIG_FILES_PROPERTY);
    if (singleConfigFile && lastLoadedConfigFilePath != null) {
      log.warn("Trying to load a second config file. The first was {} and the "
          + "current is {}. Ignoring it.", lastLoadedConfigFilePath, configFilePath);
      return;
    }

    ConfigFilePropertyHolder cfph = null;
    PropertyHolder ph = PropertiesManager.getPropertyHolder();
    if (ph != null && ph instanceof ConfigFilePropertyHolder) {
      cfph = (ConfigFilePropertyHolder) ph;
      if (cfph.loadedPaths != null) {
        if (cfph.loadedPaths.contains(configFilePath)) {
          log.debug("Trying to load again config file {}. Ignoring it.",
              configFilePath.toAbsolutePath());
          return;
        }
      }
    }

    lastLoadedConfigFilePath = configFilePath;

    // FIXME shouldn't this be the first sentence in the method?
    Preconditions.checkNotNull(configFilePath, "configFilePath paramter must be not null.");

    log.debug("Using configuration file in path '{}' ({})", configFilePath,
        configFilePath.getClass().getCanonicalName());

    JsonReader reader =
        new JsonReader(Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8));

    reader.setLenient(true);

    JsonObject configFile = gson.fromJson(reader, JsonObject.class);

    traceConfigContent(configFile);

    if (cfph == null) {
      cfph = new ConfigFilePropertyHolder();
    }
    cfph.loadedConfigFiles.add(new ConfigFileObject(configFilePath, configFile));
    cfph.loadedPaths.add(configFilePath);

    PropertiesManager.setPropertyHolder(cfph);
  }

  private static void traceConfigContent(JsonObject configFile) {
    if (log.isDebugEnabled()) {
      Gson gs = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
      String jsonContents = gs.toJson(configFile);
      log.debug("Configuration content: " + jsonContents);
    }
  }

  public ConfigFilePropertyHolder() {
    this.loadedConfigFiles = new ArrayList<ConfigFileObject>();
    this.loadedPaths = new HashSet<Path>();
  }

  @Override
  public String getProperty(String property) {
    String systemProperty = System.getProperty(property);

    if (systemProperty != null) {
      return systemProperty;
    }

    String[] tokens = property.split("\\.");

    int lastTokenNumber = tokens.length - 1;

    // TODO customize search order (by default, it starts from the first loaded file)
    // e.g. system property 'reverseSearch.multi.config.file': if true, start searching in reverse
    if (loadedConfigFiles != null) {

      for (ConfigFileObject configFileObj : loadedConfigFiles) {
        Path currentPath = configFileObj.getConfigFilePath();
        JsonObject currentObject = configFileObj.getConfigFile();

        for (int i = 0; i < tokens.length; i++) {
          JsonElement element = currentObject.get(tokens[i]);
          if (element == null) {
            break; // goto next cfg file
          }

          if (i == lastTokenNumber) {
            log.debug("Found {} in config file {}", property, currentPath.toAbsolutePath());
            if (element instanceof JsonPrimitive) {
              return element.getAsString();
            } else {
              return element.toString();
            }
          }

          try {
            currentObject = (JsonObject) element;
          } catch (ClassCastException e) {
            break; // goto next cfg file
          }
        }
      }

    }

    return null;
  }

  public List<ConfigFileObject> getLoadedConfigFiles() {
    return loadedConfigFiles;
  }
}
