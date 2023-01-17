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

package org.kurento.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.kurento.commons.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

public class ConfigurationReader {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationReader.class);
  private static final Gson gson = new GsonBuilder().create();

  private JsonObject configFile;

  public ConfigurationReader(String configFileName) throws IOException {

    String configFile = ClassPath.get(configFileName).toString();

    Path configFilePath = Paths.get(configFile);

    if (configFilePath == null) {
      log.debug("File not found {}", configFileName);
    }

    try (JsonReader reader = new JsonReader(
        Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8))) {
      reader.setLenient(true);
      this.configFile = gson.fromJson(reader, JsonObject.class);

    } catch (NoSuchFileException e) {
      log.warn("Configuration file {} not found", configFilePath);
    } catch (IOException e) {
      log.error("Error opening config file {}", configFilePath, e);
    } catch (JsonParseException e) {
      log.error("Error parsing configuration file {}", configFilePath, e);
    }
  }

  public JsonObject getConfig() {
    return this.configFile;
  }
}
