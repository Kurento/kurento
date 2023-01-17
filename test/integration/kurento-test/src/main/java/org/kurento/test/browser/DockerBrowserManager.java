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

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_CHROME_IMAGE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_CHROME_IMAGE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_FIREFOX_IMAGE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.DOCKER_NODE_FIREFOX_IMAGE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_MAX_DRIVER_ERROR_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SELENIUM_MAX_DRIVER_ERROR_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_RECORD_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SELENIUM_RECORD_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_DNAT;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_DNAT_DEFAULT;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.kurento.test.base.KurentoTest;
import org.kurento.test.docker.Docker;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerBrowserManager {

  public static final int REMOTE_WEB_DRIVER_CREATION_MAX_RETRIES = 3;
  private static final int REMOTE_WEB_DRIVER_CREATION_TIMEOUT_S = 300;
  private static final int WAIT_URL_POLL_TIME_MS = 200;
  private static final int WAIT_URL_TIMEOUT_SEC = 10;

  private static Logger log = LoggerFactory.getLogger(DockerBrowserManager.class);

  private class DockerBrowser {

    private String id;
    private String browserContainerName;
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

      if (docker.isRunningInContainer()) {

        String containerName = docker.getContainerName();

        browserContainerName = containerName + "-" + browserContainerName + "-"
            + KurentoTest.getTestClassName() + "-" + new Random().nextInt(1000);
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
                record, browserContainerIp);
          } else {
            docker.startAndWaitNode(browserContainerName, type, browserContainerName, nodeImageId,
                record);
            browserContainerIp =
                docker.inspectContainer(browserContainerName).getNetworkSettings().getNetworks()
                    .values().iterator().next().getIpAddress();
          }

          String driverUrl = String.format("http://%s:4444/wd/hub", browserContainerIp);
          waitForUrl(driverUrl);
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

          docker.stopAndRemoveContainer(browserContainerName, record);

          browserContainerName += "r";

          capabilities.setCapability("applicationName", browserContainerName);

          numRetries++;
        }

      } while (driver == null);

      log.debug("RemoteWebDriver for browser {} created (Version={}, Capabilities={})", id,
          driver.getCapabilities().getVersion(), driver.getCapabilities());

    }

    public void waitForUrl(String url) {
      boolean urlAvailable = false;
      long timeoutMs =
          currentTimeMillis() + SECONDS.toMillis(WAIT_URL_TIMEOUT_SEC);
      do {
        try {
          if (currentTimeMillis() > timeoutMs) {
            throw new KurentoException("Timeout of " + WAIT_URL_TIMEOUT_SEC
                + " seconds waiting for URL " + url);
          }
          HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
          connection.setRequestMethod("HEAD");
          int responseCode = connection.getResponseCode();
          urlAvailable = responseCode >= 200 && responseCode < 500;
          if (!urlAvailable) {
            log.debug("URL {} is not still available (response {}) ... waiting {} ms", url,
                responseCode, WAIT_URL_POLL_TIME_MS);
            sleep(WAIT_URL_POLL_TIME_MS);
          }
        } catch (ConnectException e) {
          log.trace("{} is not yet available", url);
        } catch (IOException | InterruptedException e) {
          log.error("Exception waiting for url {}", url, e);
        }
      } while (!urlAvailable);
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
                "Timeout of " + timeoutSeconds + " seconds waiting to create a RemoteWebDriver",
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


    public RemoteWebDriver getRemoteWebDriver() {
      return driver;
    }

    public void close() {

      downloadLogsForContainer(browserContainerName, id);

      docker.stopAndRemoveContainer(browserContainerName, record);

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

  public RemoteWebDriver createDockerDriver(String id, DesiredCapabilities capabilities) {

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

    if (downloadLogsPath != null) {

      try {

        String logFileName = new File(KurentoTest.getDefaultOutputFile("-" + logName + "-container.log"))
            .getAbsolutePath();
        Path logFile = downloadLogsPath.resolve(logFileName);

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
