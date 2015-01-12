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

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.LatencyException;
import org.kurento.test.latency.VideoTag;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.services.AudioChannel;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.test.services.Node;
import org.kurento.test.services.Recorder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that models the video tag (HTML5) in a web browser; it uses Selenium to
 * launch the real browser.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 * @see <a href="http://www.seleniumhq.org/">Selenium</a>
 */
public class BrowserClient implements Closeable {

	public Logger log = LoggerFactory.getLogger(BrowserClient.class);
	private List<Thread> callbackThreads = new ArrayList<>();
	private Map<String, CountDownLatch> countDownLatchEvents;

	private WebDriver driver;
	private JavascriptExecutor js;
	private String videoUrl;
	private int timeout; // seconds
	private double maxDistance;

	private String video;
	private String audio;
	private int serverPort;
	private Client client;
	private Browser browser;
	private boolean usePhysicalCam;
	private boolean useHttps;
	private boolean enableScreenCapture;
	private Node remoteNode;
	private int recordAudio;
	private int audioSampleRate;
	private AudioChannel audioChannel;
	private SystemMonitorManager monitor;

	private BrowserClient(Builder builder) {

		this.video = builder.video;
		this.audio = builder.audio;
		this.serverPort = builder.serverPort;
		this.client = builder.client;
		this.browser = builder.browser;
		this.usePhysicalCam = builder.usePhysicalCam;
		this.enableScreenCapture = builder.enableScreenCapture;
		this.useHttps = builder.useHttps;
		this.remoteNode = builder.remoteNode;
		this.recordAudio = builder.recordAudio;
		this.audioSampleRate = builder.audioSampleRate;
		this.audioChannel = builder.audioChannel;

		countDownLatchEvents = new HashMap<>();
		timeout = 60; // default (60 seconds)
		maxDistance = 60.0; // default distance (for color comparison)

		// Setup Selenium
		initDriver();

		String hostAddress;

		if (remoteNode != null) {
			hostAddress = getProperty(
					PerformanceTest.SELENIUM_HUB_HOST_PROPERTY,
					PerformanceTest.SELENIUM_HUB_HOST_DEFAULT);
			hostAddress = getProperty(
					PerformanceTest.SELENIUM_HUB_PUBLIC_PROPERTY, hostAddress);

		} else {
			hostAddress = "127.0.0.1";
		}

		// Selenium timeouts
		driver.manage().timeouts();
		driver.manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(timeout, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(timeout, TimeUnit.SECONDS);

		// Launch Browser
		String protocol = useHttps ? "https" : "http";
		driver.get(protocol + "://" + hostAddress + ":" + serverPort
				+ client.toString());

		addTestName(KurentoServicesTestHelper.getTestCaseName() + "."
				+ KurentoServicesTestHelper.getTestName());

	}

	private void initDriver() {
		Class<? extends WebDriver> driverClass = browser.getDriverClass();

		String hostAddress = getProperty(
				PerformanceTest.SELENIUM_HUB_HOST_PROPERTY,
				PerformanceTest.SELENIUM_HUB_HOST_DEFAULT);

		int hubPort = getProperty(PerformanceTest.SELENIUM_HUB_PORT_PROPERTY,
				PerformanceTest.SELENIUM_HUB_PORT_DEFAULT);

		try {
			if (driverClass.equals(FirefoxDriver.class)) {
				FirefoxProfile profile = new FirefoxProfile();
				// This flag avoids granting the access to the camera
				profile.setPreference("media.navigator.permission.disabled",
						true);
				if (remoteNode != null) {
					DesiredCapabilities capabilities = new DesiredCapabilities();
					capabilities.setCapability(FirefoxDriver.PROFILE, profile);
					capabilities.setBrowserName(DesiredCapabilities.firefox()
							.getBrowserName());

					driver = new RemoteWebDriver(new URL("http://"
							+ hostAddress + ":" + hubPort + "/wd/hub"),
							capabilities);
				} else {
					driver = new FirefoxDriver(profile);
				}

				if (!usePhysicalCam && video != null) {
					launchFakeCam();
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

				// This flag avoids warning in chrome. See:
				// https://code.google.com/p/chromedriver/issues/detail?id=799
				options.addArguments("--test-type");

				if (!usePhysicalCam) {
					// This flag makes using a synthetic video (green with
					// spinner) in webrtc. Or it is needed to combine with
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

							// Alternative: lauch fake cam also in Chrome
							// launchFakeCam();
						}
					}
				}

				if (remoteNode != null) {
					DesiredCapabilities capabilities = new DesiredCapabilities();
					capabilities.setCapability(ChromeOptions.CAPABILITY,
							options);
					capabilities.setBrowserName(DesiredCapabilities.chrome()
							.getBrowserName());
					driver = new RemoteWebDriver(new URL("http://"
							+ hostAddress + ":" + hubPort + "/wd/hub"),
							capabilities);

				} else {
					driver = new ChromeDriver(options);
				}

			}
			js = ((JavascriptExecutor) driver);

		} catch (MalformedURLException e) {
			log.error("MalformedURLException in BrowserClient.initDriver", e);
		}
	}

	private void launchFakeCam() {
		FakeCam.getSingleton().launchCam(video);
	}

	public void setURL(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public void resetEvents() {
		driver.findElement(By.id("status")).clear();
	}

	private void setColorCoordinates(int x, int y) {
		driver.findElement(By.id("x")).clear();
		driver.findElement(By.id("y")).clear();
		driver.findElement(By.id("x")).sendKeys(String.valueOf(x));
		driver.findElement(By.id("y")).sendKeys(String.valueOf(y));
	}

	public void subscribeEvents(String... eventType) {
		for (final String e : eventType) {
			CountDownLatch latch = new CountDownLatch(1);
			countDownLatchEvents.put(e, latch);
			this.addEventListener(e, new BrowserEventListener() {
				@Override
				public void onEvent(String event) {
					consoleLog(ConsoleLogLevel.info, "Event in video tag: "
							+ event);
					countDownLatchEvents.get(e).countDown();
				}
			});
		}
	}

	public boolean waitForEvent(final String eventType)
			throws InterruptedException {
		if (!countDownLatchEvents.containsKey(eventType)) {
			// We cannot wait for an event without previous subscription
			return false;
		}

		boolean result = countDownLatchEvents.get(eventType).await(timeout,
				TimeUnit.SECONDS);

		// Record local audio when playing event reaches the browser
		if (eventType.equalsIgnoreCase("playing") && recordAudio > 0) {
			if (remoteNode != null) {
				Recorder.recordRemote(remoteNode, recordAudio, audioSampleRate,
						audioChannel);
			} else {
				Recorder.record(recordAudio, audioSampleRate, audioChannel);
			}
		}

		countDownLatchEvents.remove(eventType);
		return result;
	}

	@SuppressWarnings("deprecation")
	public void addEventListener(final String eventType,
			final BrowserEventListener eventListener) {
		Thread t = new Thread() {
			public void run() {
				js.executeScript("video.addEventListener('" + eventType
						+ "', videoEvent, false);");
				try {
					(new WebDriverWait(driver, timeout))
							.until(new ExpectedCondition<Boolean>() {
								public Boolean apply(WebDriver d) {
									return d.findElement(By.id("status"))
											.getAttribute("value")
											.equalsIgnoreCase(eventType);
								}
							});
					eventListener.onEvent(eventType);
				} catch (Throwable t) {
					log.error("~~~ Exception in addEventListener {}",
							t.getMessage());
					this.interrupt();
					this.stop();
				}
			}
		};
		callbackThreads.add(t);
		t.setDaemon(true);
		t.start();
	}

	public void start() {
		js.executeScript("play('" + videoUrl + "', false);");
	}

	public void stop() {
		js.executeScript("terminate();");
	}

	public void addTestName(String testName) {
		try {
			js.executeScript("addTestName('" + testName + "');");
		} catch (WebDriverException we) {
			log.warn(we.getMessage());
		}
	}

	public void appendStringToTitle(String webRtcMode) {
		try {
			js.executeScript("appendStringToTitle('" + webRtcMode + "');");
		} catch (WebDriverException we) {
			log.warn(we.getMessage());
		}
	}

	public void consoleLog(ConsoleLogLevel level, String message) {
		log.info(message);
		js.executeScript("console." + level.toString() + "('" + message + "');");
	}

	@Override
	@SuppressWarnings("deprecation")
	public void close() {
		for (Thread t : callbackThreads) {
			t.stop();
		}
		driver.close();
		driver = null;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public double getCurrentTime() {
		log.debug("getCurrentTime() called");
		double currentTime = Double.parseDouble(driver.findElement(
				By.id("currentTime")).getAttribute("value"));
		log.debug("getCurrentTime() result: {}", currentTime);
		return currentTime;
	}

	public boolean similarColorAt(Color expectedColor, int x, int y) {
		boolean out;
		final long endTimeMillis = System.currentTimeMillis()
				+ (timeout * 1000);
		setColorCoordinates(x, y);

		while (true) {
			out = compareColor(expectedColor);
			if (out || System.currentTimeMillis() > endTimeMillis) {
				break;
			} else {
				// Polling: wait 200 ms and check again the color
				// Max wait = timeout variable
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					log.trace("InterruptedException in guard condition ({})",
							e.getMessage());
				}
			}
		}
		return out;
	}

	public boolean similarColor(Color expectedColor) {
		return similarColorAt(expectedColor, 0, 0);
	}

	private boolean compareColor(Color expectedColor) {
		String[] realColor = driver.findElement(By.id("color"))
				.getAttribute("value").split(",");
		int red = Integer.parseInt(realColor[0]);
		int green = Integer.parseInt(realColor[1]);
		int blue = Integer.parseInt(realColor[2]);

		double distance = Math.sqrt((red - expectedColor.getRed())
				* (red - expectedColor.getRed())
				+ (green - expectedColor.getGreen())
				* (green - expectedColor.getGreen())
				+ (blue - expectedColor.getBlue())
				* (blue - expectedColor.getBlue()));

		boolean out = distance <= getMaxDistance();
		if (!out) {
			log.error(
					"Difference in color comparision. Expected: {}, Real: {} (distance={})",
					expectedColor, realColor, distance);
		}

		return out;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public void initWebRtcSdpProcessor(SdpOfferProcessor sdpOfferProcessor,
			WebRtcChannel channel, WebRtcMode mode) {

		// Append WebRTC mode (send/receive and audio/video) to identify test
		appendStringToTitle(mode.toString());
		appendStringToTitle(channel.toString());

		// Setting custom audio stream (if necessary)
		if (audio != null) {
			js.executeScript("setCustomAudio('" + audio + "');");
		}

		// Setting MediaConstraints (if necessary)
		String channelJsFunction = channel.getJsFunction();
		if (channelJsFunction != null) {
			js.executeScript(channelJsFunction);
		}

		// Execute JavaScript kurentoUtils.WebRtcPeer
		js.executeScript(mode.getJsFunction());

		// Wait to valid sdpOffer
		(new WebDriverWait(driver, timeout))
				.until(new ExpectedCondition<Boolean>() {
					public Boolean apply(WebDriver d) {
						return js.executeScript("return sdpOffer;") != null;
					}
				});
		String sdpOffer = (String) js.executeScript("return sdpOffer;");
		String sdpAnswer = sdpOfferProcessor.processSdpOffer(sdpOffer);

		// Encoding in Base64 to avoid parsing errors in JavaScript
		sdpAnswer = new String(Base64.encodeBase64(sdpAnswer.getBytes()));

		// Process sdpAnswer
		js.executeScript("processSdpAnswer('" + sdpAnswer + "');");

	}

	@SuppressWarnings("deprecation")
	public void initWebRtc(final WebRtcEndpoint webRtcEndpoint,
			final WebRtcChannel channel, final WebRtcMode mode)
			throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread() {
			public void run() {
				initWebRtcSdpProcessor(new SdpOfferProcessor() {
					@Override
					public String processSdpOffer(String sdpOffer) {
						return webRtcEndpoint.processOffer(sdpOffer);
					}
				}, channel, mode);
				latch.countDown();
			}
		};
		t.start();
		if (!latch.await(this.getTimeout(), TimeUnit.SECONDS)) {
			t.interrupt();
			t.stop();
		}
	}

	public static class Builder {
		private String video;
		private String audio;
		private int serverPort;
		private Client client;
		private Browser browser = Browser.CHROME;
		private boolean usePhysicalCam;
		private boolean enableScreenCapture;
		private boolean useHttps;
		private Node remoteNode;
		private int recordAudio; // seconds
		private int audioSampleRate; // samples per seconds (e.g. 8000, 16000)
		private AudioChannel audioChannel; // stereo, mono

		public Builder() {
			this(KurentoServicesTestHelper.getAppHttpPort());
		}

		public Builder(int serverPort) {
			this.serverPort = serverPort;

			// By default physical camera will not be used; instead synthetic
			// videos will be used for testing
			this.usePhysicalCam = false;

			// By default is not a remote test
			this.remoteNode = null;

			// By default, not recording audio (0 seconds)
			this.recordAudio = 0;

			// By default, not enabling the screen capture
			this.enableScreenCapture = false;

			// By default HTTPS is not used
			this.useHttps = false;
		}

		public Builder video(String video) {
			this.video = video;
			return this;
		}

		public Builder client(Client client) {
			this.client = client;
			return this;
		}

		public Builder browser(Browser browser) {
			this.browser = browser;
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

		public Builder useHttps() {
			this.useHttps = true;
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

		public BrowserClient build() {
			return new BrowserClient(this);
		}
	}

	@Deprecated
	public void addChangeColorEventListener(VideoTag type, LatencyController cs) {
		cs.addChangeColorEventListener(type, js, type.getName());
	}

	@Deprecated
	public void addChangeColorEventListener(VideoTag type,
			LatencyController cs, String name) {
		cs.addChangeColorEventListener(type, js, name);
	}

	public void takeScreeshot(String file) throws IOException {
		File scrFile = ((TakesScreenshot) driver)
				.getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(scrFile, new File(file));
	}

	@SuppressWarnings("deprecation")
	public long getLatency() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final long[] out = new long[1];
		Thread t = new Thread() {
			public void run() {
				out[0] = (Long) js.executeScript("return latency;");
				latch.countDown();
			}
		};
		t.start();
		if (!latch.await(this.getTimeout(), TimeUnit.SECONDS)) {
			t.interrupt();
			t.stop();
			throw new LatencyException("Timeout getting latency ("
					+ this.getTimeout() + "  seconds)");
		}
		if (out[0] < 0) {
			throw new LatencyException("Error taking latency in the browser");
		}
		return out[0];
	}

	public void activateLatencyControl() {
		this.subscribeEvents("playing");
	}

	public long getRemoteTime() {
		Object time = js.executeScript(VideoTag.REMOTE.getTime());
		return (time == null) ? 0 : (Long) time;
	}

	public void checkLatencyUntil(long endTimeMillis)
			throws InterruptedException, IOException {
		while (true) {
			if (System.currentTimeMillis() > endTimeMillis) {
				break;
			}
			Thread.sleep(100);
			try {
				monitor.addCurrentLatency(this.getLatency());
			} catch (LatencyException le) {
				// log.error("$$$ " + le.getMessage());
				monitor.incrementLatencyErrors();
			}
		}
	}

	public void checkRemoteLatency(long endTimeMillis,
			BrowserClient remoteBrowser) throws InterruptedException,
			IOException {

		LatencyController cs = new LatencyController();
		cs.activateRemoteLatencyAssessmentIn(this, remoteBrowser);
		cs.checkLatency(endTimeMillis, TimeUnit.MILLISECONDS, monitor);

	}

	public void setMonitor(SystemMonitorManager monitor) {
		this.monitor = monitor;
	}

	public WebDriver getWebDriver() {
		return driver;
	}

}
