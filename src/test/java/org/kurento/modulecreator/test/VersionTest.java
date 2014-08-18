package org.kurento.modulecreator.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.kurento.modulecreator.VersionManager;

public class VersionTest {

	@Test
	public void testNumericalVersions() {

		assertTrue(VersionManager.versionCompare("1.2.3", "1.2.3") == 0);
		assertTrue(VersionManager.versionCompare("1.2.3", "1.2.3.4") < 0);
		assertTrue(VersionManager.versionCompare("1.2.3.4", "1.2.3") > 0);
		assertTrue(VersionManager.versionCompare("1.2.3", "1.2.4") < 0);
		assertTrue(VersionManager.versionCompare("1.0", "1.2.4") < 0);
	}

	@Test
	public void testCompatibleVersions() {

		assertTrue(VersionManager
				.devCompatibleVersion("1.2.3-dev", "1.2.3-dev"));

		assertTrue(VersionManager.devCompatibleVersion("1.2.3-dev", "1.2.4"));
		assertTrue(VersionManager.devCompatibleVersion("1.2.3-dev", "1.3.0"));
		assertTrue(VersionManager.devCompatibleVersion("1.2.3-dev", "1.9.9"));
		assertFalse(VersionManager.devCompatibleVersion("1.2.3-dev", "2.0.0"));
	}

}
