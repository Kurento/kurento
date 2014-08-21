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

		modCreator.addKmdFileToGen(PathUtils
				.getPathInClasspath("/namespaces/moduleC.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/namespaces/moduleB.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/namespaces/moduleA.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakecore.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakeelements.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakefilters.kmd.json"));

		modCreator.loadModulesFromKmdFiles();

		ModuleManager moduleManager = modCreator.getModuleManager();

		ModuleDefinition moduleA = moduleManager.getModule("moduleA");
		ModuleDefinition moduleB = moduleManager.getModule("moduleB");
		ModuleDefinition moduleC = moduleManager.getModule("moduleC");
		ModuleDefinition coreModule = moduleManager.getModule("core");

		assertThat(moduleA, is(notNullValue()));
		assertThat(moduleB, is(notNullValue()));
		assertThat(moduleC, is(notNullValue()));
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

		TypeRef classABModuleAType = paramsC.get(0).getType();
		assertThat(classABModuleAType.getName(), equalTo("ClassAB"));
		assertThat(classABModuleAType.getModuleName(), equalTo("moduleA"));
		assertThat(classABModuleAType.getModule(), equalTo(moduleA));

		TypeRef classABModuleBType = paramsC.get(1).getType();
		assertThat(classABModuleBType.getName(), equalTo("ClassAB"));
		assertThat(classABModuleBType.getModuleName(), equalTo("moduleB"));
		assertThat(classABModuleBType.getModule(), equalTo(moduleB));

	}

}
