package org.kurento.test.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.kurento.commons.PropertiesManager;
import org.kurento.test.ConfigFileTest;

public class DockerTest extends ConfigFileTest {

	@Test
	public void executionContainerized() {

		boolean shouldBeInContainer = PropertiesManager
				.getProperty("test.docker.shouldBeInContainer", false);

		Docker docker = Docker.getSingleton();

		boolean isRunningInContainer = docker.isRunningInContainer();

		assertEquals(
				"shouldBeInContainer=" + shouldBeInContainer
						+ " and isRunningInContainer=" + isRunningInContainer
						+ " should be equals",
				shouldBeInContainer, isRunningInContainer);
	}

	@Test
	public void dockerContainerName() {

		String expectedContainerName = PropertiesManager
				.getProperty("test.docker.expectedContainerName");

		if (expectedContainerName != null) {

			Docker docker = Docker.getSingleton();

			assertTrue("Tests should be running in a docker container",
					docker.isRunningInContainer());

			assertEquals("Container name is not retrieved correctly",
					expectedContainerName, docker.getContainerName());

		}
	}

}
