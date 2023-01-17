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

import static com.github.dockerjava.api.model.Capability.SYS_ADMIN;
import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_TRANSPORT;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
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
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

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

  private DockerClient client;
  private JerseyDockerCmdExecFactory execFactory;
  private String containerName;
  private String dockerServerUrl;
  private Map<String, String> recordingNameMap = new ConcurrentHashMap<>();

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
          if (line.contains("/docker")) {
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

  public boolean isRunningContainer(String containerName) {
    boolean isRunning = inspectContainer(containerName).getState().getRunning();
    log.trace("Container {} is running: {}", containerName, isRunning);
    return isRunning;
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

  }

  public void mountFiles(CreateContainerCmd createContainerCmd) {
    String videoFilesDiskPath = "/var/lib/jenkins/test-files";
    Volume configVol = new Volume(KurentoTest.getTestFilesDiskPath());
    createContainerCmd.withVolumes(configVol).withBinds(new Bind(videoFilesDiskPath, configVol));
  }

  public void mountDefaultFolders(CreateContainerCmd createContainerCmd) {
    mountDefaultFolders(createContainerCmd, null);
  }

  public void mountDefaultFolders(CreateContainerCmd createContainerCmd, String configFilePath) {

    boolean runningInContainer = isRunningInContainer();

    log.debug("Mounting default folders. Running inside container: {}", runningInContainer);

    if (runningInContainer) {

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
    if (imageId.isEmpty()) {
      return;
    }
    if (force || !existsImage(imageId)) {
      log.debug("Pulling Docker image {} ... please be patient until the process finishes",
          imageId);
      try {
        getClient().pullImageCmd(imageId).exec(new PullImageResultCallback()).awaitCompletion();
      }
      catch (Exception e) {
        log.warn("Exception pulling image {}", imageId, e);
      }
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
        execFactory.close();
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
          execFactory = new JerseyDockerCmdExecFactory();
          DockerCmdExecFactory dockerCmdExecFactory = execFactory.withMaxPerRouteConnections(100);
          client = DockerClientBuilder.getInstance(dockerServerUrl).withDockerCmdExecFactory(dockerCmdExecFactory).build();
        }
      }
    }
    return client;
  }

  public void stopContainers(boolean withRecording, String... containerNames) {
    for (String containerName : containerNames) {
      stopContainer(containerName, withRecording);
    }
  }

  public void stopContainer(String containerName, boolean withRecording) {
    if (isRunningContainer(containerName)) {
      log.debug("Stopping container {}", containerName);

      if (withRecording) {
        String stopRecordingOutput = execCommand(containerName, true, "stop-video-recording.sh");
        log.debug("Stopping recording in container {}:", containerName, stopRecordingOutput);

        try {
          // Wait for FFMPEG to finish writing recording file
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          log.warn("Exception waiting for recording file", e);
        }

        if (recordingNameMap.containsKey(containerName)) {
          String recordingName = recordingNameMap.get(containerName);
          copyFileFromContainer(containerName, "/home/ubuntu/recordings/" + recordingName + ".mp4",
              KurentoTest.getDefaultOutputFolder().getAbsolutePath());
          recordingNameMap.remove(containerName);
        }
      }

      getClient().stopContainerCmd(containerName).exec();

    } else {
      log.debug("Container {} is not running", containerName);
    }
  }

  public String getBrowserIdFromContainerName(String containerName) {
    String keyword = "JOB_SETUP";
    int indexOfKeyWord = containerName.indexOf(keyword);
    if (indexOfKeyWord != -1) {
      int i = containerName.indexOf("-", keyword.length() + indexOfKeyWord + 1);
      if (i != -1) {
        int j = containerName.indexOf("-", i + 1);
        if (j != -1) {
          containerName = containerName.substring(i + 1, j);
        }
      }
    }
    return containerName;
  }

  public void listFolderInContainer(String containerName, String folderName) {
    String lsRecordingsFolder = execCommand(containerName, true, "ls", "-la", folderName);
    log.debug("List of folder {} in container {}:\n{}", folderName, containerName,
        lsRecordingsFolder);
  }

  public void removeContainers(String... containerNames) {
    for (String containerName : containerNames) {
      removeContainer(containerName);
    }
  }

  public void removeContainer(String containerName) {
    log.debug("Removing container {}", containerName);
    boolean removed = false;
    int count = 0;
    do {
      try {
        count++;
        getClient().removeContainerCmd(containerName).withRemoveVolumes(true).exec();
        log.trace("*** Only for debugging: After Docker.removeContainer({}). Times: {}",
            containerName, count);
        removed = true;
      } catch (Throwable e) {
        if (count == 10) {
          log.trace("*** Only for debugging: Exception {} -> Docker.removeContainer({}).",
              containerName, e.getMessage());
        }
        try {
          log.trace("Waiting for removing {}. Times: {}", containerName, count);
          Thread.sleep(WAIT_CONTAINER_POLL_TIMEOUT);
        } catch (InterruptedException e1) {
          // Nothing to do
        }
      }
    } while (!removed && count <= 10);
  }

  public void stopAndRemoveContainer(String containerName, boolean withRecording) {
    stopContainer(containerName, withRecording);
    removeContainer(containerName);
  }

  public void startNode(String id, BrowserType browserType, String nodeName, String imageId,
      boolean record) {
    // Create node
    pullImageIfNecessary(imageId, true);

    log.debug("Creating container for browser '{}'", id);

    CreateContainerCmd createContainerCmd =
        getClient().createContainerCmd(imageId).withPrivileged(true).withCapAdd(SYS_ADMIN).withName(nodeName);
    mountDefaultFolders(createContainerCmd);
    mountFiles(createContainerCmd);

    if (isRunningInContainer()) {
      createContainerCmd.withNetworkMode("bridge");
    }

    createContainerCmd.exec();
    log.debug("Container {} started...", nodeName);


    // Start node if stopped
    startContainer(nodeName);

    startRecordingIfNeeded(id, nodeName, record);

    logMounts(nodeName);

    logNetworks(nodeName);

    listFolderInContainer(nodeName, KurentoTest.getTestFilesDiskPath());
  }

  private void logMounts(String containerId) {
    InspectContainerResponse exec = getClient().inspectContainerCmd(containerId).exec();
    List<Mount> mounts = exec.getMounts();
    log.debug("There are {} mount(s) in the container {}:", mounts.size(), containerId);
    for (int i = 0; i < mounts.size(); i++) {
      Mount mount = mounts.get(i);
      log.debug("{}) {} -> {} ({})", i + 1, mount.getSource(), mount.getDestination(), mount.getMode());
    }
  }

  private void logNetworks(String containerId) {
      Map<String, ContainerNetwork> networks = getClient().inspectContainerCmd(containerId).exec().getNetworkSettings().getNetworks();
      int networksSize = networks.size();
      log.debug("There are {} network(s) in the container {}", networksSize, containerId);
      if (networksSize == 0) {
          return;
      }
      int i = 0;
      for (Entry<String, ContainerNetwork> network : networks.entrySet()) {
          log.debug("{}) {} -> {}", ++i, network.getKey(), network.getValue());
      }
  }

  private void startRecordingIfNeeded(String id, String containerName, boolean record) {
    if (record) {
      // Start recording with script
      String browserId = getBrowserIdFromContainerName(containerName);
      String recordingName = KurentoTest.getSimpleTestName() + "-" + browserId + "-recording";
      recordingNameMap.put(containerName, recordingName);

      log.debug("Starting recording in container {} (browser {}) (target file {})", containerName,
          browserId, recordingName);
      String startRecordingOutput = execCommand(containerName, false, "start-video-recording.sh",
          "-n", recordingName);
      log.debug("Recording in container {} started (command result {})", containerName, startRecordingOutput);
    }
  }

  public void startNode(String id, BrowserType browserType, String nodeName, String imageId,
      boolean record, String containerIp) {
    // Create node
    pullImageIfNecessary(imageId, true);

    log.debug("Creating container for browser '{}'", id);

    CreateContainerCmd createContainerCmd =
        getClient().createContainerCmd(imageId).withPrivileged(true).withCapAdd(SYS_ADMIN).withName(nodeName);
    mountDefaultFolders(createContainerCmd);
    mountFiles(createContainerCmd);

    createContainerCmd.withNetworkMode("none");

    Map<String, String> labels = new HashMap<>();
    labels.put("KurentoDnat", "true");
    labels.put("Transport", getProperty(TEST_SELENIUM_TRANSPORT));
    labels.put("IpAddress", containerIp);

    createContainerCmd.withLabels(labels);

    createContainerCmd.exec();

    log.debug("Container {} started...", nodeName);

    // Start node if stopped
    startContainer(nodeName);

    startRecordingIfNeeded(id, nodeName, record);

    logMounts(nodeName);

    logNetworks(nodeName);
  }

  public void startAndWaitNode(String id, BrowserType browserType, String nodeName, String imageId,
     boolean record) {
    startNode(id, browserType, nodeName, imageId, record);
    waitForContainer(nodeName);
  }

  public void startAndWaitNode(String id, BrowserType browserType, String nodeName, String imageId,
     boolean record, String containerIp) {
    startNode(id, browserType, nodeName, imageId, record, containerIp);
    waitForContainer(nodeName);
  }

  public void waitForContainer(String containerName) {
    boolean isRunning = false;

    long timeoutMs =
        System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(WAIT_CONTAINER_POLL_TIMEOUT);
    do {
      // Check timeout
      if (System.currentTimeMillis() > timeoutMs) {
        throw new KurentoException("Timeout of " + WAIT_CONTAINER_POLL_TIMEOUT
            + " seconds waiting for container " + containerName);
      }

      isRunning = isRunningContainer(containerName);
      if (!isRunning) {
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
      String ipAddr = getContainerNetworks().values().iterator().next().getIpAddress();
      log.trace("Docker container IP address {}", ipAddr);
      return ipAddr;
    } else {
      throw new DockerClientException(
          "Can't obtain container ip address if not running in container");
    }
  }

  public Map<String, ContainerNetwork> getContainerNetworks() {
      if (isRunningInContainer()) {
          Map<String, ContainerNetwork> networks = inspectContainer(getContainerName()).getNetworkSettings()
              .getNetworks();
          log.trace("Docker container networks {}", networks);
          return networks;
      } else {
          throw new DockerClientException(
              "Can't obtain container ip address if not running in container");
      }
  }

  public String getContainerFirstNetworkName() {
     return getContainerNetworks().keySet().iterator().next();
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

  public String execCommand(String containerId, boolean awaitCompletion, String... command) {
    ExecCreateCmdResponse exec = client.execCreateCmd(containerId).withCmd(command).withTty(false)
        .withAttachStdin(true).withAttachStdout(true).withAttachStderr(true).exec();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    String output = null;
    try {
      ExecStartResultCallback resultCallback = client.execStartCmd(exec.getId()).withDetach(false)
          .withTty(true).exec(new ExecStartResultCallback(outputStream, System.err));
      if (awaitCompletion) {
          resultCallback.awaitCompletion();
      }
      output = new String(outputStream.toByteArray());
    } catch (InterruptedException e) {
      log.warn("Exception executing command {} on container {}", Arrays.toString(command),
          containerId, e);
    }

    return output;
  }


  public void copyFileFromContainer(String containerName, String containerFile, String hostFolder) {
    log.trace("Copying {} from container {} to host folder {}", containerFile, containerName,
        hostFolder);
    try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
        client.copyArchiveFromContainerCmd(containerName, containerFile).exec())) {
      unTar(tarStream, new File(hostFolder));
    } catch (Exception e) {
      log.warn("Exception getting tar file from container {}", e.getMessage());
    }
  }

  private void unTar(TarArchiveInputStream tis, File destFolder) throws IOException {
    TarArchiveEntry entry = null;
    while ((entry = tis.getNextTarEntry()) != null) {
      FileOutputStream fos = null;
      try {
        if (entry.isDirectory()) {
          continue;
        }
        File curfile = new File(destFolder, entry.getName());
        File parent = curfile.getParentFile();
        if (!parent.exists()) {
          parent.mkdirs();
        }
        fos = new FileOutputStream(curfile);
        IOUtils.copy(tis, fos);
      } catch (Exception e) {
        log.warn("Exception extracting {} to {}", tis, destFolder, e);
      } finally {
        try {
          if (fos != null) {
            fos.flush();
            fos.getFD().sync();
            fos.close();
          }
        } catch (IOException e) {
          log.warn("Exception closing {}", fos, e);
        }
      }
    }
  }

}
