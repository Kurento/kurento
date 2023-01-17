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

package org.kurento.commons.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.Test;
import org.kurento.commons.ConfigFileFinder;
import org.kurento.commons.ConfigFilePropertyHolder;
import org.kurento.commons.PropertiesManager;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class ConfigFileTest {

  @Test
  public void testSimpleProperty()
      throws JsonSyntaxException, JsonIOException, IOException, URISyntaxException {

    Path configFilePath = ConfigFileFinder.searchConfigFileInDefaultPlaces("test.conf.json");

    ConfigFilePropertyHolder.configurePropertiesFromConfigFile(configFilePath);

    assertThat(PropertiesManager.getProperty("prop1"), is("value1"));
    assertThat(PropertiesManager.getProperty("prop2.prop1"), is("xxx"));
    assertThat(PropertiesManager.getProperty("nonExistingProp3"), is(nullValue()));
    assertThat(PropertiesManager.getProperty("nonExistingProp3.prop1"), is(nullValue()));

    System.setProperty("nonExistingProp4", "kkkk");

    assertThat(PropertiesManager.getProperty("nonExistingProp4"), is("kkkk"));

    System.setProperty("prop1", "kkkk");

    assertThat(PropertiesManager.getProperty("prop1"), is("kkkk"));

  }
}
