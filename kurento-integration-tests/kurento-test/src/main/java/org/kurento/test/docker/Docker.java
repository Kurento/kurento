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
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.Shell;
import org.kurento.test.TestConfiguration;
import org.kurento.test.browser.BrowserType;
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
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * Docker client for tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class Docker implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(Docker.class);

	private static final String DOCKER_SERVER_URL_PROPERTY = "docker.server.url";
	private static final String DOCKER_SERVER_URL_DEFAULT = "http://localhost:2375";

	public static final String DOCKER_CONTAINER_NAME_PROPERTY = "docker.container.name";

	private static final int WAIT_CONTAINER_POLL_TIME = 200; // milliseconds
	private static final int WAIT_CONTAINER_POLL_TIMEOUT = 10; // seconds

	private static Docker singleton = null;
	private static Boolean isRunningInContainer;
	private static String hostIp;

	private DockerClient client;
	private String containerName;
	private String dockerServerUrl;

	public static Docker getSingleton(String dockerServerUrl) {
		if (singleton == null) {
			synchronized (Docker.class) {
				if (singleton == null) {
					singleton = new Docker(dockerServerUrl);
				}
			}
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

	public Docker(String dockerServerUrl) {
		this.dockerServerUrl = dockerServerUrl;
	}

	public boolean isRunningInContainer() {
		return isRunningInContainerInternal();
	}

	private static synchronized boolean isRunningInContainerInternal() {

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

	private static synchronized String getHostIp() {

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
			boolean mountFolders, String... env) {

		if (!existsContainer(containerName)) {

			pullImageIfNecessary(imageId);

			log.debug("Creating container {}", containerName);

			CreateContainerCmd createContainerCmd = getClient()
					.createContainerCmd(imageId).withName(containerName)
					.withEnv(env);

			if (mountFolders) {
				mountDefaultFolders(createContainerCmd);
			}

			createContainerCmd.exec();

			log.debug("Container {} started...", containerName);

		} else {
			log.debug("Container {} already exists", containerName);
		}
	}

	public void mountDefaultFolders(CreateContainerCmd createContainerCmd) {
		mountDefaultFolders(createContainerCmd, null);
	}

	public void mountDefaultFolders(CreateContainerCmd createContainerCmd,
			String configFilePath) {

		if (isRunningInContainer()) {

			createContainerCmd
					.withVolumesFrom(new VolumesFrom(getContainerId()));

			if (configFilePath != null) {

				String workspace = PropertiesManager.getProperty(
						TestConfiguration.KURENTO_WORKSPACE_PROP,
						TestConfiguration.KURENTO_WORKSPACE_DEFAULT);

				String workspaceHost = PropertiesManager.getProperty(
						TestConfiguration.KURENTO_WORKSPACE_HOST_PROP,
						TestConfiguration.KURENTO_WORKSPACE_HOST_DEFAULT);

				String hostConfigFilePath = Paths.get(workspaceHost)
						.resolve(Paths.get(workspace)
								.relativize(Paths.get(configFilePath)))
						.toString();

				log.info("Config file volume {}", hostConfigFilePath);

				Volume configVol = new Volume("/opt/selenium/config.json");

				createContainerCmd.withVolumes(configVol)
						.withBinds(new Bind(hostConfigFilePath, configVol));
			}

		} else {

			String testFilesPath = KurentoServicesTestHelper.getTestFilesPath();
			Volume testFilesVolume = new Volume(testFilesPath);

			String workspacePath = Paths
					.get(KurentoServicesTestHelper.getTestDir())
					.toAbsolutePath().toString();
			Volume workspaceVolume = new Volume(workspacePath);

			Volume configVol = new Volume("/opt/selenium/config.json");

			if (configFilePath != null) {

				createContainerCmd
						.withVolumes(testFilesVolume, workspaceVolume,
								configVol)
						.withBinds(
								new Bind(testFilesPath, testFilesVolume,
										AccessMode.ro),
								new Bind(workspacePath, workspaceVolume,
										AccessMode.rw),
								new Bind(configFilePath, configVol));
			} else {

				createContainerCmd.withVolumes(testFilesVolume, workspaceVolume)
						.withBinds(
								new Bind(testFilesPath, testFilesVolume,
										AccessMode.ro),
								new Bind(workspacePath, workspaceVolume,
										AccessMode.rw));
			}
		}
	}

	public void pullImageIfNecessary(String imageId) {
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
		if (client == null) {
			synchronized (this) {
				if (client == null) {
					client = DockerClientBuilder.getInstance(dockerServerUrl)
							.build();
				}
			}
		}
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

			getClient().removeContainerCmd(containerName)
					.withRemoveVolumes(true).exec();
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

	public synchronized String startHub(String hubName, String imageId) {
		// Create hub if not exist
		createContainer(imageId, hubName, false, "GRID_TIMEOUT=3600000");

		// Start hub if stopped
		startContainer(hubName);

		// Read IP address
		String hubIp = inspectContainer(hubName).getNetworkSettings()
				.getIpAddress();
		log.debug("Hub started on IP address: {}", hubIp);
		return hubIp;
	}

	public void startNode(String id, BrowserType browserType, String nodeName,
			String imageId, String hubIp) {
		// Create node if not exist
		if (!existsContainer(nodeName)) {

			pullImageIfNecessary(imageId);

			log.debug("Creating container {}", nodeName);

			CreateContainerCmd createContainerCmd = getClient()
					.createContainerCmd(imageId).withName(nodeName).withEnv(
							new String[] { "HUB_PORT_4444_TCP_ADDR=" + hubIp });

			String configFile = generateConfigFile(id, browserType);

			mountDefaultFolders(createContainerCmd, configFile);

			createContainerCmd.exec();

			log.debug("Container {} started...", nodeName);

		} else {
			log.debug("Container {} already exists", nodeName);
		}

		// Start node if stopped
		startContainer(nodeName);
	}

	private String generateConfigFile(String id, BrowserType browserType) {

		try {

			String workspace = PropertiesManager.getProperty(
					TestConfiguration.KURENTO_WORKSPACE_PROP,
					TestConfiguration.KURENTO_WORKSPACE_DEFAULT);

			Path config = Files.createTempFile(Paths.get(workspace), "",
					"-config.json", PosixFilePermissions.asFileAttribute(
							PosixFilePermissions.fromString("rw-r--r--")));

			String browserName1;
			String browserName2;

			if (browserType == BrowserType.CHROME) {
				browserName1 = "*googlechrome";
				browserName2 = "chrome";
			} else if (browserType == BrowserType.FIREFOX) {
				browserName1 = "*firefox";
				browserName2 = "firefox";
			} else {
				throw new KurentoException(
						"Unsupported browser type: " + browserType);
			}

			try (Writer w = Files.newBufferedWriter(config,
					StandardCharsets.UTF_8)) {
				w.write("{\n" + "  \"capabilities\": [\n" + "    {\n"
						+ "      \"browserName\": \"" + browserName1 + "\",\n"
						+ "      \"maxInstances\": 1,\n"
						+ "      \"seleniumProtocol\": \"Selenium\",\n"
						+ "      \"applicationName\": \"" + id + "\"\n"
						+ "    },\n" + "    {\n" + "      \"browserName\": \""
						+ browserName2 + "\",\n"
						+ "      \"maxInstances\": 1,\n"
						+ "      \"seleniumProtocol\": \"WebDriver\",\n"
						+ "      \"applicationName\": \"" + id + "\"\n"
						+ "    }\n" + "  ],\n" + "  \"configuration\": {\n"
						+ "    \"proxy\": \"org.openqa.grid.selenium.proxy.DefaultRemoteProxy\",\n"
						+ "    \"maxSession\": 1,\n" + "    \"port\": 5555,\n"
						+ "    \"register\": true,\n"
						+ "    \"registerCycle\": 5000\n" + "  }\n" + "}");
			}

			return config.toAbsolutePath().toString();

		} catch (IOException e) {
			throw new KurentoException("Exception creating config file", e);
		}
	}

	public void startAndWaitNode(String id, BrowserType browserType,
			String nodeName, String imageId, String hubIp) {
		startNode(id, browserType, nodeName, imageId, hubIp);
		waitForContainer(nodeName);
	}

	public String startAndWaitHub(String hubName, String imageId) {
		String hubIp = startHub(hubName, imageId);
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

	public void downloadLog(String containerName, Path file)
			throws IOException {

		LogContainerRetrieverCallback loggingCallback = new LogContainerRetrieverCallback(
				file);

		getClient().logContainerCmd(containerName).withStdErr().withStdOut()
				.exec(loggingCallback);

		try {
			loggingCallback.awaitCompletion();
		} catch (InterruptedException e) {
			log.warn("Interrupted while downloading logs for container {}",
					containerName);
		}
	}

	public static class LogContainerRetrieverCallback
			extends LogContainerResultCallback {

		private PrintWriter pw;

		public LogContainerRetrieverCallback(Path file) throws IOException {
			pw = new PrintWriter(
					Files.newBufferedWriter(file, StandardCharsets.UTF_8));
		}

		@Override
		public void onNext(Frame frame) {
			pw.append(new String(frame.getPayload()));
			super.onNext(frame);
		}

		@Override
		public void onComplete() {
			pw.close();
			super.onComplete();
		}
	}

	public Statistics getStatistics(String containerId) {
		FirstObjectResultCallback<Statistics> resultCallback = new FirstObjectResultCallback<>();

		try {
			return getClient().statsCmd().withContainerId(containerId)
					.exec(resultCallback).waitForObject();
		} catch (InterruptedException e) {
			throw new RuntimeException(
					"Interrupted while waiting for statistics");
		}
	}

}
