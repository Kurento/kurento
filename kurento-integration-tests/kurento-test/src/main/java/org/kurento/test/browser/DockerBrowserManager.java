/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.test.browser;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_CHROME_IMAGE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_CHROME_IMAGE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_FIREFOX_IMAGE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_FIREFOX_IMAGE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.DOCKER_VNCRECORDER_CONTAINER_NAME_DEFAULT;
import static org.kurento.test.config.TestConfiguration.DOCKER_VNCRECORDER_CONTAINER_NAME_PROPERTY;
import static org.kurento.test.config.TestConfiguration.DOCKER_VNCRECORDER_IMAGE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.DOCKER_VNCRECORDER_IMAGE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_MAX_DRIVER_ERROR_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SELENIUM_MAX_DRIVER_ERROR_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_RECORD_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SELENIUM_RECORD_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_DNAT;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_DNAT_DEFAULT;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.net.RemoteService;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.docker.Docker;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.command.CreateContainerCmd;

public class DockerBrowserManager {

  public static final int REMOTE_WEB_DRIVER_CREATION_MAX_RETRIES = 3;
  private static final int REMOTE_WEB_DRIVER_CREATION_TIMEOUT_S = 300;

  private static Logger log = LoggerFactory.getLogger(DockerBrowserManager.class);

  private class DockerBrowser {

    private String id;
    private String browserContainerName;
    private String vncrecorderContainerName;
    private String browserContainerIp;
    private DesiredCapabilities capabilities;
    private RemoteWebDriver driver;

    public DockerBrowser(String id, DesiredCapabilities capabilities) {
      this.id = id;
      this.capabilities = capabilities;

      calculateContainerNames();
    }

    private void calculateContainerNames() {

      browserContainerName = id;

      vncrecorderContainerName =
          browserContainerName + "-" + getProperty(DOCKER_VNCRECORDER_CONTAINER_NAME_PROPERTY,
              DOCKER_VNCRECORDER_CONTAINER_NAME_DEFAULT);

      if (docker.isRunningInContainer()) {

        String containerName = docker.getContainerName();

        browserContainerName = containerName + "-" + browserContainerName + "-"
            + KurentoTest.getTestClassName() + "-" + new Random().nextInt(1000);
        vncrecorderContainerName = containerName + "-" + vncrecorderContainerName;
      }
    }

    public void create() {

      String nodeImageId = calculateBrowserImageName(capabilities);

      BrowserType type = BrowserType.valueOf(capabilities.getBrowserName().toUpperCase());

      int numRetries = 0;

      do {
        try {
          Boolean kmsSelenium = false;
          if (getProperty(TEST_SELENIUM_DNAT) != null
              && getProperty(TEST_SELENIUM_DNAT, TEST_SELENIUM_DNAT_DEFAULT)) {
            kmsSelenium = true;
          }

          if (kmsSelenium) {
            browserContainerIp = docker.generateIpAddressForContainer();
            docker.startAndWaitNode(browserContainerName, type, browserContainerName, nodeImageId,
                browserContainerIp);
          } else {
            docker.startAndWaitNode(browserContainerName, type, browserContainerName, nodeImageId);
            browserContainerIp =
                docker.inspectContainer(browserContainerName).getNetworkSettings().getIpAddress();
          }

          // TODO: use a free port instead
          String driverUrl = String.format("http://%s:4444/wd/hub", browserContainerIp);
          createAndWaitRemoteDriver(driverUrl, capabilities);

        } catch (TimeoutException e) {

          if (numRetries == REMOTE_WEB_DRIVER_CREATION_MAX_RETRIES) {
            throw new KurentoException("Timeout of "
                + REMOTE_WEB_DRIVER_CREATION_TIMEOUT_S * REMOTE_WEB_DRIVER_CREATION_MAX_RETRIES
                + " seconds trying to create a RemoteWebDriver after"
                + REMOTE_WEB_DRIVER_CREATION_MAX_RETRIES + "retries");
          }

          log.warn("Timeout of {} seconds creating RemoteWebDriver. Retrying {}...",
              REMOTE_WEB_DRIVER_CREATION_TIMEOUT_S, numRetries);

          docker.stopAndRemoveContainer(browserContainerName);

          browserContainerName += "r";

          capabilities.setCapability("applicationName", browserContainerName);

          numRetries++;
        }

      } while (driver == null);

      log.debug("RemoteWebDriver for browser {} created (Version={}, Capabilities={})", id,
          driver.getCapabilities().getVersion(), driver.getCapabilities());

      if (record) {
        createVncRecorderContainer();
      }
    }

    private void createAndWaitRemoteDriver(final String driverUrl,
        final DesiredCapabilities capabilities) throws TimeoutException {

      log.debug("Creating remote driver for browser {} in hub {}", id, driverUrl);

      int timeoutSeconds =
          getProperty(SELENIUM_MAX_DRIVER_ERROR_PROPERTY, SELENIUM_MAX_DRIVER_ERROR_DEFAULT);

      long timeoutMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);

      do {

        Future<RemoteWebDriver> driverFuture = null;

        try {

          driverFuture = exec.submit(new Callable<RemoteWebDriver>() {
            @Override
            public RemoteWebDriver call() throws Exception {
              return new RemoteWebDriver(new URL(driverUrl), capabilities);
            }
          });

          RemoteWebDriver remoteDriver;
          remoteDriver = driverFuture.get(REMOTE_WEB_DRIVER_CREATION_TIMEOUT_S, TimeUnit.SECONDS);

          SessionId sessionId = remoteDriver.getSessionId();

          log.debug("Created selenium session {} for browser {}", sessionId, id);

          driver = remoteDriver;

        } catch (TimeoutException e) {

          driverFuture.cancel(true);
          throw e;

        } catch (InterruptedException e) {

          throw new RuntimeException("Interrupted exception waiting for RemoteWebDriver", e);

        } catch (ExecutionException e) {

          log.warn("Exception creating RemoveWebDriver", e);

          // Check timeout
          if (System.currentTimeMillis() > timeoutMs) {
            throw new KurentoException(
                "Timeout of " + timeoutMs + " millis waiting to create a RemoteWebDriver",
                e.getCause());
          }

          log.debug("Exception creating RemoteWebDriver for browser \"{}\". Retrying...", id,
              e.getCause());

          // Poll time
          try {
            Thread.sleep(500);
          } catch (InterruptedException t) {
            Thread.currentThread().interrupt();
            return;
          }

        }

      } while (driver == null);
    }

    private void createVncRecorderContainer() {

      try {

        try {
          RemoteService.waitForReady(browserContainerIp, 5900, 10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          throw new RuntimeException("Timeout when connecting to browser VNC");
        }

        String vncrecordImageId =
            getProperty(DOCKER_VNCRECORDER_IMAGE_PROPERTY, DOCKER_VNCRECORDER_IMAGE_DEFAULT);

        if (docker.existsContainer(vncrecorderContainerName)) {
          throw new KurentoException(
              "Vncrecorder container '" + vncrecorderContainerName + "' already exists");
        }

        String secretFile = createSecretFile();

        docker.pullImageIfNecessary(vncrecordImageId, false);

        String videoFile = Paths.get(KurentoTest.getDefaultOutputFile("-" + id + "-record.flv"))
            .toAbsolutePath().toString();

        log.debug("Creating container {} for recording video from browser {} in file {}",
            vncrecorderContainerName, browserContainerName, videoFile);

        CreateContainerCmd createContainerCmd = docker.getClient()
            .createContainerCmd(vncrecordImageId).withName(vncrecorderContainerName)
            .withCmd("-o", videoFile, "-P", secretFile, browserContainerIp, "5900");

        docker.mountDefaultFolders(createContainerCmd);

        createContainerCmd.exec();

        docker.startContainer(vncrecorderContainerName);

        log.debug("Container {} started...", vncrecorderContainerName);

      } catch (Exception e) {
        log.warn("Exception creating vncRecorder container");
      }
    }

    public RemoteWebDriver getRemoteWebDriver() {
      return driver;
    }

    public void close() {

      downloadLogsForContainer(browserContainerName, id);

      downloadLogsForContainer(vncrecorderContainerName, id + "-recorder");

      docker.stopAndRemoveContainers(vncrecorderContainerName, browserContainerName);

    }
  }

  private Docker docker = Docker.getSingleton();

  private ExecutorService exec = Executors.newFixedThreadPool(10);

  private ConcurrentMap<String, DockerBrowser> browsers = new ConcurrentHashMap<>();

  private boolean record;

  private Path downloadLogsPath;

  public DockerBrowserManager() {
    docker = Docker.getSingleton();
    record = getProperty(SELENIUM_RECORD_PROPERTY, SELENIUM_RECORD_DEFAULT);
  }

  public void setDownloadLogsPath(Path path) {
    this.downloadLogsPath = path;
  }

  public RemoteWebDriver createDockerDriver(String id, DesiredCapabilities capabilities)
      throws MalformedURLException {

    DockerBrowser browser = new DockerBrowser(id, capabilities);

    if (browsers.putIfAbsent(id, browser) != null) {
      throw new KurentoException("Browser with id " + id + " already exists");
    }

    browser.create();

    return browser.getRemoteWebDriver();
  }

  public void closeDriver(String id) {

    DockerBrowser browser = browsers.remove(id);

    if (browser == null) {
      log.warn("Browser " + id + " does not exists");
      return;
    }

    browser.close();

  }

  private String createSecretFile() throws IOException {
    Path secretFile = Paths.get(KurentoTest.getTestDir() + "vnc-passwd");

    try (BufferedWriter bw = Files.newBufferedWriter(secretFile, StandardCharsets.UTF_8)) {
      bw.write("secret");
    }

    return secretFile.toAbsolutePath().toString();
  }

  private String calculateBrowserImageName(DesiredCapabilities capabilities) {

    String browserName = capabilities.getBrowserName();

    if (browserName.equals(DesiredCapabilities.chrome().getBrowserName())) {

      // Chrome
      return getProperty(DOCKER_NODE_CHROME_IMAGE_PROPERTY, DOCKER_NODE_CHROME_IMAGE_DEFAULT);

    } else if (browserName.equals(DesiredCapabilities.firefox().getBrowserName())) {

      // Firefox
      return getProperty(DOCKER_NODE_FIREFOX_IMAGE_PROPERTY, DOCKER_NODE_FIREFOX_IMAGE_DEFAULT);

    } else {
      throw new RuntimeException(
          "Browser " + browserName + " is not supported currently for Docker scope");
    }
  }

  private void downloadLogsForContainer(String container, String logName) {

    if (docker.existsContainer(container) && downloadLogsPath != null) {

      try {

        Path logFile = downloadLogsPath.resolve(logName + ".log");

        if (Files.exists(logFile.getParent())) {
          Files.createDirectories(logFile.getParent());
        }

        log.debug("Downloading log for container {} in file {}", container,
            logFile.toAbsolutePath());

        docker.downloadLog(container, logFile);

      } catch (IOException e) {
        log.warn("Exception writing logs for container {}", container, e);
      }
    }
  }

}
