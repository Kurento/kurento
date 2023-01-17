/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
 */

package org.kurento.commons;

import java.nio.file.Path;

import com.google.gson.JsonObject;

/**
 * Bean that stores a loaded configuration file.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.2.2
 */
public class ConfigFileObject {

  private Path configFilePath;
  private JsonObject configFile;

  public ConfigFileObject(Path configFilePath, JsonObject configFile) {
    super();
    this.configFilePath = configFilePath;
    this.configFile = configFile;
  }

  /**
   * Returns the configuration file path.
   *
   * @return the {@link Path} of this configuration file
   */
  public Path getConfigFilePath() {
    return configFilePath;
  }

  public void setConfigFilePath(Path configFilePath) {
    this.configFilePath = configFilePath;
  }

  /**
   * Return the contents of a config file, as JSON object.
   *
   * @return the contents of the file as a {@link JsonObject}
   */
  public JsonObject getConfigFile() {
    return configFile;
  }

  public void setConfigFile(JsonObject configFile) {
    this.configFile = configFile;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((configFilePath == null) ? 0 : configFilePath.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ConfigFileObject)) {
      return false;
    }
    ConfigFileObject other = (ConfigFileObject) obj;
    if (configFilePath == null) {
      if (other.configFilePath != null) {
        return false;
      }
    } else if (!configFilePath.equals(other.configFilePath)) {
      return false;
    }
    return true;
  }

}
