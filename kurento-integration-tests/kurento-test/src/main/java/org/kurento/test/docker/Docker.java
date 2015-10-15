/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.docker;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.kurento.commons.PropertiesManager;
import org.kurento.test.Shell;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerClientException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * Docker client for tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class Docker implements Closeable {

	private static Docker singleton = null;

	private static Boolean isRunningInContainer;

	private static final Logger log = LoggerFactory.getLogger(Docker.class);

	private static final String DOCKER_SERVER_URL_PROPERTY = "docker.server.url";
	private static final String DOCKER_SERVER_URL_DEFAULT = "http://localhost:2375";

	public static final String DOCKER_CONTAINER_NAME_PROPERTY = "docker.container.name";

	private static final int WAIT_CONTAINER_POLL_TIME = 200; // milliseconds
	private static final int WAIT_CONTAINER_POLL_TIMEOUT = 10; // seconds

	private DockerClient client;

	private String containerName;

	private static String hostIp;

	public synchronized static Docker getSingleton(String dockerServerUrl) {
		if (singleton == null) {
			singleton = new Docker(dockerServerUrl);
		}
		return singleton;
	}

	public static Docker getSingleton() {

		return getSingleton(PropertiesManager.getProperty(
				DOCKER_SERVER_URL_PROPERTY, getDefaultDockerServerUrl()));
	}

	private static String getDefaultDockerServerUrl() {

		if (isRunningInContainerInternal()) {
			return "http://" + getHostIp() + ":2375";
		} else {
			return DOCKER_SERVER_URL_DEFAULT;
		}
	}

	public boolean isRunningInContainer() {
		return isRunningInContainerInternal();
	}

	private static boolean isRunningInContainerInternal() {

		if (isRunningInContainer == null) {

			try (BufferedReader br = Files.newBufferedReader(
					Paths.get("/proc/1/cgroup"), StandardCharsets.UTF_8)) {

				String line = null;
				while ((line = br.readLine()) != null) {
					if (!line.endsWith("/")) {
						return true;
					}
				}
				isRunningInContainer = false;

			} catch (IOException e) {
				isRunningInContainer = false;
			}
		}

		return isRunningInContainer;
	}

	private static String getHostIp() {

		if (hostIp == null) {

			if (isRunningInContainerInternal()) {

				try {

					String ipRoute = Shell.runAndWait("sh", "-c",
							"/sbin/ip route");

					String[] tokens = ipRoute.split("\\s");

					hostIp = tokens[2];

				} catch (Exception e) {
					throw new DockerClientException(
							"Exception executing /sbin/ip route", e);
				}

			} else {
				hostIp = "127.0.0.1";
			}
		}

		return hostIp;
	}

	public Docker(String dockerServerUrl) {
		client = DockerClientBuilder.getInstance(dockerServerUrl).build();
	}

	public boolean isRunningContainer(String containerName) {
		boolean isRunning = false;
		if (existsContainer(containerName)) {
			isRunning = inspectContainer(containerName).getState().isRunning();
			log.trace("Container {} is running: {}", containerName, isRunning);
		}

		return isRunning;
	}

	public boolean existsContainer(String containerName) {
		boolean exists = true;
		try {
			getClient().inspectContainerCmd(containerName).exec();
			log.trace("Container {} already exist", containerName);

		} catch (NotFoundException e) {
			log.trace("Container {} does not exist", containerName);
			exists = false;
		}
		return exists;
	}

	public boolean existsImage(String imageName) {
		boolean exists = true;
		try {
			getClient().inspectImageCmd(imageName).exec();
			log.trace("Image {} exists", imageName);

		} catch (NotFoundException e) {
			log.trace("Image {} does not exist", imageName);
			exists = false;
		}
		return exists;
	}

	public void createContainer(String imageId, String containerName,
			boolean mountTestFiles, String... env) {

		if (!existsContainer(containerName)) {
			if (!existsImage(imageId)) {
				log.info(
						"Pulling Docker image {} ... please be patient until the process finishes",
						imageId);
				getClient().pullImageCmd(imageId)
						.exec(new PullImageResultCallback()).awaitSuccess();
				log.info("Image {} downloaded", imageId);

			} else {
				log.debug("Image {} already exists", imageId);
			}

			log.debug("Creating container {}", containerName);

			CreateContainerCmd createContainerCmd = getClient()
					.createContainerCmd(imageId).withName(containerName)
					.withEnv(env);

			if (mountTestFiles && isRunningInContainer()) {
				createContainerCmd
						.withVolumesFrom(new VolumesFrom(getContainerId()));
			} else {
				String testFilesPath = KurentoServicesTestHelper
						.getTestFilesPath();
				Volume volume = new Volume(testFilesPath);
				createContainerCmd.withVolumes(volume).withBinds(
						new Bind(testFilesPath, volume, AccessMode.ro));
			}

			createContainerCmd.exec();

			log.debug("Container {} started...", containerName);

		} else {
			log.debug("Container {} already exists", containerName);
		}
	}

	public InspectContainerResponse inspectContainer(String containerName) {
		return getClient().inspectContainerCmd(containerName).exec();
	}

	public void startContainer(String containerName) {
		if (!isRunningContainer(containerName)) {
			log.debug("Starting container {}", containerName);

			getClient().startContainerCmd(containerName).exec();

			log.debug("Started container {}", containerName);
		} else {
			log.debug("Container {} is already started", containerName);
		}
	}

	public void close() {
		if (client != null) {
			try {
				getClient().close();
			} catch (IOException e) {
				log.error("Exception closing Docker client", e);
			}
		}
	}

	public DockerClient getClient() {
		return client;
	}

	public void stopContainers(String... containerNames) {
		for (String containerName : containerNames) {
			stopContainer(containerName);
		}
	}

	public void stopContainer(String containerName) {
		if (isRunningContainer(containerName)) {
			log.debug("Stopping container {}", containerName);

			getClient().stopContainerCmd(containerName).exec();

		} else {
			log.debug("Container {} is not running", containerName);
		}
	}

	public void removeContainers(String... containerNames) {
		for (String containerName : containerNames) {
			removeContainer(containerName);
		}
	}

	public void removeContainer(String containerName) {
		if (existsContainer(containerName)) {
			log.debug("Removing container {}", containerName);

			getClient().removeContainerCmd(containerName).exec();
		}
	}

	public void stopAndRemoveContainer(String containerName) {
		stopContainer(containerName);
		removeContainer(containerName);
	}

	public void stopAndRemoveContainers(String... containerNames) {
		for (String containerName : containerNames) {
			stopAndRemoveContainer(containerName);
		}
	}

	public synchronized String startHub(String hubName, String imageId,
			long timeoutMs) {
		// Create hub if not exist
		createContainer(imageId, hubName, false, "GRID_TIMEOUT=" + timeoutMs);

		// Start hub if stopped
		startContainer(hubName);

		// Read IP address
		String hubIp = inspectContainer(hubName).getNetworkSettings()
				.getIpAddress();
		log.debug("Hub started on IP address: {}", hubIp);
		return hubIp;
	}

	public void startNode(String nodeName, String imageId, String hubIp) {
		// Create node if not exist
		createContainer(imageId, nodeName, true,
				"HUB_PORT_4444_TCP_ADDR=" + hubIp);

		// Start node if stopped
		startContainer(nodeName);
	}

	public void startAndWaitNode(String nodeName, String imageId,
			String hubIp) {
		startNode(nodeName, imageId, hubIp);
		waitForContainer(nodeName);
	}

	public String startAndWaitHub(String hubName, String imageId,
			long timeoutMs) {
		String hubIp = startHub(hubName, imageId, timeoutMs);
		waitForContainer(hubName);
		return hubIp;
	}

	public void waitForContainer(String containerName) {
		boolean isRunning = false;

		long timeoutMs = System.currentTimeMillis()
				+ TimeUnit.SECONDS.toMillis(WAIT_CONTAINER_POLL_TIMEOUT);
		do {
			isRunning = isRunningContainer(containerName);
			if (!isRunning) {

				// Check timeout
				if (System.currentTimeMillis() > timeoutMs) {
					throw new RuntimeException(
							"Timeout of " + WAIT_CONTAINER_POLL_TIMEOUT
									+ " seconds waiting for container "
									+ containerName);
				}

				try {
					// Wait WAIT_HUB_POLL_TIME ms
					log.debug(
							"Container {} is not still running ... waiting {} ms",
							containerName, WAIT_CONTAINER_POLL_TIME);
					Thread.sleep(WAIT_CONTAINER_POLL_TIME);

				} catch (InterruptedException e) {
					log.error("Exception waiting for hub");
				}

			}
		} while (!isRunning);
	}

	public String getContainerId() {
		try {

			BufferedReader br = Files.newBufferedReader(
					Paths.get("/proc/self/cgroup"), StandardCharsets.UTF_8);

			String line = null;
			while ((line = br.readLine()) != null) {
				log.info(line);
				if (line.contains("docker")) {
					return line.substring(line.lastIndexOf('/') + 1,
							line.length());
				}
			}

			throw new DockerClientException("Exception obtaining containerId. "
					+ "The file /proc/self/cgroup doesn't contain a line with 'docker'");

		} catch (IOException e) {
			throw new DockerClientException("Exception obtaining containerId. "
					+ "Exception reading file /proc/self/cgroup", e);
		}
	}

	public String getContainerName() {

		if (!isRunningInContainer()) {
			throw new DockerClientException(
					"Can't obtain container name if not running in container");
		}

		if (containerName == null) {

			containerName = System.getProperty(DOCKER_CONTAINER_NAME_PROPERTY);

			if (containerName == null) {

				String containerId = getContainerId();
				containerName = inspectContainer(containerId).getName();
				containerName = containerName.substring(1);
			}
		}

		return containerName;

	}

	public String getContainerIpAddress() {
		if (isRunningInContainer()) {
			return inspectContainer(getContainerName()).getNetworkSettings()
					.getIpAddress();
		} else {
			throw new DockerClientException(
					"Can't obtain container ip address if not running in container");
		}
	}

	public String getHostIpForContainers() {
		return Shell.runAndWait("sh", "-c",
				"ip route | awk '/docker/ { print $NF }'").trim();
	}

}
