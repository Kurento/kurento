package org.kurento.test.docker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.kurento.commons.PropertiesManager;
import org.kurento.test.ConfigFileTest;

public class DockerTest extends ConfigFileTest {

	@Test
	public void executionContainerized() {

		boolean shouldBeInContainer = PropertiesManager
				.getProperty("test.docker.shouldBeInContainer", true);

		Docker docker = Docker.getSingleton();

		boolean isRunningInContainer = docker.isRunningInContainer();

		assertEquals(
				"shouldBeInContainer=" + shouldBeInContainer
						+ " and isRunningInContainer=" + isRunningInContainer
						+ " should be equals",
				shouldBeInContainer, isRunningInContainer);
	}

}
