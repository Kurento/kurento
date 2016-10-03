/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.docker;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_TRANSPORT;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.config.TestConfiguration;
import org.kurento.test.utils.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
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
  private static final String DOCKER_SERVER_URL_DEFAULT = "unix:///var/run/docker.sock";

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

    return getSingleton(
        PropertiesManager.getProperty(DOCKER_SERVER_URL_PROPERTY, getDefaultDockerServerUrl()));
  }

  private static String getDefaultDockerServerUrl() {
    return DOCKER_SERVER_URL_DEFAULT;
  }

  public Docker(String dockerServerUrl) {
    this.dockerServerUrl = dockerServerUrl;
  }

  public boolean isRunningInContainer() {
    return isRunningInContainerInternal();
  }

  private static synchronized boolean isRunningInContainerInternal() {

    if (isRunningInContainer == null) {

      try (BufferedReader br =
          Files.newBufferedReader(Paths.get("/proc/1/cgroup"), StandardCharsets.UTF_8)) {

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

          String ipRoute = Shell.runAndWait("sh", "-c", "/sbin/ip route");

          String[] tokens = ipRoute.split("\\s");

          hostIp = tokens[2];

        } catch (Exception e) {
          throw new DockerClientException("Exception executing /sbin/ip route", e);
        }

      } else {
        hostIp = "127.0.0.1";
      }
    }

    log.debug("Host IP is {}", hostIp);

    return hostIp;
  }

  public boolean isRunningContainer(String containerName) {
    boolean isRunning = false;
    if (existsContainer(containerName)) {
      isRunning = inspectContainer(containerName).getState().getRunning();
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

  public void createContainer(String imageId, String containerName, boolean mountFolders,
      String... env) {

    if (!existsContainer(containerName)) {

      pullImageIfNecessary(imageId, false);

      log.debug("Creating container {}", containerName);

      CreateContainerCmd createContainerCmd =
          getClient().createContainerCmd(imageId).withName(containerName).withEnv(env)
          .withVolumes(new Volume("/var/run/docker.sock"));

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

  public void mountDefaultFolders(CreateContainerCmd createContainerCmd, String configFilePath) {

    if (isRunningInContainer()) {

      createContainerCmd.withVolumesFrom(new VolumesFrom(getContainerId()));

      if (configFilePath != null) {

        String workspace = PropertiesManager.getProperty(TestConfiguration.TEST_WORKSPACE_PROP,
            TestConfiguration.TEST_WORKSPACE_DEFAULT);

        String workspaceHost =
            PropertiesManager.getProperty(TestConfiguration.TEST_WORKSPACE_HOST_PROP,
                TestConfiguration.TEST_WORKSPACE_HOST_DEFAULT);

        String hostConfigFilePath = Paths.get(workspaceHost)
            .resolve(Paths.get(workspace).relativize(Paths.get(configFilePath))).toString();

        log.debug("Config file volume {}", hostConfigFilePath);

        Volume configVol = new Volume("/opt/selenium/config.json");

        createContainerCmd.withVolumes(configVol)
            .withBinds(new Bind(hostConfigFilePath, configVol));
      }

    } else {

      String testFilesPath = KurentoTest.getTestFilesDiskPath();
      Volume testFilesVolume = new Volume(testFilesPath);

      String workspacePath = Paths.get(KurentoTest.getTestDir()).toAbsolutePath().toString();
      Volume workspaceVolume = new Volume(workspacePath);

      Volume configVol = new Volume("/opt/selenium/config.json");

      Volume dockerSock = new Volume("/var/run/docker.sock");

      if (configFilePath != null) {

        createContainerCmd.withVolumes(testFilesVolume, workspaceVolume, configVol, dockerSock)
            .withBinds(
            new Bind(testFilesPath, testFilesVolume, AccessMode.ro),
            new Bind(workspacePath, workspaceVolume, AccessMode.rw),
            new Bind(configFilePath, configVol));
      } else {

        createContainerCmd.withVolumes(testFilesVolume, workspaceVolume, dockerSock).withBinds(
            new Bind(testFilesPath, testFilesVolume, AccessMode.ro),
            new Bind(workspacePath, workspaceVolume, AccessMode.rw));
      }
    }
  }

  public void pullImageIfNecessary(String imageId, boolean force) {
    if (force || !existsImage(imageId)) {
      log.debug("Pulling Docker image {} ... please be patient until the process finishes",
          imageId);
      getClient().pullImageCmd(imageId).exec(new PullImageResultCallback()).awaitSuccess();
      log.debug("Image {} downloaded", imageId);

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

  @Override
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
          client = DockerClientBuilder.getInstance(dockerServerUrl).build();
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
      boolean removed = false;
      int count = 0;
      do {
        try {
          count++;
          getClient().removeContainerCmd(containerName).withRemoveVolumes(true).exec();
          log.debug("*** Only for debuggin: After Docker.removeContainer({}). Times: {}",
              containerName, count);
          removed = true;
        } catch (Throwable e) {
          if (count == 10) {
            log.error("*** Only for debugging: Exception {} -> Docker.removeContainer({}).",
                containerName, e.getMessage());
          }
          try {
            log.debug("Waiting for removing {}. Times: {}", containerName, count);
            Thread.sleep(WAIT_CONTAINER_POLL_TIMEOUT);
          } catch (InterruptedException e1) {
            // Nothing todo
          }
        }
      } while (!removed && count <= 10);
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
    String hubIp = inspectContainer(hubName).getNetworkSettings().getIpAddress();
    log.debug("Hub started on IP address: {}", hubIp);
    return hubIp;
  }

  public void startNode(String id, BrowserType browserType, String nodeName, String imageId,
      String hubIp) {
    // Create node if not exist
    if (!existsContainer(nodeName)) {

      pullImageIfNecessary(imageId, false);

      log.debug("Creating container {}", nodeName);

      CreateContainerCmd createContainerCmd =
          getClient().createContainerCmd(imageId).withName(nodeName);

      String configFile = generateConfigFile(id, browserType);

      mountDefaultFolders(createContainerCmd, configFile);

      createContainerCmd.withEnv(new String[] { "HUB_PORT_4444_TCP_ADDR=" + hubIp });

      createContainerCmd.exec();

      log.debug("Container {} started...", nodeName);

    } else {
      log.debug("Container {} already exists", nodeName);
    }

    // Start node if stopped
    startContainer(nodeName);
  }

  public void startNode(String id, BrowserType browserType, String nodeName, String imageId,
      String hubIp, String containerIp) {
    // Create node if not exist
    if (!existsContainer(nodeName)) {

      pullImageIfNecessary(imageId, false);

      log.debug("Creating container {}", nodeName);

      CreateContainerCmd createContainerCmd =
          getClient().createContainerCmd(imageId).withName(nodeName);

      String configFile = generateConfigFile(id, browserType);

      mountDefaultFolders(createContainerCmd, configFile);

      createContainerCmd.withNetworkMode("none");

      Map<String, String> labels = new HashMap<>();
      labels.put("KurentoDnat", "true");
      labels.put("Transport", getProperty(TEST_SELENIUM_TRANSPORT));
      labels.put("IpAddress", containerIp);

      createContainerCmd.withLabels(labels);
      createContainerCmd.withEnv(new String[] { "HUB_PORT_4444_TCP_ADDR=" + hubIp,
          "REMOTE_HOST=http://" + containerIp + ":5555" });

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

      String workspace = PropertiesManager.getProperty(TestConfiguration.TEST_WORKSPACE_PROP,
          TestConfiguration.TEST_WORKSPACE_DEFAULT);

      Path config = Files.createTempFile(Paths.get(workspace), "", "-config.json",
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")));

      String browserName1;
      String browserName2;

      if (browserType == BrowserType.CHROME) {
        browserName1 = "*googlechrome";
        browserName2 = "chrome";
      } else if (browserType == BrowserType.FIREFOX) {
        browserName1 = "*firefox";
        browserName2 = "firefox";
      } else {
        throw new KurentoException("Unsupported browser type: " + browserType);
      }

      try (Writer w = Files.newBufferedWriter(config, StandardCharsets.UTF_8)) {
        w.write("{\n" + "  \"capabilities\": [\n" + "    {\n" + "      \"browserName\": \""
            + browserName1 + "\",\n" + "      \"maxInstances\": 1,\n"
            + "      \"seleniumProtocol\": \"Selenium\",\n" + "      \"applicationName\": \"" + id
            + "\"\n" + "    },\n" + "    {\n" + "      \"browserName\": \"" + browserName2 + "\",\n"
            + "      \"maxInstances\": 1,\n" + "      \"seleniumProtocol\": \"WebDriver\",\n"
            + "      \"applicationName\": \"" + id + "\"\n" + "    }\n" + "  ],\n"
            + "  \"configuration\": {\n"
            + "    \"proxy\": \"org.openqa.grid.selenium.proxy.DefaultRemoteProxy\",\n"
            + "    \"maxSession\": 1,\n" + "    \"port\": 5555,\n" + "    \"register\": true,\n"
            + "    \"registerCycle\": 5000\n" + "  }\n" + "}");
      }

      return config.toAbsolutePath().toString();

    } catch (IOException e) {
      throw new KurentoException("Exception creating config file", e);
    }
  }

  public void startAndWaitNode(String id, BrowserType browserType, String nodeName, String imageId,
      String hubIp) {
    startNode(id, browserType, nodeName, imageId, hubIp);
    waitForContainer(nodeName);
  }

  public void startAndWaitNode(String id, BrowserType browserType, String nodeName, String imageId,
      String hubIp, String containerIp) {
    startNode(id, browserType, nodeName, imageId, hubIp, containerIp);
    waitForContainer(nodeName);
  }

  public String startAndWaitHub(String hubName, String imageId) {
    String hubIp = startHub(hubName, imageId);
    waitForContainer(hubName);
    return hubIp;
  }

  public void waitForContainer(String containerName) {
    boolean isRunning = false;

    long timeoutMs =
        System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(WAIT_CONTAINER_POLL_TIMEOUT);
    do {
      isRunning = isRunningContainer(containerName);
      if (!isRunning) {

        // Check timeout
        if (System.currentTimeMillis() > timeoutMs) {
          throw new KurentoException("Timeout of " + WAIT_CONTAINER_POLL_TIMEOUT
              + " seconds waiting for container " + containerName);
        }

        try {
          // Wait WAIT_HUB_POLL_TIME ms
          log.debug("Container {} is not still running ... waiting {} ms", containerName,
              WAIT_CONTAINER_POLL_TIME);
          Thread.sleep(WAIT_CONTAINER_POLL_TIME);

        } catch (InterruptedException e) {
          log.error("Exception waiting for hub");
        }

      }
    } while (!isRunning);
  }

  public String getContainerId() {
    try {

      BufferedReader br =
          Files.newBufferedReader(Paths.get("/proc/self/cgroup"), StandardCharsets.UTF_8);

      String line = null;
      while ((line = br.readLine()) != null) {
        log.debug(line);
        if (line.contains("docker")) {
          return line.substring(line.lastIndexOf('/') + 1, line.length());
        }
      }

      throw new DockerClientException("Exception obtaining containerId. "
          + "The file /proc/self/cgroup doesn't contain a line with 'docker'");

    } catch (IOException e) {
      throw new DockerClientException(
          "Exception obtaining containerId. " + "Exception reading file /proc/self/cgroup", e);
    }
  }

  public String getContainerName() {

    if (!isRunningInContainer()) {
      throw new DockerClientException("Can't obtain container name if not running in container");
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
      String ipAddr = inspectContainer(getContainerName()).getNetworkSettings().getIpAddress();
      log.debug("Docker container IP address {}", ipAddr);
      return ipAddr;
    } else {
      throw new DockerClientException(
          "Can't obtain container ip address if not running in container");
    }
  }

  public String getHostIpForContainers() {
    try {
      Enumeration<NetworkInterface> b = NetworkInterface.getNetworkInterfaces();
      while (b.hasMoreElements()) {
        NetworkInterface iface = b.nextElement();
        if (iface.getName().contains("docker")) {
          for (InterfaceAddress f : iface.getInterfaceAddresses()) {
            if (f.getAddress().isSiteLocalAddress()) {
              String addr = f.getAddress().toString();
              log.debug("Host IP for container is {}", addr);
              return addr;
            }
          }
        }
      }
    } catch (SocketException e) {
      // This shouldn't happen
      log.warn("Exception getting docker address", e);
    }

    return null;
  }

  /**
   * Return an ip address according with some parameters for testing Ice
   *
   * @param container
   * @param webRtcCandidate
   * @param isKmsDnat
   * @param isSeleniumDnat
   * @param isUpdTransport
   * @return
   */
  public String generateIpAddressForContainer() {

    String baseIpAddress = "172.17";
    String ipAddress = "";
    Random random = new Random();
    Integer x;
    Integer y;
    String output = "";

    do {
      x = random.nextInt((240 - 1) + 1) + 1;
      y = random.nextInt((240 - 1) + 1) + 1;
      ipAddress = baseIpAddress + "." + x + "." + y;
      output = Shell.runAndWaitString("ping -c 1 " + ipAddress);
    } while (!output.contains("Destination Host Unreachable"));

    log.debug("Ip address generated: {}", ipAddress);
    return ipAddress;
  }

  public void downloadLog(String containerName, Path file) throws IOException {

    LogContainerRetrieverCallback loggingCallback = new LogContainerRetrieverCallback(file);

    getClient().logContainerCmd(containerName).withStdErr(true).withStdOut(true)
        .exec(loggingCallback);

    try {
      loggingCallback.awaitCompletion();
    } catch (InterruptedException e) {
      log.warn("Interrupted while downloading logs for container {}", containerName);
    }
  }

  public static class LogContainerRetrieverCallback extends LogContainerResultCallback {

    private PrintWriter pw;

    public LogContainerRetrieverCallback(Path file) throws IOException {
      pw = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
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
      return getClient().statsCmd(containerId).exec(resultCallback).waitForObject();
    } catch (InterruptedException e) {
      throw new KurentoException("Interrupted while waiting for statistics");
    }
  }

  public String execCommand(String containerId, String... command) {
    ExecCreateCmdResponse exec = client.execCreateCmd(containerId).withCmd(command).withTty(false)
        .withAttachStdin(true).withAttachStdout(true).withAttachStderr(true).exec();
    OutputStream outputStream = new ByteArrayOutputStream();
    String output = null;
    try {
      client.execStartCmd(exec.getId()).withDetach(false).withTty(true)
          .exec(new ExecStartResultCallback(outputStream, System.err)).awaitCompletion();
      output = outputStream.toString();// IOUtils.toString(outputStream, Charset.defaultCharset());
    } catch (InterruptedException e) {
      log.warn("Exception executing command {} on container {}", Arrays.toString(command),
          containerId, e);
    }

    return output;
  }

}
