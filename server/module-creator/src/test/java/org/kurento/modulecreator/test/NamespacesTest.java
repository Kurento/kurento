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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.ModuleManager;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.definition.ModuleDefinition;
import org.kurento.modulecreator.definition.Param;
import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.TypeRef;

public class NamespacesTest {

  @Test
  public void test() throws IOException, URISyntaxException {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/namespaces/moduleC.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/namespaces/moduleB.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/namespaces/moduleA.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));

    modCreator.loadModulesFromKmdFiles();

    ModuleManager moduleManager = modCreator.getModuleManager();

    ModuleDefinition moduleA = moduleManager.getModule("moduleA");
    assertThat(moduleA, is(notNullValue()));

    ModuleDefinition moduleB = moduleManager.getModule("moduleB");
    assertThat(moduleB, is(notNullValue()));

    ModuleDefinition moduleC = moduleManager.getModule("moduleC");
    assertThat(moduleC, is(notNullValue()));

    ModuleDefinition coreModule = moduleManager.getModule("core");
    assertThat(coreModule, is(notNullValue()));

    RemoteClass classA = moduleA.getRemoteClass("moduleA.ClassA");
    assertThat(classA, is(notNullValue()));

    RemoteClass classB = moduleB.getRemoteClass("moduleB.ClassB");
    assertThat(classB, is(notNullValue()));

    RemoteClass classC = moduleC.getRemoteClass("moduleC.ClassC");
    assertThat(classC, is(notNullValue()));

    RemoteClass coreClass = coreModule.getRemoteClass("CoreClass");
    assertThat(coreClass, is(notNullValue()));

    List<Param> paramsB = classB.getMethods().get(0).getParams();

    TypeRef classAType = paramsB.get(0).getType();
    assertThat(classAType.getName(), equalTo("ClassA"));
    assertThat(classAType.getModuleName(), equalTo("moduleA"));
    assertThat(classAType.getModule(), equalTo(moduleA));

    TypeRef coreClassType = paramsB.get(1).getType();
    assertThat(coreClassType.getName(), equalTo("CoreClass"));
    assertThat(coreClassType.getModule(), equalTo(coreModule));

    TypeRef classBType = paramsB.get(2).getType();
    assertThat(classBType.getName(), equalTo("ClassB"));
    assertThat(classBType.getModule(), equalTo(moduleB));

    List<Param> paramsC = classC.getMethods().get(0).getParams();

    TypeRef classAbModuleaType = paramsC.get(0).getType();
    assertThat(classAbModuleaType.getName(), equalTo("ClassAB"));
    assertThat(classAbModuleaType.getModuleName(), equalTo("moduleA"));
    assertThat(classAbModuleaType.getModule(), equalTo(moduleA));

    TypeRef classAbModulebType = paramsC.get(1).getType();
    assertThat(classAbModulebType.getName(), equalTo("ClassAB"));
    assertThat(classAbModulebType.getModuleName(), equalTo("moduleB"));
    assertThat(classAbModulebType.getModule(), equalTo(moduleB));

  }

}
