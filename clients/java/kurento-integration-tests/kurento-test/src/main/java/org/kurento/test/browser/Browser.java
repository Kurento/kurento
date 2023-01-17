/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
import static org.kurento.test.config.TestConfiguration.SAUCELAB_COMMAND_TIMEOUT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SAUCELAB_COMMAND_TIMEOUT_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SAUCELAB_IDLE_TIMEOUT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SAUCELAB_IDLE_TIMEOUT_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SAUCELAB_KEY_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SAUCELAB_MAX_DURATION_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SAUCELAB_MAX_DURATION_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SAUCELAB_USER_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_MAX_DRIVER_ERROR_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SELENIUM_MAX_DRIVER_ERROR_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_REMOTEWEBDRIVER_TIME_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SELENIUM_REMOTEWEBDRIVER_TIME_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_REMOTE_HUB_URL_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_SCOPE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.SELENIUM_VERSION;
import static org.kurento.test.config.TestConfiguration.TEST_HOST_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_NODE_LOGIN_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_NODE_PASSWD_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_NODE_PEM_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PATH_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PATH_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PORT_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PROTOCOL_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PROTOCOL_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PUBLIC_IP_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PUBLIC_IP_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PUBLIC_PORT_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_SCREEN_SHARE_TITLE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_SCREEN_SHARE_TITLE_DEFAULT_WIN;
import static org.kurento.test.config.TestConfiguration.TEST_SCREEN_SHARE_TITLE_PROPERTY;
import static org.openqa.selenium.firefox.FirefoxOptions.FIREFOX_OPTIONS;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.config.AudioChannel;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestConfiguration;
import org.kurento.test.docker.Docker;
import org.kurento.test.grid.GridHandler;
import org.kurento.test.grid.GridNode;
import org.kurento.test.services.WebServerService;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Wrapper of Selenium Webdriver for testing Kurento applications.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.1.0
 * @see <a href="http://www.seleniumhq.org/">Selenium</a>
 */
public class Browser implements Closeable {

  public static Logger log = LoggerFactory.getLogger(Browser.class);

  private WebDriver driver;
  private String jobId;
  private Builder builder;
  private BrowserType browserType;
  private BrowserScope scope;
  private String browserVersion;
  private Platform platform;
  private String video;
  private String audio;
  private int recordAudio;
  private int audioSampleRate;
  private AudioChannel audioChannel;
  private int timeout;
  private boolean usePhysicalCam;
  private boolean enableScreenCapture;
  private String name;
  private String id;
  private double colorDistance;
  private int thresholdTime;
  private int numInstances;
  private int browserPerInstance;
  private Protocol protocol;
  private String node;
  private String host;
  private int serverPort;
  private WebPageType webPageType;
  private String webPagePath;
  private String login;
  private String passwd;
  private String pem;
  private boolean avoidProxy;
  private String parentTunnel;
  private List<Map<String, String>> extensions;
  private URI url;

  private static volatile DockerBrowserManager dockerManager;
  private static Docker docker = Docker.getSingleton();

  public Browser(Builder builder) {

    this.builder = builder;
    this.scope = builder.scope;
    this.video = builder.video;
    this.audio = builder.audio;
    this.serverPort =
        getProperty(TEST_PORT_PROPERTY, getProperty(TEST_PUBLIC_PORT_PROPERTY, builder.serverPort));
    this.webPageType = builder.webPageType;
    this.browserType = builder.browserType;
    this.usePhysicalCam = builder.usePhysicalCam;
    this.enableScreenCapture = builder.enableScreenCapture;
    this.recordAudio = builder.recordAudio;
    this.audioSampleRate = builder.audioSampleRate;
    this.audioChannel = builder.audioChannel;
    this.browserVersion = getProperty("test.browser.version", builder.browserVersion);
    this.platform = builder.platform;
    this.timeout = builder.timeout;
    this.colorDistance = builder.colorDistance;
    this.thresholdTime = builder.thresholdTime;
    this.node = builder.node;
    this.protocol = builder.protocol;
    this.numInstances = builder.numInstances;
    this.browserPerInstance = builder.browserPerInstance;
    this.login = builder.login;
    this.passwd = builder.passwd;
    this.pem = builder.pem;
    this.host = builder.host;
    this.avoidProxy = builder.avoidProxy;
    this.parentTunnel = builder.parentTunnel;
    this.extensions = builder.extensions;
    this.url = builder.url;
    this.webPagePath = builder.webPagePath;
  }

  public void init() {

    log.debug("Starting browser {}", getId());

    Class<? extends WebDriver> driverClass = browserType.getDriverClass();

    try {
      DesiredCapabilities capabilities = new DesiredCapabilities();

      LoggingPreferences logs = new LoggingPreferences();
      logs.enable(LogType.BROWSER, Level.INFO);
      capabilities.setCapability(CapabilityType.LOGGING_PREFS, logs);

      if (driverClass.equals(FirefoxDriver.class)) {

        createFirefoxBrowser(capabilities);

      } else if (driverClass.equals(ChromeDriver.class)) {

        createChromeBrowser(capabilities);

      } else if (driverClass.equals(InternetExplorerDriver.class)) {

        if (scope == BrowserScope.SAUCELABS) {
          capabilities.setBrowserName(DesiredCapabilities.internetExplorer().getBrowserName());
          capabilities.setCapability("ignoreProtectedModeSettings", true);
          createSaucelabsDriver(capabilities);
        }

      } else if (driverClass.equals(SafariDriver.class)) {

        if (scope == BrowserScope.SAUCELABS) {
          capabilities.setBrowserName(DesiredCapabilities.safari().getBrowserName());
          createSaucelabsDriver(capabilities);
        }
      }

      // Timeouts
      changeTimeout(timeout);

      log.debug("Browser {} started", getId());

      calculateUrl();

      log.debug("Browser {} loading url {}", getId(), url);

      driver.get(url.toString());
      driver.navigate().refresh();

      log.debug("Browser {} initialized", getId());

    } catch (MalformedURLException e) {
      log.error("MalformedURLException in Browser.init", e);
    }

  }

  private void calculateUrl() {
    if (url == null) {
      if (protocol == Protocol.FILE) {
        String webPage = webPagePath != null ? webPagePath : webPageType.toString();
        File webPageFile =
            new File(this.getClass().getClassLoader().getResource("static" + webPage).getFile());
        try {
          url = new URI(protocol.toString() + webPageFile.getAbsolutePath());
        } catch (URISyntaxException e) {
          throw new KurentoException("Exception generating URI from " + protocol + " and "
              + webPageFile.getAbsolutePath());
        }
      } else {

        String hostName;
        log.debug("BrowserScope is {}", scope);
        if (scope == BrowserScope.DOCKER || scope == BrowserScope.ELASTEST) {
          if (docker.isRunningInContainer()) {
            hostName = docker.getContainerIpAddress();
          } else {
            hostName = docker.getHostIpForContainers();
          }

        } else {
          hostName = host != null ? host : node;
        }

        log.debug("Protocol: {}, Hostname: {}, Port: {}, Web page type: {}", protocol, hostName,
            serverPort, webPageType);

        try {
          url = new URI(protocol.toString(), null, hostName, serverPort, webPageType.toString(),
              null, null);
        } catch (URISyntaxException e) {
          throw new KurentoException("Exception generating URI from " + protocol + ", " + hostName
              + ", server port " + serverPort + " and webpage type " + webPageType);
        }
      }
    }
  }

  private void createChromeBrowser(DesiredCapabilities capabilities) throws MalformedURLException {

    if (scope == BrowserScope.LOCAL) {
      // Management of chromedriver
      WebDriverManager.chromedriver().setup();
    }

    // Chrome options
    ChromeOptions options = new ChromeOptions();

    // Chrome extensions
    if (extensions != null && !extensions.isEmpty()) {

      for (Map<String, String> extension : extensions) {
        InputStream is = getExtensionAsInputStream(extension.values().iterator().next());
        if (is != null) {
          try {
            File crx = File.createTempFile(extension.keySet().iterator().next(), ".crx");
            FileUtils.copyInputStreamToFile(is, crx);
            options.addExtensions(crx);
          } catch (Throwable t) {
            log.error("Error loading Chrome extension {} ({} : {})", extension, t.getClass(),
                t.getMessage());
          }
        }
      }
    }

    if (enableScreenCapture) {
      // This flag enables the screen sharing
      options.addArguments("--enable-usermedia-screen-capturing");

      String windowTitle = TEST_SCREEN_SHARE_TITLE_DEFAULT;
      if (platform != null
          && (platform == Platform.WINDOWS || platform == Platform.XP || platform == Platform.VISTA
              || platform == Platform.WIN8 || platform == Platform.WIN8_1)) {

        windowTitle = TEST_SCREEN_SHARE_TITLE_DEFAULT_WIN;
      }
      options.addArguments("--auto-select-desktop-capture-source="
          + getProperty(TEST_SCREEN_SHARE_TITLE_PROPERTY, windowTitle));

    } else {
      // This flag avoids grant the camera
      options.addArguments("--use-fake-ui-for-media-stream");
    }

    // This flag avoids warning in Chrome. See:
    // https://code.google.com/p/chromedriver/issues/detail?id=799
    options.addArguments("--test-type");

    // To avoid problems with DevToolsActivePort
    options.addArguments("--no-sandbox");

    if (protocol == Protocol.FILE) {
      // This flag allows reading local files in video tags
      options.addArguments("--allow-file-access-from-files");
    }

    if (!usePhysicalCam) {
      // This flag makes using a synthetic video (green with
      // spinner) in WebRTC. Or it is needed to combine with
      // use-file-for-fake-video-capture to use a file faking the
      // cam
      options.addArguments("--use-fake-device-for-media-stream=fps=30");

      if (video != null && (isLocal() || isDocker() || isElastest())) {

        if (!Files.exists(Paths.get(video))) {
          throw new RuntimeException("Trying to create a browser using video file " + video
              + ", but this file doesn't exist.");
        }

        File videoFile = new File(video);
        log.debug("Using video {} in browser {} (exists {}, {} bytes, can read {})", video, id,
            videoFile.exists(), videoFile.length(), videoFile.canRead());
        options.addArguments("--use-file-for-fake-video-capture=" + video);
      }
    }

    capabilities.setCapability(ChromeOptions.CAPABILITY, options);
    capabilities.setBrowserName(DesiredCapabilities.chrome().getBrowserName());

    createDriver(capabilities, options);
  }

  private void createFirefoxBrowser(DesiredCapabilities capabilities) throws MalformedURLException {
    if (scope == BrowserScope.LOCAL) {
      WebDriverManager.firefoxdriver().setup();
    }
    FirefoxOptions firefoxOptions = new FirefoxOptions();
    // This flag avoids granting the access to the camera
    firefoxOptions.addPreference("media.navigator.permission.disabled", true);

    // This flag force to use fake user media (synthetic video of multiple color)
    firefoxOptions.addPreference("media.navigator.streams.fake", true);

    // This allows to load pages with self-signed certificates
    capabilities.setCapability("acceptInsecureCerts", true);
    firefoxOptions.setAcceptInsecureCerts(true);

    capabilities.setCapability(FIREFOX_OPTIONS, firefoxOptions);
    capabilities.setBrowserName(firefoxOptions.getBrowserName());

    // Firefox extensions
    if (extensions != null && !extensions.isEmpty()) {
      for (Map<String, String> extension : extensions) {
        InputStream is = getExtensionAsInputStream(extension.values().iterator().next());
        if (is != null) {
          try {
            File xpi = File.createTempFile(extension.keySet().iterator().next(), ".xpi");
            FileUtils.copyInputStreamToFile(is, xpi);
            firefoxOptions.getProfile().addExtension(xpi);
          } catch (Throwable t) {
            log.error("Error loading Firefox extension {} ({} : {})", extension, t.getClass(),
                t.getMessage());
          }
        }
      }
    }

    createDriver(capabilities, firefoxOptions);
  }

  private void createDriver(DesiredCapabilities capabilities, Object options)
      throws MalformedURLException {

    log.debug("Creating driver in scope {} for browser '{}'", scope, id);

    if (scope == BrowserScope.SAUCELABS) {
      createSaucelabsDriver(capabilities);
    } else if (scope == BrowserScope.REMOTE) {
      createRemoteDriver(capabilities);
    } else if (scope == BrowserScope.DOCKER) {
      driver = getDockerManager().createDockerDriver(id, capabilities);
    } else if (scope == BrowserScope.ELASTEST) {
      String eusURL = System.getenv("ET_EUS_API");
      try {
        String testNameCap = KurentoTest.getTestMethodName()
            + (id != null && !id.isEmpty() ? "_" + id : "");

        capabilities.setCapability("testName", testNameCap);

        if (browserVersion != null && !"".equals(browserVersion)) {
          capabilities.setCapability("version", browserVersion);
        }

        driver = new RemoteWebDriver(new URL(eusURL), capabilities);
      } catch (MalformedURLException e) {
        String errMessage = "ElasTest EUS API URL is Null";
        log.error(errMessage);
        throw new KurentoException(errMessage);
      }
    } else {
      driver = newWebDriver(options);
    }
  }

  private DockerBrowserManager getDockerManager() {
    if (dockerManager == null) {
      synchronized (Browser.class) {
        if (dockerManager == null) {
          dockerManager = new DockerBrowserManager();
        }
      }
    }

    return dockerManager;
  }

  public static WebDriver newWebDriver(Object options) {

    WebDriver driver = null;

    int numDriverTries = 0;
    final int maxDriverError =
        getProperty(SELENIUM_MAX_DRIVER_ERROR_PROPERTY, SELENIUM_MAX_DRIVER_ERROR_DEFAULT);
    final String errMessage = "Exception creating webdriver for chrome";
    do {
      try {

        if (options instanceof ChromeOptions) {
          driver = new ChromeDriver((ChromeOptions) options);
        } else if (options instanceof FirefoxOptions) {
          driver = new FirefoxDriver((FirefoxOptions) options);
        }

      } catch (Throwable t) {
        driver = null;
        log.warn(errMessage + " #" + numDriverTries, t);
      } finally {
        numDriverTries++;
        if (numDriverTries > maxDriverError) {
          throw new KurentoException(errMessage + " (" + maxDriverError + " times)");
        }
      }
    } while (driver == null);

    return driver;
  }

  public void reload() {
    if (url != null) {
      this.driver.get(url.toString());
    }
  }

  public InputStream getExtensionAsInputStream(String extension) {
    InputStream is = null;

    try {
      log.debug("Trying to locate extension in the classpath ({}) ...", extension);
      is = ClassLoader.getSystemResourceAsStream(extension);
      if (is.available() < 0) {
        log.warn("Extension {} is not located in the classpath", extension);
        is = null;
      } else {
        log.debug("Success. Loading extension {} from classpath", extension);
      }
    } catch (Throwable t) {
      log.warn("Exception reading extension {} in the classpath ({} : {})", extension, t.getClass(),
          t.getMessage());
      is = null;
    }
    if (is == null) {
      try {
        log.debug("Trying to locate extension as URL ({}) ...", extension);
        URL url = new URL(extension);
        is = url.openStream();
        log.debug("Success. Loading extension {} from URL", extension);
      } catch (Throwable t) {
        log.warn("Exception reading extension {} as URL ({} : {})", extension, t.getClass(),
            t.getMessage());
      }
    }
    if (is == null) {
      throw new RuntimeException(
          extension + " is not a valid extension (it is not located in project"
              + " classpath neither is a valid URL)");
    }
    return is;
  }

  public void changeTimeout(int timeoutSeconds) {
    driver.manage().timeouts().implicitlyWait(timeoutSeconds, TimeUnit.SECONDS);
    driver.manage().timeouts().setScriptTimeout(timeoutSeconds, TimeUnit.SECONDS);
  }

  public void createSaucelabsDriver(DesiredCapabilities capabilities) throws MalformedURLException {
    assertPublicIpNotNull();
    String sauceLabsUser = getProperty(SAUCELAB_USER_PROPERTY);
    String sauceLabsKey = getProperty(SAUCELAB_KEY_PROPERTY);

    if (sauceLabsUser == null || sauceLabsKey == null) {
      throw new RuntimeException("Invalid Saucelabs credentials: " + SAUCELAB_USER_PROPERTY + "="
          + sauceLabsUser + " " + SAUCELAB_KEY_PROPERTY + "=" + sauceLabsKey);
    }

    capabilities.setCapability("version", browserVersion);
    capabilities.setCapability("platform", platform);

    String seleniumVersion = getProperty(SELENIUM_VERSION);
    if (seleniumVersion != null) {
      capabilities.setCapability("seleniumVersion", seleniumVersion);
    }
    if (parentTunnel != null) {
      capabilities.setCapability("parent-tunnel", parentTunnel);
    }
    if (avoidProxy) {
      capabilities.setCapability("avoid-proxy", avoidProxy);
    }

    int idleTimeout = getProperty(SAUCELAB_IDLE_TIMEOUT_PROPERTY, SAUCELAB_IDLE_TIMEOUT_DEFAULT);
    int commandTimeout =
        getProperty(SAUCELAB_COMMAND_TIMEOUT_PROPERTY, SAUCELAB_COMMAND_TIMEOUT_DEFAULT);
    int maxDuration = getProperty(SAUCELAB_MAX_DURATION_PROPERTY, SAUCELAB_MAX_DURATION_DEFAULT);
    capabilities.setCapability("idleTimeout", idleTimeout);
    capabilities.setCapability("commandTimeout", commandTimeout);
    capabilities.setCapability("maxDuration", maxDuration);

    if (name != null) {
      capabilities.setCapability("name", name);
    }

    driver = new RemoteWebDriver(
        new URL(
            "http://" + sauceLabsUser + ":" + sauceLabsKey + "@ondemand.saucelabs.com:80/wd/hub"),
        capabilities);

    jobId = ((RemoteWebDriver) driver).getSessionId().toString();
    log.debug("%%%%%%%%%%%%% Saucelabs URL job for {} ({} {} in {}) %%%%%%%%%%%%%", id, browserType,
        browserVersion, platform);
    log.debug("https://saucelabs.com/tests/{}", jobId);
  }

  public void createRemoteDriver(final DesiredCapabilities capabilities)
      throws MalformedURLException {

    assertPublicIpNotNull();

    String remoteHubUrl = getProperty(SELENIUM_REMOTE_HUB_URL_PROPERTY);
    GridNode gridNode = null;

    if (remoteHubUrl == null) {

      log.debug("Creating remote webdriver for {}", id);
      if (!GridHandler.getInstance().containsSimilarBrowserKey(id)) {

        if (login != null) {
          System.setProperty(TEST_NODE_LOGIN_PROPERTY, login);
        }
        if (passwd != null) {
          System.setProperty(TEST_NODE_PASSWD_PROPERTY, passwd);
        }
        if (pem != null) {
          System.setProperty(TEST_NODE_PEM_PROPERTY, pem);
        }

        // Filtering valid nodes (just the first time will be effective)
        GridHandler.getInstance().filterValidNodes();

        if (!node.equals(host) && login != null && !login.isEmpty()
            && (passwd != null && !passwd.isEmpty() || pem != null && !pem.isEmpty())) {
          gridNode = new GridNode(node, browserType, browserPerInstance, login, passwd, pem);
          GridHandler.getInstance().addNode(id, gridNode);
        } else {
          gridNode =
              GridHandler.getInstance().getRandomNodeFromList(id, browserType, browserPerInstance);
        }

        // Start Hub (just the first time will be effective)
        GridHandler.getInstance().startHub();

        // Start node
        GridHandler.getInstance().startNode(gridNode);

        // Copy video (if necessary)
        if (video != null && browserType == BrowserType.CHROME) {
          GridHandler.getInstance().copyRemoteVideo(gridNode, video);
        }

      } else {
        // Wait for node
        boolean started = false;
        do {
          gridNode = GridHandler.getInstance().getNode(id);
          if (gridNode != null) {
            started = gridNode.isStarted();
          }
          if (!started) {
            log.debug("Node {} is not started ... waiting 1 second", id);
            waitSeconds(1);
          }
        } while (!started);
      }

      // At this moment we are able to use the argument for remote video
      if (video != null && browserType == BrowserType.CHROME) {
        ChromeOptions options =
            (ChromeOptions) capabilities.getCapability(ChromeOptions.CAPABILITY);
        options.addArguments("--use-file-for-fake-video-capture="
            + GridHandler.getInstance().getFirstNode(id).getRemoteVideo(video));
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
      }

      if (browserVersion != null && !"".equals(browserVersion)) {
        capabilities.setCapability("version", browserVersion);
      }

      final int hubPort = GridHandler.getInstance().getHubPort();
      final String hubHost = GridHandler.getInstance().getHubHost();

      log.debug("Creating remote webdriver of {} ({})", id, gridNode.getHost());

      remoteHubUrl = "http://" + hubHost + ":" + hubPort + "/wd/hub";
    }

    final String remoteHub = remoteHubUrl;
    Thread t = new Thread() {
      @Override
      public void run() {
        boolean exception = false;
        do {
          try {
            driver = new RemoteWebDriver(new URL(remoteHub), capabilities);
            exception = false;
          } catch (MalformedURLException | WebDriverException e) {
            log.error("Exception {} creating RemoteWebDriver ... retrying in 1 second",
                e.getClass());
            waitSeconds(1);
            exception = true;
          }
        } while (exception);
      }
    };
    t.start();

    int timeout =
        getProperty(SELENIUM_REMOTEWEBDRIVER_TIME_PROPERTY, SELENIUM_REMOTEWEBDRIVER_TIME_DEFAULT);
    String nodeMsg = gridNode != null ? " (" + gridNode.getHost() + ")" : "";

    for (int i = 0; i < timeout; i++) {
      if (t.isAlive()) {
        log.debug("Waiting for RemoteWebDriver {}{}", id, nodeMsg);
      } else {
        log.debug("Remote webdriver of {}{} created", id, nodeMsg);
        return;
      }
      waitSeconds(1);
    }

    String exceptionMessage =
        "Remote webdriver of " + id + nodeMsg + " not created in " + timeout + "seconds";
    log.error(">>>>>>>>>> " + exceptionMessage);
    throw new RuntimeException(exceptionMessage);
  }

  private void waitSeconds(long timeInSeconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(timeInSeconds));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void assertPublicIpNotNull() {
    if (host == null) {
      throw new RuntimeException("Public IP must be available to run remote test. "
          + "You can do it by adding the paramter -D" + TEST_HOST_PROPERTY
          + "=<public_ip> or with key 'host' in " + "the JSON configuration file.");
    }
  }

  public void injectKurentoTestJs() throws IOException {
    if (this.getBrowserType() != BrowserType.IEXPLORER) {

      String kurentoTestJsContent = "";
      String kurentoTestPath = "static/lib/kurento-test.min.js";
      try {
        File pageFile =
            new File(this.getClass().getClassLoader().getResource(kurentoTestPath).getFile());
        kurentoTestJsContent = new String(Files.readAllBytes(pageFile.toPath()));
      } catch (NoSuchFileException nsfe) {
        InputStream inputStream =
            this.getClass().getClassLoader().getResourceAsStream(kurentoTestPath);
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, Charset.defaultCharset());
        kurentoTestJsContent = writer.toString();
      }

      String kurentoTestJs = "var kurentoScript=window.document.createElement('script');";
      kurentoTestJs += "kurentoScript.type='text/javascript';";
      kurentoTestJs += "kurentoScript.text='" + kurentoTestJsContent + "';";
      kurentoTestJs += "window.document.head.appendChild(kurentoScript);";
      kurentoTestJs += "return true;";
      this.executeScript(kurentoTestJs);

      // Disable RecordRTC.js injection
      // String recordingJs = "var recScript=window.document.createElement('script');";
      // recordingJs += "recScript.type='text/javascript';";
      // recordingJs += "recScript.src='https://cdn.webrtc-experiment.com/RecordRTC.js';";
      // recordingJs += "window.document.head.appendChild(recScript);";
      // recordingJs += "return true;";
      // this.executeScript(recordingJs);
    }
  }

  public int getRecordAudio() {
    return recordAudio;
  }

  public int getAudioSampleRate() {
    return audioSampleRate;
  }

  public AudioChannel getAudioChannel() {
    return audioChannel;
  }

  public String getAudio() {
    return audio;
  }

  public int getTimeout() {
    return timeout;
  }

  public long getTimeoutMs() {
    return TimeUnit.SECONDS.toMillis(timeout);
  }

  public WebDriver getWebDriver() {
    return driver;
  }

  public JavascriptExecutor getJs() {
    return (JavascriptExecutor) driver;
  }

  public Object executeScriptAndWaitOutput(final String command) {
    WebDriverWait wait = new WebDriverWait(driver, timeout);
    wait.withMessage("Timeout executing script: " + command);

    final Object[] out = new Object[1];
    wait.until(new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver d) {
        try {
          out[0] = executeScript(command);
        } catch (WebDriverException we) {
          log.warn("Exception executing script", we);
          out[0] = null;
        }
        return out[0] != null;
      }
    });
    return out[0];
  }

  public Object executeScript(final String command) {
    return ((JavascriptExecutor) driver).executeScript(command);
  }

  public double getColorDistance() {
    return colorDistance;
  }

  public int getThresholdTime() {
    return thresholdTime;
  }

  public void setThresholdTime(int thresholdTime) {
    this.thresholdTime = thresholdTime;
  }

  public boolean isLocal() {
    return BrowserScope.LOCAL.equals(this.scope);
  }

  public boolean isRemote() {
    return BrowserScope.REMOTE.equals(this.scope);
  }

  public boolean isSauceLabs() {
    return BrowserScope.SAUCELABS.equals(this.scope);
  }

  public boolean isDocker() {
    return BrowserScope.DOCKER.equals(this.scope);
  }

  public boolean isElastest() {
    return BrowserScope.ELASTEST.equals(this.scope);
  }

  public BrowserType getBrowserType() {
    return browserType;
  }

  public BrowserScope getScope() {
    return scope;
  }

  public String getBrowserVersion() {
    return browserVersion;
  }

  public Platform getPlatform() {
    return platform;
  }

  public String getVideo() {
    return video;
  }

  public int getServerPort() {
    return serverPort;
  }

  public WebPageType getWebPageType() {
    return webPageType;
  }

  public boolean isUsePhysicalCam() {
    return usePhysicalCam;
  }

  public boolean isEnableScreenCapture() {
    return enableScreenCapture;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getNode() {
    return node;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getNumInstances() {
    return numInstances;
  }

  public Builder getBuilder() {
    return builder;
  }

  public String getLogin() {
    return login;
  }

  public String getPasswd() {
    return passwd;
  }

  public String getPem() {
    return pem;
  }

  public int getBrowserPerInstance() {
    return browserPerInstance;
  }

  public String getHost() {
    return host;
  }

  public void setTimeout(int timeoutSeconds) {
    this.timeout = timeoutSeconds;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public URL getUrl() {
    URL url = null;
    try {
      if (this.url != null) {
        url = this.url.toURL();
      } else {
        String ip = this.getHost();
        int port = this.getServerPort();
        String protocol = this.getProtocol().toString();
        String path = this.getWebPageType().toString();
        url = new URL(protocol, ip, port, path);
      }
    } catch (MalformedURLException e) {
      log.error("Malformed URL", e);
      throw new RuntimeException(e);
    }
    return url;
  }

  @Override
  public void close() {
    // WebDriver
    if (driver != null) {
      try {
        log.debug("Closing webdriver of {} ", id);
        driver.quit();
        driver = null;
      } catch (Throwable t) {
        log.warn("** Exception closing webdriver {} : {}", t.getClass(), t.getMessage());
      }
    }

    // Stop Selenium Grid (if necessary)
    if (GridHandler.getInstance().useRemoteNodes()) {
      log.debug("Closing Grid of {} ", id);
      GridHandler.getInstance().stopGrid();
    }

    // Stop docker containers (if necessary)
    if (scope == BrowserScope.DOCKER || scope == BrowserScope.ELASTEST) {
      Path logFile = KurentoTest.getDefaultOutputFolder().toPath();

      try {
        if (!Files.exists(logFile)) {
          Files.createDirectories(logFile);
        }
        getDockerManager().setDownloadLogsPath(logFile);

      } catch (IOException e) {
        log.warn("Exception creating path {} for logs", logFile);
      }

      getDockerManager().closeDriver(id);
    }
  }

  public String getJobId() {
    return jobId;
  }

  public static class Builder {

    private int timeout = 60; // seconds
    private int thresholdTime = 10; // seconds
    private double colorDistance = 60;
    private String node = getProperty(TEST_HOST_PROPERTY,
        getProperty(TEST_PUBLIC_IP_PROPERTY, TEST_PUBLIC_IP_DEFAULT));
    private String host = node;
    private int serverPort = getProperty(TEST_PORT_PROPERTY,
        getProperty(TEST_PUBLIC_PORT_PROPERTY, WebServerService.getAppHttpsPort()));
    private BrowserScope scope = BrowserScope.LOCAL;
    private BrowserType browserType = BrowserType.CHROME;
    private Protocol protocol =
        Protocol.valueOf(getProperty(TEST_PROTOCOL_PROPERTY, TEST_PROTOCOL_DEFAULT).toUpperCase());
    private WebPageType webPageType =
        WebPageType.value2WebPageType(getProperty(TEST_PATH_PROPERTY, TEST_PATH_DEFAULT));
    private boolean usePhysicalCam = false;
    private boolean enableScreenCapture = false;
    private int recordAudio = 0; // seconds
    private int audioSampleRate; // samples per seconds (e.g. 8000, 16000)
    private AudioChannel audioChannel; // stereo, mono
    private int numInstances = 0;
    private int browserPerInstance = 1;
    private String video;
    private String audio;
    private String browserVersion;
    private Platform platform;
    private String login;
    private String passwd;
    private String pem;
    private boolean avoidProxy;
    private String parentTunnel;
    private List<Map<String, String>> extensions;
    private URI url;
    private String webPagePath;

    public Builder browserPerInstance(int browserPerInstance) {
      this.browserPerInstance = browserPerInstance;
      return this;
    }

    public Builder login(String login) {
      this.login = login;
      return this;
    }

    public Builder passwd(String passwd) {
      this.passwd = passwd;
      return this;
    }

    public Builder pem(String pem) {
      this.pem = pem;
      return this;
    }

    public Builder protocol(Protocol protocol) {
      this.protocol = protocol;
      return this;
    }

    public Builder numInstances(int numInstances) {
      this.numInstances = numInstances;
      return this;
    }

    public Builder serverPort(int serverPort) {
      this.serverPort = serverPort;
      return this;
    }

    public Builder node(String node) {
      this.node = node;
      return this;
    }

    public Builder scope(BrowserScope scope) {
      String scopeProp = getProperty(SELENIUM_SCOPE_PROPERTY);
      if (scopeProp != null) {
        scope = BrowserScope.valueOf(scopeProp.toUpperCase());
      }
      this.scope = scope;

      String appAutostart = getProperty(TestConfiguration.TEST_APP_AUTOSTART_PROPERTY,
          TestConfiguration.TEST_APP_AUTOSTART_DEFAULT);

      if ((BrowserScope.DOCKER.equals(scope) || scope == BrowserScope.ELASTEST)
          && !appAutostart.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {

        if (docker.isRunningInContainer()) {
          this.node = docker.getContainerIpAddress();
        } else {
          this.node = docker.getHostIpForContainers();
        }
      }

      return this;
    }

    public Builder timeout(int timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder thresholdTime(int thresholdTime) {
      this.thresholdTime = thresholdTime;
      return this;
    }

    public Builder colorDistance(double colorDistance) {
      this.colorDistance = colorDistance;
      return this;
    }

    public Builder video(String video) {
      this.video = video;
      return this;
    }

    public Builder webPageType(WebPageType webPageType) {
      this.webPageType = webPageType;
      return this;
    }

    public Builder browserType(BrowserType browser) {
      this.browserType = browser;
      return this;
    }

    public Builder usePhysicalCam() {
      this.usePhysicalCam = true;
      return this;
    }

    public Builder avoidProxy() {
      this.avoidProxy = true;
      return this;
    }

    public Builder parentTunnel(String parentTunnel) {
      this.parentTunnel = parentTunnel;
      return this;
    }

    public Builder enableScreenCapture() {
      this.enableScreenCapture = true;
      return this;
    }

    public Builder audio(String audio, int recordAudio, int audioSampleRate,
        AudioChannel audioChannel) {
      this.audio = audio;
      this.recordAudio = recordAudio;
      this.audioSampleRate = audioSampleRate;
      this.audioChannel = audioChannel;
      return this;
    }

    public Builder browserVersion(String browserVersion) {
      this.browserVersion = browserVersion;
      return this;
    }

    public Builder platform(Platform platform) {
      this.platform = platform;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder extensions(List<Map<String, String>> extensions) {
      this.extensions = extensions;
      return this;
    }

    public Builder url(String url) {
      try {
        this.url = new URI(url);

      } catch (URISyntaxException e) {
        throw new KurentoException("Could not parse URI " + url);
      }
      return this;
    }

    public Builder webPagePath(String webPagePath) {
      this.webPagePath = webPagePath;
      return this;
    }

    public Browser build() {
      return new Browser(this);
    }

  }
}
