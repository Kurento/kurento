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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFileManager {

  private static final Logger log = LoggerFactory.getLogger(ConfigFileManager.class);

  private static final String CONFIG_FILE_PATH_PROPERTY = "configFilePath";
  private static final String CONFIG_FILE_PATH_PROPERTY2 = "config.file";
  private static final String CONFIG_FILE_PATH_PROPERTY3 = "test.config.file";

  public static void loadConfigFile() {
    loadConfigFile("kurento.conf.json");
  }

  public static void loadConfigFile(String configFileName) {

    try {

      String property = CONFIG_FILE_PATH_PROPERTY;
      String configFilePath = System.getProperty(CONFIG_FILE_PATH_PROPERTY);

      if (configFilePath == null) {
        configFilePath = System.getProperty(CONFIG_FILE_PATH_PROPERTY2);
        property = CONFIG_FILE_PATH_PROPERTY2;
      }

      if (configFilePath == null) {
        configFilePath = System.getProperty(CONFIG_FILE_PATH_PROPERTY3);
        property = CONFIG_FILE_PATH_PROPERTY3;
      }

      Path configFile = null;

      if (configFilePath != null) {
        configFile = Paths.get(configFilePath);

        if (!Files.exists(configFile)) {
          log.warn(
              "Property '{}' points to an invalid location '{}'. Searching default "
                  + "config file '{}' in classpath and workdir",
              property, configFilePath, configFileName);
          configFile = ConfigFileFinder.searchConfigFileInDefaultPlaces(configFileName);
        } else {
          log.debug("Property {} points to a valid location. Will use the config from {}", property,
              configFilePath);
        }

      } else {
        configFile = ConfigFileFinder.searchConfigFileInDefaultPlaces(configFileName);
      }

      if (configFile != null && Files.exists(configFile)) {
        ConfigFilePropertyHolder.configurePropertiesFromConfigFile(configFile);
      } else {
        log.warn("Config file {} not found. Using all default values", configFileName);
      }

    } catch (Exception e) {
      log.warn("Exception loading config file", e);
    }
  }

}
