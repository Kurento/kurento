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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.Result;

public class CodeGenerationTest {

  static String[] TEMPLATES = { "cpp_cmake_dependencies", "cpp_find_cmake", "cpp_interface",
      "cpp_interface_internal", "cpp_module", "cpp_pkgconfig", "cpp_server", "cpp_server_opencv",
      "cpp_server_internal", "doc", "maven", "npm" };

  @Test
  public void test() throws IOException, URISyntaxException {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/core.kmd.json"));

    for (String template : TEMPLATES) {
      System.out.println("Template:" + template);
      modCreator.setInternalTemplates(template);
      modCreator.setCodeGenDir(Paths.get("/tmp/test_java"));
      Result result = modCreator.generateCode();
      if (!result.isSuccess()) {
        result.showErrorsInConsole();
        throw new KurentoModuleCreatorException(result.toString());
      }
    }
  }
}
