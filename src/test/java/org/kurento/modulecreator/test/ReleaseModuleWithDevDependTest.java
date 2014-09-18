package org.kurento.modulecreator.test;

import static org.junit.Assert.assertThat;
import junit.framework.Assert;

import org.junit.Test;
import org.junit.internal.matchers.StringContains;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.PathUtils;

public class ReleaseModuleWithDevDependTest {

	@Test
	public void test() throws Exception {

		KurentoModuleCreator modCreator = new KurentoModuleCreator();

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/releaseversion/moduleA.kmd.json"));

		modCreator.addKmdFileToGen(PathUtils
				.getPathInClasspath("/releaseversion/moduleB.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakecore.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakeelements.kmd.json"));

		modCreator.addDependencyKmdFile(PathUtils
				.getPathInClasspath("/fakefilters.kmd.json"));

		try {

			modCreator.loadModulesFromKmdFiles();

			Assert.fail("Exception KurentoModuleCreatorException for dev dependencies in release should be thrown");

		} catch (KurentoModuleCreatorException e) {

			assertThat(
					e.getMessage(),
					StringContains
							.containsString("All dependencies of a release version must be also release versions"));
		}
	}

}
