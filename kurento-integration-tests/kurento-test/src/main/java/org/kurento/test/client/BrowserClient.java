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
import static org.kurento.test.TestConfiguration.SAUCELAB_KEY_PROPERTY;
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

import java.io.Closeable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
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

	public BrowserClient(Builder builder) {
		this.builder = builder;
		this.scope = builder.scope;
		this.video = builder.video;
		this.audio = builder.audio;
		this.serverPort = getProperty(TEST_PORT_PROPERTY,
				getProperty(TEST_PUBLIC_PORT_PROPERTY, builder.serverPort));
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
	}

	public void init() {
		Class<? extends WebDriver> driverClass = browserType.getDriverClass();

		try {
			DesiredCapabilities capabilities = new DesiredCapabilities();
			if (driverClass.equals(FirefoxDriver.class)) {
				FirefoxProfile profile = new FirefoxProfile();
				// This flag avoids granting the access to the camera
				profile.setPreference("media.navigator.permission.disabled",
						true);

				capabilities.setCapability(FirefoxDriver.PROFILE, profile);
				capabilities.setBrowserName(DesiredCapabilities.firefox()
						.getBrowserName());

				if (scope == BrowserScope.SAUCELABS) {
					createSaucelabsDriver(capabilities);
				} else if (scope == BrowserScope.REMOTE) {
					createRemoteDriver(capabilities);
				} else {
					driver = new FirefoxDriver(profile);
				}

			} else if (driverClass.equals(ChromeDriver.class)) {
				String chromedriver = null;
				if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
					chromedriver = "chromedriver";
				} else if (SystemUtils.IS_OS_WINDOWS) {
					chromedriver = "chromedriver.exe";
				}
				System.setProperty("webdriver.chrome.driver", new File(
						"target/webdriver/" + chromedriver).getAbsolutePath());
				ChromeOptions options = new ChromeOptions();

				if (enableScreenCapture) {
					// This flag enables the screen sharing
					options.addArguments("--enable-usermedia-screen-capturing");
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
						options.addArguments("--use-file-for-fake-video-capture="
								+ video);
					}
				}

				capabilities.setCapability(ChromeOptions.CAPABILITY, options);
				capabilities.setBrowserName(DesiredCapabilities.chrome()
						.getBrowserName());

				if (scope == BrowserScope.SAUCELABS) {
					createSaucelabsDriver(capabilities);
				} else if (scope == BrowserScope.REMOTE) {
					createRemoteDriver(capabilities);
				} else {
					driver = new ChromeDriver(options);
				}
			} else if (driverClass.equals(InternetExplorerDriver.class)) {

				if (scope == BrowserScope.SAUCELABS) {
					capabilities.setBrowserName(DesiredCapabilities
							.internetExplorer().getBrowserName());
					createSaucelabsDriver(capabilities);
				}

			} else if (driverClass.equals(SafariDriver.class)) {

				if (scope == BrowserScope.SAUCELABS) {
					capabilities.setBrowserName(DesiredCapabilities.safari()
							.getBrowserName());
					createSaucelabsDriver(capabilities);
				}

			}

			// Timeouts
			driver.manage().timeouts();
			driver.manage().timeouts()
					.implicitlyWait(timeout, TimeUnit.SECONDS);
			driver.manage().timeouts()
					.setScriptTimeout(timeout, TimeUnit.SECONDS);

			// Launch Browser
			String url;
			if (protocol == Protocol.FILE) {
				String clientPage = client.toString();
				File clientPageFile = new File(this.getClass().getClassLoader()
						.getResource("static" + clientPage).getFile());
				url = protocol.toString() + clientPageFile.getAbsolutePath();
			} else {
				String hostName = host != null ? host : node;
				url = protocol.toString() + hostName + ":" + serverPort
						+ client.toString();
			}
			log.info("*** Browsing URL with WebDriver: {}", url);
			driver.get(url);

		} catch (MalformedURLException e) {
			log.error("MalformedURLException in BrowserClient.initDriver", e);
		}

	}

	public void createSaucelabsDriver(DesiredCapabilities capabilities)
			throws MalformedURLException {
		assertPublicIpNotNull();
		String sauceLabsUser = getProperty(SAUCELAB_USER_PROPERTY);
		String sauceLabsKey = getProperty(SAUCELAB_KEY_PROPERTY);

		if (sauceLabsUser == null || sauceLabsKey == null) {
			throw new RuntimeException("Invalid Saucelabs credentials: "
					+ SAUCELAB_USER_PROPERTY + "=" + sauceLabsUser + " "
					+ SAUCELAB_KEY_PROPERTY + "=" + sauceLabsKey);
		}

		capabilities.setCapability("version", browserVersion);
		capabilities.setCapability("platform", platform);
		if (name != null) {
			capabilities.setCapability("name", name);
		}

		driver = new RemoteWebDriver(new URL("http://" + sauceLabsUser + ":"
				+ sauceLabsKey + "@ondemand.saucelabs.com:80/wd/hub"),
				capabilities);
	}

	public void createRemoteDriver(DesiredCapabilities capabilities)
			throws MalformedURLException {
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

			if (!node.equals(host)
					&& login != null
					&& !login.isEmpty()
					&& ((passwd != null && !passwd.isEmpty()) || (pem != null && !pem
							.isEmpty()))) {
				gridNode = new GridNode(node, browserType, browserPerInstance,
						login, passwd, pem);
				GridHandler.getInstance().addNode(id, gridNode);
			} else {
				gridNode = GridHandler.getInstance().getRandomNodeFromList(id,
						browserType, browserPerInstance);
			}

			// Start Hub (just the first time will be effective)
			GridHandler.getInstance().setHubAddress(host);
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
			ChromeOptions options = (ChromeOptions) capabilities
					.getCapability(ChromeOptions.CAPABILITY);
			options.addArguments("--use-file-for-fake-video-capture="
					+ GridHandler.getInstance().getFirstNode(id)
							.getRemoteVideo(video));
			capabilities.setCapability(ChromeOptions.CAPABILITY, options);
		}

		int hubPort = GridHandler.getInstance().getHubPort();

		driver = new RemoteWebDriver(new URL("http://" + host + ":" + hubPort
				+ "/wd/hub"), capabilities);
	}

	private void assertPublicIpNotNull() {
		if (host == null) {
			throw new RuntimeException(
					"Public IP must be available to run remote test. "
							+ "You can do it by adding the paramter -D"
							+ TEST_HOST_PROPERTY
							+ "=<public_ip> or with key 'host' in "
							+ "the JSON configuration file.");
		}
	}

	public void injectKurentoTestJs() {
		String kurentoTestJs = "var kurentoScript=window.document.createElement('script');";
		String kurentoTestJsPath = "./lib/kurento-test.js";
		if (this.getProtocol() == Protocol.FILE) {
			File clientPageFile = new File(this.getClass().getClassLoader()
					.getResource("static/lib/kurento-test.js").getFile());
			kurentoTestJsPath = this.getProtocol().toString()
					+ clientPageFile.getAbsolutePath();
		}
		kurentoTestJs += "kurentoScript.src='" + kurentoTestJsPath + "';";
		kurentoTestJs += "window.document.head.appendChild(kurentoScript);";
		kurentoTestJs += "return true;";
		this.executeScript(kurentoTestJs);
	}

	public static class Builder {
		private int timeout = 60; // seconds
		private int thresholdTime = 10; // seconds
		private double colorDistance = 60;
		private String node = getProperty(TEST_HOST_PROPERTY,
				getProperty(TEST_PUBLIC_IP_PROPERTY, TEST_PUBLIC_IP_DEFAULT));
		private String host = node;
		private int serverPort = getProperty(
				TEST_PORT_PROPERTY,
				getProperty(TEST_PUBLIC_PORT_PROPERTY,
						KurentoServicesTestHelper.getAppHttpPort()));
		private BrowserScope scope = BrowserScope.LOCAL;
		private BrowserType browserType = BrowserType.CHROME;
		private Protocol protocol = Protocol.valueOf(getProperty(
				TEST_PROTOCOL_PROPERTY, TEST_PROTOCOL_DEFAULT).toUpperCase());
		private Client client = Client.value2Client(getProperty(
				TEST_PATH_PROPERTY, TEST_PATH_DEFAULT));
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

		public Builder enableScreenCapture() {
			this.enableScreenCapture = true;
			return this;
		}

		public Builder audio(String audio, int recordAudio,
				int audioSampleRate, AudioChannel audioChannel) {
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

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	@Override
	public void close() {
		// Stop Selenium Grid (if necessary)
		if (GridHandler.getInstance().useRemoteNodes()) {
			GridHandler.getInstance().stopGrid();
		}

		// WebDriver
		driver.close();
		driver.quit();
		driver = null;
	}

}
