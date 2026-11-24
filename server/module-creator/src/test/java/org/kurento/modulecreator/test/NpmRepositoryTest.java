/*
 * (C) Copyright 2026 Kurento (http://kurento.org/)
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.Result;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Checks the "repository" field of the generated NPM package.json: it is taken from "npmRepository"
 * when set (this must match the repository that publishes the package), and from "npmGit"
 * otherwise. The value of "npmGit" is not altered, because it is also used for the development
 * dependencies between modules.
 */
public class NpmRepositoryTest {

  private JsonObject generatePackageJson(String kmdFile) throws IOException, URISyntaxException {
    Path outDir = Files.createTempDirectory("npm_repository_test");

    KurentoModuleCreator modCreator = new KurentoModuleCreator();
    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath(kmdFile));
    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));
    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));
    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));
    modCreator.setInternalTemplates("npm");
    modCreator.setCodeGenDir(outDir);

    Result result = modCreator.generateCode();
    if (!result.isSuccess()) {
      result.showErrorsInConsole();
      throw new KurentoModuleCreatorException(result.toString());
    }

    Path packageJson = outDir.resolve("package.json");
    assertTrue("package.json was not generated in " + outDir, Files.exists(packageJson));

    String content = new String(Files.readAllBytes(packageJson), StandardCharsets.UTF_8);
    return JsonParser.parseString(content).getAsJsonObject();
  }

  @Test
  public void repositoryFromNpmRepository() throws IOException, URISyntaxException {
    JsonObject doc = generatePackageJson("/npmrepository/withRepository.kmd.json");

    assertThat(doc.getAsJsonObject("repository").get("url").getAsString(),
        is("https://github.com/example/monorepo.git"));
  }

  @Test
  public void repositoryFallsBackToNpmGit() throws IOException, URISyntaxException {
    JsonObject doc = generatePackageJson("/npmrepository/withoutRepository.kmd.json");

    assertThat(doc.getAsJsonObject("repository").get("url").getAsString(),
        is("https://github.com/example/mirror-js.git"));
  }
}
