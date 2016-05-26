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

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.PathUtils;

import junit.framework.Assert;

public class ReleaseModuleWithDevDependTest {

  @Test
  public void test() throws Exception {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator
        .addDependencyKmdFile(PathUtils.getPathInClasspath("/releaseversion/moduleA.kmd.json"));

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/releaseversion/moduleB.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));

    try {

      modCreator.loadModulesFromKmdFiles();

      Assert.fail("Exception KurentoModuleCreatorException for dev"
          + " dependencies in release should be thrown");

    } catch (KurentoModuleCreatorException e) {

      Assert.assertTrue(
          "Exception message should be: "
              + "\"All dependencies of a release version must be also release versions\""
              + " but it is: \"" + e.getMessage() + "\"",
          e.getMessage()
              .contains("All dependencies of a release version must be also release versions"));

    }
  }
}
