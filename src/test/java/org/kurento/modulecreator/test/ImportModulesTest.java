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

public class ImportModulesTest {

	@Test
	public void test() throws IOException, URISyntaxException {

		KurentoModuleCreator modCreator = new KurentoModuleCreator();

		modCreator.addKmdFileToGen(PathUtils
				.getPathInClasspath("/importmodules/moduleC.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/importmodules/moduleB.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/importmodules/moduleA.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakecore.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakeelements.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakefilters.kmd.json"));

		modCreator.loadModulesFromKmdFiles();

		ModuleManager moduleManager = modCreator.getModuleManager();

		ModuleDefinition moduleB = moduleManager.getModule("moduleB");
		ModuleDefinition moduleC = moduleManager.getModule("moduleC");

		String impModAFromModB = moduleB.getImports().get(0).getMavenVersion();
		assertThat(impModAFromModB, is("1.0.0-SNAPSHOT"));

		String impModAFromModC = moduleC.getImports().get(0).getMavenVersion();
		assertThat(impModAFromModC, is("0.0.1-SNAPSHOT"));

	}
}
