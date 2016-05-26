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
package org.kurento.modulecreator.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.ModuleManager;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.definition.ModuleDefinition;

public class NpmVersionTest {

  @Test
  public void test() throws IOException, URISyntaxException {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/npmversion/moduleC.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/npmversion/moduleB.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/npmversion/moduleA.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));

    modCreator.loadModulesFromKmdFiles();

    ModuleManager moduleManager = modCreator.getModuleManager();

    ModuleDefinition moduleA = moduleManager.getModule("moduleA");
    ModuleDefinition moduleB = moduleManager.getModule("moduleB");
    ModuleDefinition moduleC = moduleManager.getModule("moduleC");

    String npmVersionA = moduleA.getCode().getApi().get("js").get("npmVersion");
    assertThat(npmVersionA, is("git://host/path"));

    String npmVersionB = moduleB.getCode().getApi().get("js").get("npmVersion");
    assertThat(npmVersionB, is("1.0.0"));

    String npmVersionC = moduleC.getCode().getApi().get("js").get("npmVersion");
    assertThat(npmVersionC, is("1.0.0-d"));

  }

}
