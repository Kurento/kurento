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

package org.kurento.test.services;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.browser.WebRtcCandidateType.RELAY;
import static org.kurento.test.browser.WebRtcCandidateType.SRFLX;
import static org.kurento.test.config.TestConfiguration.AUTOSTART_FALSE_VALUE;
import static org.kurento.test.config.TestConfiguration.AUTOSTART_TESTCLASS_VALUE;
import static org.kurento.test.config.TestConfiguration.AUTOSTART_TESTSUITE_VALUE;
import static org.kurento.test.config.TestConfiguration.AUTOSTART_TEST_VALUE;
import static org.kurento.test.config.TestConfiguration.KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_AUTOSTART_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_IMAGE_FORCE_PULLING_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_IMAGE_FORCE_PULLING_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_IMAGE_NAME_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_IMAGE_NAME_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_S3_ACCESS_KEY_ID;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_S3_BUCKET_NAME;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_S3_HOSTNAME;
import static org.kurento.test.config.TestConfiguration.KMS_DOCKER_S3_SECRET_ACCESS_KEY;
import static org.kurento.test.config.TestConfiguration.KMS_GENERATE_RTP_PTS_STATS_PROPERTY;
import static org.kurento.test.config.TestConfiguration.KMS_GST_PLUGINS_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_LOGIN_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_LOG_PATH_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_LOG_PATH_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_PASSWD_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_PEM_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_DOCKER;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_ELASTEST;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_SERVER_COMMAND_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_SERVER_COMMAND_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_SERVER_DEBUG_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_SERVER_DEBUG_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_PROP_EXPORT;
import static org.kurento.test.config.TestConfiguration.KSM_GST_PLUGINS_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_CANDIDATE_KMS_TYPE;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_CANDIDATE_SELENIUM_TYPE;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_SERVER_URL_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_KMS_DNAT;
import static org.kurento.test.config.TestConfiguration.TEST_KMS_DNAT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_KMS_TRANSPORT;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_DNAT;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_DNAT_DEFAULT;
import static org.kurento.test.services.TestService.TestServiceScope.EXTERNAL;
import static org.kurento.test.services.TestService.TestServiceScope.TEST;
import static org.kurento.test.services.TestService.TestServiceScope.TESTCLASS;
import static org.kurento.test.services.TestService.TestServiceScope.TESTSUITE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.io.FileUtils;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ObjectCreatedEvent;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.config.TestConfiguration;
import org.kurento.test.docker.Docker;
import org.kurento.test.utils.Shell;
import org.kurento.test.utils.SshConnection;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.google.common.io.CharStreams;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Kurento Media Server service.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KmsService extends TestService {

  // FIXME replace with a registration mechanism
  protected static String monitoredDockerContainerName;

  protected String defaultDockerContainerName = "kms";
  protected String dockerContainerName = defaultDockerContainerName;
  protected SshConnection remoteKmsSshConnection;
  protected Path workspace;
  protected String wsUri;
  protected boolean isKmsRemote;
  protected boolean isKmsDocker;
  protected boolean isKmsElastest;
  protected boolean isKmsStarted;
  protected String registrarUri;
  protected String registrarLocalAddress = "127.0.0.1";
  protected String kmsLoginProp;
  protected String kmsPasswdProp;
  protected String kmsPemProp;
  protected String kmsAutostartProp;
  protected String kmsAutostartDefault;
  protected String kmsWsUriProp;
  protected String kmsWsUriExportProp;
  protected String kmsScopeProp;
  protected String kmsScopeDefault;
  protected KurentoClient kurentoClient;

  public KmsService(String wsUri) {
    this();
    setWsUri(wsUri);
  }

  public KmsService() {
    this.kmsLoginProp = KMS_LOGIN_PROP;
    this.kmsPasswdProp = KMS_PASSWD_PROP;
    this.kmsPemProp = KMS_PEM_PROP;
    this.kmsAutostartProp = KMS_AUTOSTART_PROP;
    this.kmsAutostartDefault = KMS_AUTOSTART_DEFAULT;
    this.kmsWsUriProp = KMS_WS_URI_PROP;
    this.kmsWsUriExportProp = KMS_WS_URI_PROP_EXPORT;
    this.kmsScopeProp = KMS_SCOPE_PROP;
    this.kmsScopeDefault = KMS_SCOPE_DEFAULT;

    setWsUri(getProperty(kmsWsUriProp, KMS_WS_URI_DEFAULT));
  }

  public KmsService(String kmsLoginProp, String kmsPasswdProp, String kmsPemProp,
      String kmsAutostartProp, String kmsWsUriProp, String kmsWsUriExportProp, String kmsScopeProp,
      String kmsScopeDefault) {
    this.kmsLoginProp = kmsLoginProp;
    this.kmsPasswdProp = kmsPasswdProp;
    this.kmsPemProp = kmsPemProp;
    this.kmsAutostartProp = kmsAutostartProp;
    this.kmsWsUriProp = kmsWsUriProp;
    this.kmsWsUriExportProp = kmsWsUriExportProp;
    this.kmsScopeProp = kmsScopeProp;
    this.kmsScopeDefault = kmsScopeDefault;

    setWsUri(getProperty(kmsWsUriProp, KMS_WS_URI_DEFAULT));
  }

  @Override
  public void start() {
    super.start();

    if (wsUri == null) {
      log.warn("WS URI is null, will not start");
      isKmsStarted = false;
      return;
    }

    isKmsRemote = !wsUri.contains("localhost") && !wsUri.contains("127.0.0.1") && !isKmsDocker && !isKmsElastest;
    isKmsDocker = KMS_SCOPE_DOCKER.equals(getProperty(kmsScopeProp, kmsScopeDefault));
    isKmsElastest = KMS_SCOPE_ELASTEST.equals(getProperty(kmsScopeProp, kmsScopeDefault));

    // Assertion: if KMS remote, credentials should be available
    String kmsLogin = getProperty(kmsLoginProp);
    String kmsPasswd = getProperty(kmsPasswdProp);
    String kmsPem = getProperty(kmsPemProp);
    String kmsAutoStart = getProperty(kmsAutostartProp, kmsAutostartDefault);

    if (isKmsRemote && kmsLogin == null && (kmsPem == null || kmsPasswd == null)) {
      throw new KurentoException(
          "Bad test parameters: " + kmsAutostartProp + "=" + kmsAutoStart + " and " + kmsWsUriProp
              + "=" + wsUri + ". Remote KMS should be started but its credentials are not present: "
              + kmsLoginProp + "=" + kmsLogin + ", " + kmsPasswdProp + "=" + kmsPasswd + ", "
              + kmsPemProp + "=" + kmsPem);
    }

    // Assertion: if local or Dockerized KMS, port should be available
    if (!isKmsRemote && !isFreePort(wsUri)) {
      throw new KurentoException("KMS cannot be started in URI: " + wsUri + ". Port is not free");
    }

    if (isKmsDocker || isKmsElastest) {
      log.debug("Starting KMS dockerized" + (isKmsElastest ? "from ElasTest" : ""));
      Docker dockerClient = Docker.getSingleton();
      if (dockerClient.isRunningInContainer() && !isKmsElastest) {
        setDockerContainerName(dockerClient.getContainerName() + getDockerContainerNameSuffix()
            + "-" + KurentoTest.getTestClassName() + "-" + +new Random().nextInt(3000));
      }
    } else {
      log.debug("Starting KMS with URI: {}", wsUri);

      try {
        workspace = Files.createTempDirectory("kurento-test");
      } catch (IOException e) {
        throw new KurentoException("Exception creating temporal folder", e);
      }
      log.trace("Local folder to store temporal files: {}", workspace);

      if (isKmsRemote) {
        String remoteKmsStr = wsUri.substring(wsUri.indexOf("//") + 2, wsUri.lastIndexOf(":"));
        log.debug("Using remote KMS at {}", remoteKmsStr);
        remoteKmsSshConnection = new SshConnection(remoteKmsStr, kmsLogin, kmsPasswd, kmsPem);
        if (kmsPem != null) {
          remoteKmsSshConnection.setPem(kmsPem);
        }
        remoteKmsSshConnection.start();
        remoteKmsSshConnection.createTmpFolder();
      }

      createKurentoConf();
    }

    if (isKmsRemote && !kmsAutoStart.equals(AUTOSTART_FALSE_VALUE)) {
      String[] filesToBeCopied = { "kurento.conf.json", "kurento.sh" };
      for (String s : filesToBeCopied) {
        remoteKmsSshConnection.scp(workspace + File.separator + s,
            remoteKmsSshConnection.getTmpFolder() + File.separator + s);
      }
      remoteKmsSshConnection.runAndWaitCommand("chmod", "+x",
          remoteKmsSshConnection.getTmpFolder() + File.separator + "kurento.sh");
    }

    startKms();
    waitForKms();
  }

  @Override
  public void stop() {
    super.stop();

    // Close Kurento client
    closeKurentoClient();

    // Stop KMS
    stopKms();

    // Retrieve logs
    try {
      retrieveLogs();
    } catch (IOException e) {
      log.warn("Exception retrieving KMS logs", e);
    }

    if (isKmsDocker) {
      try {
        Docker.getSingleton().removeContainer(dockerContainerName);
        log.trace("*** Only for debugging: Docker.getSingleton().removeContainer({})",
            dockerContainerName);
      } catch (Throwable name) {
        log.trace(" +++ Only for debugging: Exception on Docker.getSingleton().removeContainer({})",
            dockerContainerName);
      }
    }

    log.trace("+++ Only for debugging: After removeContainer {}", dockerContainerName);

    // Delete temporal folder and content
    if (!isKmsDocker) {
      try {
        deleteFolderAndContent(workspace);
      } catch (IOException e) {
        log.warn("Exception deleting temporal folder {}", workspace, e);
      }
    }
    log.trace("+++ Only for debugging: End of KmsService.stop() for: {}", dockerContainerName);
  }

  @Override
  public TestServiceScope getScope() {
    TestServiceScope scope = TESTSUITE;
    String kmsAutostart = getProperty(kmsAutostartProp, kmsAutostartDefault);
    switch (kmsAutostart) {
      case AUTOSTART_FALSE_VALUE:
        scope = EXTERNAL;
        break;
      case AUTOSTART_TEST_VALUE:
        scope = TEST;
        break;
      case AUTOSTART_TESTCLASS_VALUE:
        scope = TESTCLASS;
        break;
      case AUTOSTART_TESTSUITE_VALUE:
        scope = TESTSUITE;
        break;
      default:
        throw new IllegalArgumentException("Unknown autostart value " + kmsAutostart);
    }
    return scope;
  }

  protected String getDockerContainerNameSuffix() {
    return "_kms";
  }

  protected String getDockerLogSuffix() {
    return "-kms";
  }

  private boolean isFreePort(String wsUri) {
    try {
      URI wsUrl = new URI(wsUri);
      String result = Shell.runAndWait("/bin/bash", "-c",
          "nc -z " + wsUrl.getHost() + " " + wsUrl.getPort() + "; echo $?");
      if (result.trim().equals("0")) {
        log.warn("Port " + wsUrl.getPort()
            + " is used. Maybe another KMS instance is running in this port");
        return false;
      }
    } catch (URISyntaxException e) {
      log.warn("WebSocket URI {} is malformed: " + e.getMessage(), wsUri);
    }
    return true;
  }

  private void createKurentoConf() {
    Map<String, Object> data = new HashMap<String, Object>();
    try {
      URI wsAsUri = new URI(wsUri);
      int port = wsAsUri.getPort();
      String path = wsAsUri.getPath();
      data.put("wsPort", String.valueOf(port));
      data.put("wsPath", path.substring(1));
      data.put("registrar", registrarUri);
      data.put("registrarLocalAddress", registrarLocalAddress);

    } catch (URISyntaxException e) {
      throw new KurentoException("Invalid ws uri: " + wsUri);
    }
    data.put("gstPlugins", getGstPlugins());
    data.put("debugOptions", getDebugOptions());
    data.put("serverCommand", getServerCommand());
    data.put("workspace", getKmsLogPath());

    Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    cfg.setClassForTemplateLoading(this.getClass(), "/templates/");

    createFileFromTemplate(cfg, data, "kurento.conf.json");
    createFileFromTemplate(cfg, data, "kurento.sh");

    Shell.runAndWait("chmod", "+x", workspace + File.separator + "kurento.sh");
  }

  private void startKms() {
    String kmsLogPath = getKmsLogPath();
    if (isKmsRemote) {
      remoteKmsSshConnection.runAndWaitCommand("sh", "-c", kmsLogPath + "kurento.sh > /dev/null");
      log.debug("Remote KMS started in URI {}", wsUri);

    } else if (isKmsDocker || isKmsElastest) {
        if(isKmsElastest) {
            dockerContainerName = System.getenv("ET_SUT_CONTAINER_NAME") + "_" + defaultDockerContainerName;
        }
      startDockerizedKms();
    } else {
      Shell.run("sh", "-c", kmsLogPath + "kurento.sh");
      log.debug("Local KMS started in URI {}", wsUri);
    }

    isKmsStarted = true;
  }

  private void waitForKms() {
    long initTime = System.nanoTime();

    @ClientEndpoint
    class WebSocketClient extends Endpoint {

      @OnClose
      @Override
      public void onClose(Session session, CloseReason closeReason) {
      }

      @OnOpen
      @Override
      public void onOpen(Session session, EndpointConfig config) {
      }
    }

    if (wsUri != null) {
      WebSocketContainer container = ContainerProvider.getWebSocketContainer();

      final int retries = 600;
      final int waitTime = 100;

      for (int i = 0; i < retries; i++) {
        try {
          log.debug("({}) Wait for KMS: {}. Container: {}", i, wsUri, container);
          Session wsSession = container.connectToServer(new WebSocketClient(),
              ClientEndpointConfig.Builder.create().build(), new URI(wsUri));
          wsSession.close();

          double time = (System.nanoTime() - initTime) / (double) 1000000;

          log.debug("Connected to KMS in " + String.format("%3.2f", time) + " milliseconds");
          return;
        } catch (DeploymentException | IOException | URISyntaxException e) {
          try {
            log.warn("Exception while waiting for KMS: {}. {}", wsUri, e.getMessage());
            Thread.sleep(waitTime);
          } catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        }
      }

      throw new KurentoException(
          "Timeout of " + retries * waitTime + " millis waiting for KMS " + wsUri);

    } else {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log.error("InterruptedException {}", e.getMessage());
      }
    }
  }

  private void startDockerizedKms() {
    Docker dockerClient = Docker.getSingleton();
    String kmsImageName = getProperty(KMS_DOCKER_IMAGE_NAME_PROP, KMS_DOCKER_IMAGE_NAME_DEFAULT);

    boolean forcePulling =
        getProperty(KMS_DOCKER_IMAGE_FORCE_PULLING_PROP, KMS_DOCKER_IMAGE_FORCE_PULLING_DEFAULT);

    if (!dockerClient.existsImage(kmsImageName) || forcePulling) {
      log.debug("Pulling KMS image {} ... please wait", kmsImageName);
      dockerClient.pullImageIfNecessary(kmsImageName, true);
      log.debug("KMS image {} pulled", kmsImageName);
    }

    log.debug("Starting KMS container...{}", dockerContainerName);

    // Check S3 properties
    String s3BucketName = getProperty(KMS_DOCKER_S3_BUCKET_NAME);
    String s3AccessKeyId = getProperty(KMS_DOCKER_S3_ACCESS_KEY_ID);
    String s3SecretAccessKey = getProperty(KMS_DOCKER_S3_SECRET_ACCESS_KEY);
    String s3Hostname = getProperty(KMS_DOCKER_S3_HOSTNAME);

    Boolean kmsDnat = false;
    if (getProperty(TEST_KMS_DNAT) != null && getProperty(TEST_KMS_DNAT, TEST_KMS_DNAT_DEFAULT)) {
      kmsDnat = true;
    }

    Boolean seleniumDnat = false;
    if (getProperty(TEST_SELENIUM_DNAT) != null
        && getProperty(TEST_SELENIUM_DNAT, TEST_SELENIUM_DNAT_DEFAULT)) {
      seleniumDnat = true;
    }

    String kmsCandidateType = getProperty(TEST_ICE_CANDIDATE_KMS_TYPE);
    String seleniumCandidateType = getProperty(TEST_ICE_CANDIDATE_SELENIUM_TYPE);

    // Check Stun properties
    String kmsStunIp = getProperty(TestConfiguration.KMS_STUN_IP_PROPERTY);
    String kmsStunPort = getProperty(TestConfiguration.KMS_STUN_PORT_PROPERTY);

    CreateContainerCmd createContainerCmd;

    if (kmsDnat && seleniumDnat && RELAY.toString().toUpperCase().equals(kmsCandidateType)
        && SRFLX.toString().toUpperCase().equals(seleniumCandidateType)) {
      // Use Turn for KMS
      String kmsTurnIp = getProperty(TEST_ICE_SERVER_URL_PROPERTY);
      log.debug("Turn Server {}", kmsTurnIp);
      createContainerCmd =
          dockerClient.getClient().createContainerCmd(kmsImageName).withName(dockerContainerName)
          .withEnv("GST_DEBUG=" + getDebugOptions(), "S3_ACCESS_BUCKET_NAME=" + s3BucketName,
              "S3_ACCESS_KEY_ID=" + s3AccessKeyId, "S3_SECRET_ACCESS_KEY=" + s3SecretAccessKey,
              "S3_HOSTNAME=" + s3Hostname, "KMS_TURN_URL=" + kmsTurnIp,
              "KURENTO_GENERATE_RTP_PTS_STATS=" + getKurentoGenerateRtpPtsStats())
              .withCmd("--gst-debug-no-color").withVolumes(new Volume("/var/run/docker.sock"));
    } else {
      if (kmsDnat && seleniumDnat && RELAY.toString().toUpperCase().equals(seleniumCandidateType)
          && SRFLX.toString().toUpperCase().equals(kmsCandidateType)) {
        // Change kmsStunIp by turn values
        kmsStunIp = getProperty(TEST_ICE_SERVER_URL_PROPERTY).split(":")[1];
        kmsStunPort = "3478";
      }

      if (kmsStunIp == null) {
        kmsStunIp = "";
      }

      if (kmsStunPort == null) {
        kmsStunPort = "";
      }

      log.debug("Stun Server {}:{}", kmsStunIp, kmsStunPort);

      createContainerCmd =
          dockerClient.getClient().createContainerCmd(kmsImageName).withName(dockerContainerName)
          .withEnv("GST_DEBUG=" + getDebugOptions(), "S3_ACCESS_BUCKET_NAME=" + s3BucketName,
              "S3_ACCESS_KEY_ID=" + s3AccessKeyId, "S3_SECRET_ACCESS_KEY=" + s3SecretAccessKey,
              "S3_HOSTNAME=" + s3Hostname, "KMS_STUN_IP=" + kmsStunIp,
              "KMS_STUN_PORT=" + kmsStunPort,
              "KURENTO_GENERATE_RTP_PTS_STATS=" + getKurentoGenerateRtpPtsStats())
          .withCmd("--gst-debug-no-color").withVolumes(new Volume("/var/run/docker.sock"));
    }

    if (dockerClient.isRunningInContainer()) {
      createContainerCmd.withVolumesFrom(new VolumesFrom(dockerClient.getContainerId()));
      if(isKmsElastest) {
          String elastestNetwork = dockerClient.getContainerFirstNetworkName();
          log.debug("Using Elastest network: {}", elastestNetwork);
          createContainerCmd.withNetworkMode(elastestNetwork);
      }
    } else {
      String testFilesPath = KurentoTest.getTestFilesDiskPath();
      Volume volume = new Volume(testFilesPath);
      String targetPath =
          Paths.get(KurentoTest.getDefaultOutputFolder().toURI()).toAbsolutePath().toString();
      Volume volumeTest = new Volume(targetPath);
      createContainerCmd.withVolumes(volume, volumeTest).withBinds(
          new Bind(testFilesPath, volume, AccessMode.ro),
          new Bind(targetPath, volumeTest, AccessMode.rw));
    }

    String kmsAddress = "";
    if (kmsDnat) {
      log.debug("Set network, for kms, as none");
      createContainerCmd.withNetworkMode("none");

      Map<String, String> labels = new HashMap<String, String>();
      labels.put("KurentoDnat", "true");
      labels.put("Transport", getProperty(TEST_KMS_TRANSPORT));

      kmsAddress = dockerClient.generateIpAddressForContainer();

      labels.put("IpAddress", kmsAddress);
      createContainerCmd.withLabels(labels);

      CreateContainerResponse kmsContainer = createContainerCmd.exec();
      dockerClient.getClient().startContainerCmd(kmsContainer.getId()).exec();
    } else {
      CreateContainerResponse kmsContainer = createContainerCmd.exec();
      dockerClient.getClient().startContainerCmd(kmsContainer.getId()).exec();
      kmsAddress =
          dockerClient.inspectContainer(dockerContainerName).getNetworkSettings()
              .getNetworks().values().iterator().next().getIpAddress();
    }

    setWsUri("ws://" + kmsAddress + ":8888/kurento");

    log.debug("Dockerized KMS started in URI {}", wsUri);
  }

  public String getKmsLogPath() {
    String kmsAutoStart = getProperty(kmsAutostartProp, kmsAutostartDefault);

    return kmsAutoStart.equals(AUTOSTART_FALSE_VALUE)
        ? getProperty(KMS_LOG_PATH_PROP, KMS_LOG_PATH_DEFAULT)
        : isKmsRemote ? remoteKmsSshConnection.getTmpFolder() + File.separator
            : workspace + File.separator;
  }

  private void createFileFromTemplate(Configuration cfg, Map<String, Object> data,
      String filename) {

    try {
      Template template = cfg.getTemplate(filename + ".ftl");
      File file = new File(workspace + File.separator + filename);
      Writer writer = new FileWriter(file);
      template.process(data, writer);
      writer.flush();
      writer.close();

      log.trace("Created file '{}'", file.getAbsolutePath());

    } catch (Exception e) {
      throw new KurentoException("Exception while creating file from template", e);
    }
  }

  public void retrieveLogs() throws IOException {
    File targetFolder = KurentoTest.getDefaultOutputFolder();
    String kmsLogsPath = getKmsLogPath();

    Path defaultOutput = Paths.get(targetFolder.toURI());
    if (!Files.exists(defaultOutput)) {
      Files.createDirectories(defaultOutput);
    }

    if (isKmsStarted) {
      kmsLogsPath += "logs/";
    }

    String testMethodName = KurentoTest.getSimpleTestName();

    if (isKmsDocker) {
      Docker.getSingleton().downloadLog(dockerContainerName, Paths
          .get(targetFolder.getAbsolutePath(), testMethodName + getDockerLogSuffix() + ".log"));
    }

    else if (isKmsRemote) {
      if (!remoteKmsSshConnection.isStarted()) {
        remoteKmsSshConnection.start();
      }
      log.debug("Copying KMS logs located on {} from remote host {} to {}", kmsLogsPath,
          remoteKmsSshConnection.getConnection(), targetFolder);

      List<String> remoteLogFiles = remoteKmsSshConnection.listFiles(kmsLogsPath, true, false);

      for (String remoteLogFile : remoteLogFiles) {

        String localLogFile = targetFolder + "/" + testMethodName + "-"
            + remoteLogFile.substring(remoteLogFile.lastIndexOf("/") + 1);

        remoteKmsSshConnection.getFile(localLogFile, remoteLogFile);

        KurentoTest.addLogFile(new File(localLogFile));
        log.debug("Log file: {}", localLogFile);
      }

    } else {
      File directory = new File(kmsLogsPath);
      if (directory.isDirectory()) {
        log.debug("Copying KMS logs from local path {} to {}", kmsLogsPath, targetFolder);

        Collection<File> logFiles = FileUtils.listFiles(directory, null, true);

        for (File logFile : logFiles) {
          File destFile = new File(targetFolder, testMethodName + "-" + logFile.getName());
          try {
            FileUtils.copyFile(logFile, destFile);

            KurentoTest.addLogFile(destFile);
            log.debug("Log file: {}", destFile);
          } catch (Throwable e) {
            log.warn("Exception copy KMS file {} {}", e.getClass(), e.getMessage());
          }
        }
      } else {
        log.warn("Path {} is not a directory", directory);
      }
    }
  }

  public void stopKms() {
    if (isKmsDocker || isKmsElastest) {
      Docker.getSingleton().stopContainer(dockerContainerName, false);
      if(isKmsElastest) {
          Docker.getSingleton().removeContainer(dockerContainerName);
      }

    } else {
      killKmsProcesses();

      if (isKmsRemote) {
        remoteKmsSshConnection.stop();
      }
    }

    isKmsStarted = false;
  }

  private void killKmsProcesses() {
    int numKmsProcesses = 0;
    // Max timeout waiting kms ending: 5 seconds
    long timeout = System.currentTimeMillis() + 5000;
    do {
      // If timeout, break the loop
      if (System.currentTimeMillis() > timeout) {
        break;
      }

      // Sending SIGTERM signal to KMS process
      kmsSigTerm();

      // Wait 100 msec to order kms termination
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      numKmsProcesses = countKmsProcesses();

    } while (numKmsProcesses > 0);

    if (numKmsProcesses > 0) {
      // If at this point there is still kms process (after trying to
      // kill it with SIGTERM during 5 seconds), we send the SIGKILL
      // signal to the process
      kmsSigKill();
    }
  }

  private void kmsSigTerm() {
    log.trace("Sending SIGTERM to KMS process");
    if (isKmsRemote) {
      String kmsPid = remoteKmsSshConnection.execAndWaitCommandNoBr("cat",
          remoteKmsSshConnection.getTmpFolder() + "/kms-pid");
      remoteKmsSshConnection.runAndWaitCommand("kill", kmsPid);
    } else {
      Shell.runAndWait("sh", "-c", "kill `cat " + workspace + File.separator + "kms-pid`");
    }
  }

  private void kmsSigKill() {
    log.trace("Sending SIGKILL to KMS process");
    if (isKmsRemote) {
      String kmsPid = remoteKmsSshConnection.execAndWaitCommandNoBr("cat",
          remoteKmsSshConnection.getTmpFolder() + "/kms-pid");
      remoteKmsSshConnection.runAndWaitCommand("sh", "-c", "kill -9 " + kmsPid);
    } else {
      Shell.runAndWait("sh", "-c", "kill -9 `cat " + workspace + File.separator + "kms-pid`");
    }
  }

  private int countKmsProcesses() {
    int result = 0;
    try {
      // This command counts number of process (given its PID, stored in
      // kms-pid file)

      if (isKmsRemote) {
        String kmsPid = remoteKmsSshConnection.execAndWaitCommandNoBr("cat",
            remoteKmsSshConnection.getTmpFolder() + "/kms-pid");
        result = Integer.parseInt(remoteKmsSshConnection
            .execAndWaitCommandNoBr("ps --pid " + kmsPid + " --no-headers | wc -l"));
      } else {
        String[] command = { "sh", "-c",
            "ps --pid `cat " + workspace + File.separator + "kms-pid` --no-headers | wc -l" };
        Process countKms = Runtime.getRuntime().exec(command);
        String stringFromStream =
            CharStreams.toString(new InputStreamReader(countKms.getInputStream(), "UTF-8"));
        result = Integer.parseInt(stringFromStream.trim());
      }
    } catch (IOException e) {
      log.warn("Exception counting KMS processes", e);
    }

    return result;
  }

  private void deleteFolderAndContent(Path folder) throws IOException {
    if (folder != null) {
      Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  public synchronized void setDockerContainerName(String containerName) {
    dockerContainerName = containerName;
    if (monitoredDockerContainerName == null) {
      monitoredDockerContainerName = dockerContainerName;
    }
  }

  private String getServerCommand() {
    return getProperty(KMS_SERVER_COMMAND_PROP, KMS_SERVER_COMMAND_DEFAULT);
  }

  private String getGstPlugins() {
    return getProperty(KSM_GST_PLUGINS_PROP, KMS_GST_PLUGINS_DEFAULT);
  }

  private String getDebugOptions() {
    return getProperty(KMS_SERVER_DEBUG_PROP, KMS_SERVER_DEBUG_DEFAULT);
  }

  private String getKurentoGenerateRtpPtsStats() {
    String path =
        getProperty(KMS_GENERATE_RTP_PTS_STATS_PROPERTY, KurentoTest.getDefaultOutputTestPath());
    log.debug("{} = {}", KMS_GENERATE_RTP_PTS_STATS_PROPERTY, path);
    return path;
  }

  public KurentoClient getKurentoClient() {
    if (kurentoClient == null && wsUri != null) {
      kurentoClient = createKurentoClient();
      kurentoClient.getServerManager()
          .addObjectCreatedListener(new EventListener<ObjectCreatedEvent>() {

            @Override
            public void onEvent(ObjectCreatedEvent event) {
              if (event instanceof MediaPipeline) {
                MediaPipeline mp = (MediaPipeline) event;
                mp.addErrorListener(new EventListener<ErrorEvent>() {

                  @Override
                  public void onEvent(ErrorEvent event) {
                    String msgException = "Error in KMS: " + event.getDescription() + "; Type: "
                        + event.getType() + "; Error Code: " + event.getErrorCode();
                    log.error(msgException);
                    throw new KurentoException(msgException);
                  }
                });
              }
            }
          });
    }
    return kurentoClient;
  }

  public KurentoClient createKurentoClient() {
    return KurentoClient.create(wsUri);
  }

  public void closeKurentoClient() {
    if (kurentoClient != null) {
      kurentoClient.destroy();
      kurentoClient = null;
    }
  }

  public String getWsUri() {
    return wsUri;
  }

  public void setWsUri(String wsUri) {
    if (wsUri != null) {
      System.setProperty(kmsWsUriExportProp, wsUri);
    }
    this.wsUri = wsUri;
  }

  public void setRegistrarUri(String registrarUri) {
    this.registrarUri = registrarUri;
  }

  public void setRegistrarLocalAddress(String registrarLocalAddress) {
    this.registrarLocalAddress = registrarLocalAddress;
  }

  public boolean isKmsStarted() {
    return isKmsStarted;
  }

  // returns the name of the first container
  public static String getMonitoredDockerContainerName() {
    return monitoredDockerContainerName;
  }

}
