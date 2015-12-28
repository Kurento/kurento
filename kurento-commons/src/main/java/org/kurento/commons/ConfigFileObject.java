/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
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
   * @return the {@link Path} of this configuration file
   */
  public Path getConfigFilePath() {
    return configFilePath;
  }

  public void setConfigFilePath(Path configFilePath) {
    this.configFilePath = configFilePath;
  }

  /**
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
