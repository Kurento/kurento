/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.test.client;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.SAUCELAB_COMMAND_TIMEOUT_DEFAULT;
import static org.kurento.test.TestConfiguration.SAUCELAB_COMMAND_TIMEOUT_PROPERTY;
import static org.kurento.test.TestConfiguration.SAUCELAB_IDLE_TIMEOUT_DEFAULT;
import static org.kurento.test.TestConfiguration.SAUCELAB_IDLE_TIMEOUT_PROPERTY;
import static org.kurento.test.TestConfiguration.SAUCELAB_KEY_PROPERTY;
import static org.kurento.test.TestConfiguration.SAUCELAB_MAX_DURATION_DEFAULT;
import static org.kurento.test.TestConfiguration.SAUCELAB_MAX_DURATION_PROPERTY;
import static org.kurento.test.TestConfiguration.SAUCELAB_USER_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_HOST_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_NODE_LOGIN_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_NODE_PASSWD_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_NODE_PEM_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_PATH_DEFAULT;
import static org.kurento.test.TestConfiguration.TEST_PATH_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_PORT_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_PROTOCOL_DEFAULT;
import static org.kurento.test.TestConfiguration.TEST_PROTOCOL_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_PUBLIC_IP_DEFAULT;
import static org.kurento.test.TestConfiguration.TEST_PUBLIC_IP_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_PUBLIC_PORT_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_SCREEN_SHARE_TITLE_DEFAULT;
import static org.kurento.test.TestConfiguration.TEST_SCREEN_SHARE_TITLE_DEFAULT_WIN;
import static org.kurento.test.TestConfiguration.TEST_SCREEN_SHARE_TITLE_PROPERTY;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.Protocol;
import org.kurento.test.grid.GridHandler;
import org.kurento.test.grid.GridNode;
import org.kurento.test.services.AudioChannel;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.bonigarcia.wdm.ChromeDriverManager;

/**
 * Wrapper of Selenium Webdriver for testing Kurento applications.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.1.0
 * @see <a href="http://www.seleniumhq.org/">Selenium</a>
 */
public class BrowserClient implements Closeable {

	public Logger log = LoggerFactory.getLogger(BrowserClient.class);

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
	private Client client;
	private String login;
	private String passwd;
	private String pem;
	private boolean avoidProxy;
	private String parentTunnel;
	private List<Map<String, String>> extensions;

	private String url;

	public BrowserClient(Builder builder) {
		this.builder = builder;
		this.scope = builder.scope;
		this.video = builder.video;
		this.audio = builder.audio;
		this.serverPort = getProperty(TEST_PORT_PROPERTY, getProperty(TEST_PUBLIC_PORT_PROPERTY, builder.serverPort));
		this.client = builder.client;
		this.browserType = builder.browserType;
		this.usePhysicalCam = builder.usePhysicalCam;
		this.enableScreenCapture = builder.enableScreenCapture;
		this.recordAudio = builder.recordAudio;
		this.audioSampleRate = builder.audioSampleRate;
		this.audioChannel = builder.audioChannel;
		this.browserVersion = builder.browserVersion;
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
	}

	public void init() {

		Class<? extends WebDriver> driverClass = browserType.getDriverClass();

		try {
			DesiredCapabilities capabilities = new DesiredCapabilities();

			if (driverClass.equals(FirefoxDriver.class)) {
				FirefoxProfile profile = new FirefoxProfile();
				// This flag avoids granting the access to the camera
				profile.setPreference("media.navigator.permission.disabled", true);

				capabilities.setCapability(FirefoxDriver.PROFILE, profile);
				capabilities.setBrowserName(DesiredCapabilities.firefox().getBrowserName());

				// Firefox extensions
				if (extensions != null && !extensions.isEmpty()) {
					for (Map<String, String> extension : extensions) {
						InputStream is = getExtensionAsInputStream(extension.values().iterator().next());
						if (is != null) {
							try {
								File xpi = File.createTempFile(extension.keySet().iterator().next(), ".xpi");
								FileUtils.copyInputStreamToFile(is, xpi);
								profile.addExtension(xpi);
							} catch (Throwable t) {
								log.error("Error loading Firefox extension {} ({} : {})", extension, t.getClass(),
										t.getMessage());
							}
						}
					}
				}

				if (scope == BrowserScope.SAUCELABS) {
					createSaucelabsDriver(capabilities);
				} else if (scope == BrowserScope.REMOTE) {
					createRemoteDriver(capabilities);
				} else {
					driver = new FirefoxDriver(profile);
				}

			} else if (driverClass.equals(ChromeDriver.class)) {
				// Chrome driver
				ChromeDriverManager.getInstance().setup();

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

				if (protocol == Protocol.FILE) {
					// This flag allows reading local files in video tags
					options.addArguments("--allow-file-access-from-files");
				}

				if (!usePhysicalCam) {
					// This flag makes using a synthetic video (green with
					// spinner) in WebRTC. Or it is needed to combine with
					// use-file-for-fake-video-capture to use a file faking the
					// cam
					options.addArguments("--use-fake-device-for-media-stream");

					if (video != null && isLocal()) {
						options.addArguments("--use-file-for-fake-video-capture=" + video);
					}
				}

				capabilities.setCapability(ChromeOptions.CAPABILITY, options);
				capabilities.setBrowserName(DesiredCapabilities.chrome().getBrowserName());

				if (scope == BrowserScope.SAUCELABS) {
					createSaucelabsDriver(capabilities);
				} else if (scope == BrowserScope.REMOTE) {
					createRemoteDriver(capabilities);
				} else {
					driver = new ChromeDriver(options);
				}
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

			if (protocol == Protocol.FILE) {
				String clientPage = client.toString();
				File clientPageFile = new File(
						this.getClass().getClassLoader().getResource("static" + clientPage).getFile());
				url = protocol.toString() + clientPageFile.getAbsolutePath();
			} else {
				String hostName = host != null ? host : node;
				url = protocol.toString() + hostName + ":" + serverPort + client.toString();
			}
			log.info("*** Browsing URL with WebDriver: {}", url);
			driver.get(url);

		} catch (MalformedURLException e) {
			log.error("MalformedURLException in BrowserClient.initDriver", e);
		}

		// startPing();
	}

	// private void startPing() {
	// if (ping) {
	// exec.scheduleAtFixedRate(new Runnable() {
	// @Override
	// public void run() {
	// driver.findElement(By.name("body"));
	// }
	// }, PING_DELAY, PING_DELAY, TimeUnit.MILLISECONDS);
	// }
	// }

	public void reload() {
		if (url != null) {
			this.driver.get(url);
		}
	}

	public InputStream getExtensionAsInputStream(String extension) {
		InputStream is = null;

		try {
			log.info("Trying to locate extension in the classpath ({}) ...", extension);
			is = ClassLoader.getSystemResourceAsStream(extension);
			if (is.available() < 0) {
				log.warn("Extension {} is not located in the classpath", extension);
				is = null;
			} else {
				log.info("Success. Loading extension {} from classpath", extension);
			}
		} catch (Throwable t) {
			log.warn("Exception reading extension {} in the classpath ({} : {})", extension, t.getClass(),
					t.getMessage());
			is = null;
		}
		if (is == null) {
			try {
				log.info("Trying to locate extension as URL ({}) ...", extension);
				URL url = new URL(extension);
				is = url.openStream();
				log.info("Success. Loading extension {} from URL", extension);
			} catch (Throwable t) {
				log.warn("Exception reading extension {} as URL ({} : {})", extension, t.getClass(), t.getMessage());
			}
		}
		if (is == null) {
			throw new RuntimeException(extension + " is not a valid extension (it is not located in project"
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
		int idleTimeout = getProperty(SAUCELAB_IDLE_TIMEOUT_PROPERTY, SAUCELAB_IDLE_TIMEOUT_DEFAULT);
		int commandTimeout = getProperty(SAUCELAB_COMMAND_TIMEOUT_PROPERTY, SAUCELAB_COMMAND_TIMEOUT_DEFAULT);
		int maxDuration = getProperty(SAUCELAB_MAX_DURATION_PROPERTY, SAUCELAB_MAX_DURATION_DEFAULT);

		if (sauceLabsUser == null || sauceLabsKey == null) {
			throw new RuntimeException("Invalid Saucelabs credentials: " + SAUCELAB_USER_PROPERTY + "=" + sauceLabsUser
					+ " " + SAUCELAB_KEY_PROPERTY + "=" + sauceLabsKey);
		}

		capabilities.setCapability("version", browserVersion);
		capabilities.setCapability("platform", platform);

		if (parentTunnel != null) {
			capabilities.setCapability("parent-tunnel", parentTunnel);
		}
		if (avoidProxy) {
			capabilities.setCapability("avoid-proxy", avoidProxy);
		}

		capabilities.setCapability("idleTimeout", idleTimeout);
		capabilities.setCapability("commandTimeout", commandTimeout);
		capabilities.setCapability("maxDuration", maxDuration);

		if (name != null) {
			capabilities.setCapability("name", name);
		}

		driver = new RemoteWebDriver(
				new URL("http://" + sauceLabsUser + ":" + sauceLabsKey + "@ondemand.saucelabs.com:80/wd/hub"),
				capabilities);

		jobId = ((RemoteWebDriver) driver).getSessionId().toString();
		log.info("%%%%%%%%%%%%% Saucelabs URL job for {} ({} {} in {}) %%%%%%%%%%%%%", id, browserType, browserVersion,
				platform);
		log.info("https://saucelabs.com/tests/{}", jobId);
	}

	public void createRemoteDriver(DesiredCapabilities capabilities) throws MalformedURLException {
		assertPublicIpNotNull();
		if (!GridHandler.getInstance().containsSimilarBrowserKey(id)) {
			GridNode gridNode = null;

			if (login != null) {
				System.setProperty(TEST_NODE_LOGIN_PROPERTY, login);
			}
			if (passwd != null) {
				System.setProperty(TEST_NODE_PASSWD_PROPERTY, passwd);
			}
			if (pem != null) {
				System.setProperty(TEST_NODE_PEM_PROPERTY, pem);
			}

			if (!node.equals(host) && login != null && !login.isEmpty()
					&& (passwd != null && !passwd.isEmpty() || pem != null && !pem.isEmpty())) {
				gridNode = new GridNode(node, browserType, browserPerInstance, login, passwd, pem);
				GridHandler.getInstance().addNode(id, gridNode);
			} else {
				gridNode = GridHandler.getInstance().getRandomNodeFromList(id, browserType, browserPerInstance);
			}

			// Start Hub (just the first time will be effective)
			GridHandler.getInstance().startHub();

			// Start node
			GridHandler.getInstance().startNode(gridNode);

			// Copy video (if necessary)
			if (video != null && browserType == BrowserType.CHROME) {
				GridHandler.getInstance().copyRemoteVideo(gridNode, video);
			}

		}

		// At this moment we are able to use the argument for remote video
		if (video != null && browserType == BrowserType.CHROME) {
			ChromeOptions options = (ChromeOptions) capabilities.getCapability(ChromeOptions.CAPABILITY);
			options.addArguments("--use-file-for-fake-video-capture="
					+ GridHandler.getInstance().getFirstNode(id).getRemoteVideo(video));
			capabilities.setCapability(ChromeOptions.CAPABILITY, options);
		}

		int hubPort = GridHandler.getInstance().getHubPort();
		String hubHost = GridHandler.getInstance().getHubHost();

		driver = new RemoteWebDriver(new URL("http://" + hubHost + ":" + hubPort + "/wd/hub"), capabilities);
	}

	private void assertPublicIpNotNull() {
		if (host == null) {
			throw new RuntimeException("Public IP must be available to run remote test. "
					+ "You can do it by adding the paramter -D" + TEST_HOST_PROPERTY
					+ "=<public_ip> or with key 'host' in " + "the JSON configuration file.");
		}
	}

	public void injectKurentoTestJs() {
		if (this.getBrowserType() != BrowserType.IEXPLORER) {
			String kurentoTestJs = "var kurentoScript=window.document.createElement('script');";
			String kurentoTestJsPath = "./lib/kurento-test.js";
			if (this.getProtocol() == Protocol.FILE) {
				File clientPageFile = new File(
						this.getClass().getClassLoader().getResource("static/lib/kurento-test.js").getFile());
				kurentoTestJsPath = this.getProtocol().toString() + clientPageFile.getAbsolutePath();
			}
			kurentoTestJs += "kurentoScript.src='" + kurentoTestJsPath + "';";
			kurentoTestJs += "window.document.head.appendChild(kurentoScript);";
			kurentoTestJs += "return true;";
			this.executeScript(kurentoTestJs);
		}
	}

	public static class Builder {
		private int timeout = 60; // seconds
		private int thresholdTime = 10; // seconds
		private double colorDistance = 60;
		private String node = getProperty(TEST_HOST_PROPERTY,
				getProperty(TEST_PUBLIC_IP_PROPERTY, TEST_PUBLIC_IP_DEFAULT));
		private String host = node;
		private int serverPort = getProperty(TEST_PORT_PROPERTY,
				getProperty(TEST_PUBLIC_PORT_PROPERTY, KurentoServicesTestHelper.getAppHttpPort()));
		private BrowserScope scope = BrowserScope.LOCAL;
		private BrowserType browserType = BrowserType.CHROME;
		private Protocol protocol = Protocol
				.valueOf(getProperty(TEST_PROTOCOL_PROPERTY, TEST_PROTOCOL_DEFAULT).toUpperCase());
		private Client client = Client.value2Client(getProperty(TEST_PATH_PROPERTY, TEST_PATH_DEFAULT));
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
			this.scope = scope;
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

		public Builder client(Client client) {
			this.client = client;
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

		public Builder audio(String audio, int recordAudio, int audioSampleRate, AudioChannel audioChannel) {
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

		public BrowserClient build() {
			return new BrowserClient(this);
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

	public WebDriver getDriver() {
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
				out[0] = executeScript(command);
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

	public boolean isLocal() {
		return BrowserScope.LOCAL.equals(this.scope);
	}

	public boolean isRemote() {
		return BrowserScope.REMOTE.equals(this.scope);
	}

	public boolean isSauceLabs() {
		return BrowserScope.SAUCELABS.equals(this.scope);
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

	public Client getClient() {
		return client;
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
		String ip = this.getHost();
		int port = this.getServerPort();
		String protocol = this.getProtocol().toString();
		String path = this.getClient().toString();
		URL url = null;
		try {
			url = new URL(protocol + ip + ":" + port + path);
		} catch (MalformedURLException e) {
			log.error("Malformed URL", e);
			throw new RuntimeException(e);
		}
		return url;
	}

	@Override
	public void close() {
		// Stop Selenium Grid (if necessary)
		if (GridHandler.getInstance().useRemoteNodes()) {
			GridHandler.getInstance().stopGrid();
		}

		// WebDriver
		if (driver != null) {
			try {
				driver.quit();
				driver = null;
			} catch (Throwable t) {
				log.warn("Exception closing webdriver {} : {}", t.getClass(), t.getMessage());
			}
		}
	}

	public String getJobId() {
		return jobId;
	}

}
