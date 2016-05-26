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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.ModuleManager;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.definition.Import;
import org.kurento.modulecreator.definition.ModuleDefinition;

public class ImportModulesWithRangesTest {

  @Test
  public void testSatisDep() throws IOException, URISyntaxException {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/versionranges/moduleC.kmd.json"));

    modCreator
        .addDependencyKmdFile(PathUtils.getPathInClasspath("/versionranges/moduleB.kmd.json"));

    modCreator
        .addDependencyKmdFile(PathUtils.getPathInClasspath("/versionranges/moduleA.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));

    modCreator.loadModulesFromKmdFiles();

    ModuleManager moduleManager = modCreator.getModuleManager();

    ModuleDefinition moduleB = moduleManager.getModule("moduleB");

    Import modAfromB = moduleB.getImports().get(0);
    assertThat(modAfromB.getVersion(), is("~1.0"));
    assertThat(modAfromB.getMavenVersion(), is("[1.0.0,2.0.0-SNAPSHOT)"));
    assertThat(modAfromB.getNpmVersion(), is(">=1.0.0 <2.0.0"));
    assertThat(modAfromB.getModule().getVersion(), is("1.5.0"));

    ModuleDefinition moduleC = moduleManager.getModule("moduleC");
    Import modAfromC = moduleC.getImports().get(0);
    assertThat(modAfromC.getVersion(), is(">=1.0.0 & <2.0.0"));
    assertThat(modAfromC.getMavenVersion(), is("[1.0.0,2.0.0-SNAPSHOT)"));
    assertThat(modAfromC.getNpmVersion(), is(">=1.0.0 <2.0.0"));
    assertThat(modAfromC.getModule().getVersion(), is("1.5.0"));

  }

  @Test
  public void testInsatisDep() throws IOException, URISyntaxException {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/versionranges/moduleD.kmd.json"));

    modCreator
        .addDependencyKmdFile(PathUtils.getPathInClasspath("/versionranges/moduleA.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));

    try {

      modCreator.loadModulesFromKmdFiles();

    } catch (KurentoModuleCreatorException e) {
      assertTrue(e.getMessage().contains("Import 'moduleA'")
          && e.getMessage().contains("not found in dependencies"));
      return;
    }

    fail("A KurentoModuleException should be thrown for insatisfied dependencies");

  }

}
