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

import java.io.Closeable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.Protocol;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.services.AudioChannel;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.test.services.Node;
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
	private double colorDistance;
	private int thresholdTime;
	private Protocol protocol;
	private int serverPort;
	private Client client;
	private String hostAddress;
	private SystemMonitorManager monitor;
	private Node remoteNode;

	public static final String SAUCELAB_USER_PROPERTY = "saucelab.user";
	public static final String SAUCELAB_KEY_PROPERTY = "saucelab.key";
	public static final String TEST_PUBLIC_IP_PROPERTY = "test.public.ip";
	public static final String TEST_PUBLIC_PORT_PROPERTY = "test.public.port";

	private BrowserClient(Builder builder) {
		this.scope = builder.scope;
		this.video = builder.video;
		this.audio = builder.audio;
		this.serverPort = builder.serverPort;
		this.client = builder.client;
		this.browserType = builder.browserType;
		this.usePhysicalCam = builder.usePhysicalCam;
		this.enableScreenCapture = builder.enableScreenCapture;
		this.remoteNode = builder.remoteNode;
		this.recordAudio = builder.recordAudio;
		this.audioSampleRate = builder.audioSampleRate;
		this.audioChannel = builder.audioChannel;
		this.browserVersion = builder.browserVersion;
		this.platform = builder.platform;
		this.name = builder.name;
		this.timeout = builder.timeout;
		this.colorDistance = builder.colorDistance;
		this.thresholdTime = builder.thresholdTime;
		this.hostAddress = builder.hostAddress;
		this.protocol = builder.protocol;

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

					if (video != null) {
						if (remoteNode != null) {
							options.addArguments("--use-file-for-fake-video-capture="
									+ remoteNode.getRemoteVideo());
						} else {
							options.addArguments("--use-file-for-fake-video-capture="
									+ video);
						}
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
				url = protocol.toString() + hostAddress + ":" + serverPort
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
		String sauceLabsUser = getProperty(SAUCELAB_USER_PROPERTY);
		String sauceLabsKey = getProperty(SAUCELAB_KEY_PROPERTY);

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
		driver = new RemoteWebDriver(new URL("http://" + hostAddress + ":"
				+ serverPort + "/wd/hub"), capabilities);
	}

	public static class Builder {
		private int timeout = 60; // seconds
		private int thresholdTime = 10; // seconds
		private double colorDistance = 60;
		private String hostAddress = getProperty(TEST_PUBLIC_IP_PROPERTY,
				"127.0.0.1");
		private int serverPort = getProperty(TEST_PUBLIC_PORT_PROPERTY,
				KurentoServicesTestHelper.getAppHttpPort());
		private BrowserScope scope = BrowserScope.LOCAL;
		private BrowserType browserType = BrowserType.CHROME;
		private Protocol protocol = Protocol.HTTP;
		private Client client = Client.WEBRTC;
		private boolean usePhysicalCam = false;
		private boolean enableScreenCapture = false;
		private int recordAudio = 0; // seconds
		private int audioSampleRate; // samples per seconds (e.g. 8000, 16000)
		private AudioChannel audioChannel; // stereo, mono
		private String video;
		private String audio;
		private Node remoteNode;
		private String browserVersion;
		private Platform platform;
		private String name;

		public Builder protocol(Protocol protocol) {
			this.protocol = protocol;
			return this;
		}

		public Builder serverPort(int serverPort) {
			this.serverPort = serverPort;
			return this;
		}

		public Builder hostAddress(String hostAddress) {
			this.hostAddress = hostAddress;
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

		public Builder remoteNode(Node remoteNode) {
			this.remoteNode = remoteNode;
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

		public Builder name(String name) {
			this.name = name;
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

	public Node getRemoteNode() {
		return remoteNode;
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

	public Object executeScript(String command) {
		return ((JavascriptExecutor) driver).executeScript(command);
	}

	public SystemMonitorManager getMonitor() {
		return monitor;
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

	public String getHostAddress() {
		return hostAddress;
	}

	public void setMonitor(SystemMonitorManager monitor) {
		this.monitor = monitor;
	}

	@Override
	public void close() {
		driver.close();
		driver.quit();
		driver = null;
	}

}
