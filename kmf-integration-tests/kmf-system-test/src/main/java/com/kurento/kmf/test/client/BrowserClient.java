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
package com.kurento.kmf.test.client;

import static com.kurento.kmf.common.PropertiesManager.getProperty;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
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

import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.factory.KmfMediaApiProperties;
import com.kurento.kmf.test.base.GridBrowserMediaApiTest;
import com.kurento.kmf.test.services.AudioChannel;
import com.kurento.kmf.test.services.KurentoServicesTestHelper;
import com.kurento.kmf.test.services.Node;
import com.kurento.kmf.test.services.Recorder;

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
	private String videoUrl;
	private int timeout; // seconds
	private double maxDistance;

	private String video;
	private String audio;
	private int serverPort;
	private Client client;
	private Browser browser;
	private boolean usePhysicalCam;
	private Node remoteNode;
	private int recordAudio;
	private int audioSampleRate;
	private AudioChannel audioChannel;

	private BrowserClient(Builder builder) {
		this.video = builder.video;
		this.audio = builder.audio;
		this.serverPort = builder.serverPort;
		this.client = builder.client;
		this.browser = builder.browser;
		this.usePhysicalCam = builder.usePhysicalCam;
		this.remoteNode = builder.remoteNode;
		this.recordAudio = builder.recordAudio;
		this.audioSampleRate = builder.audioSampleRate;
		this.audioChannel = builder.audioChannel;

		countDownLatchEvents = new HashMap<>();
		timeout = 60; // default (60 seconds)
		maxDistance = 60.0; // default distance (for color comparison)

		String hostAddress = KmfMediaApiProperties.getThriftKmfAddress()
				.getHost();

		// Setup Selenium
		initDriver(hostAddress);

		// Launch Browser
		driver.manage().timeouts();
		driver.get("http://" + hostAddress + ":" + serverPort
				+ client.toString());
	}

	private void initDriver(String hostAddress) {
		Class<? extends WebDriver> driverClass = browser.getDriverClass();
		int hubPort = getProperty("test.hub.port",
				GridBrowserMediaApiTest.DEFAULT_HUB_PORT);

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

				// This flag avoids grant the camera
				options.addArguments("--use-fake-ui-for-media-stream");

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
						options.addArguments("--use-file-for-fake-video-capture="
								+ video);

						// Alternative: lauch fake cam also in Chrome
						// launchFakeCam();
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
			driver.manage().timeouts()
					.setScriptTimeout(timeout, TimeUnit.SECONDS);

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

	public void setColorCoordinates(int x, int y) {
		driver.findElement(By.id("x")).clear();
		driver.findElement(By.id("y")).clear();
		driver.findElement(By.id("x")).sendKeys(String.valueOf(x));
		driver.findElement(By.id("y")).sendKeys(String.valueOf(y));
	}

	public void subscribeEvents(String... eventType) {
		for (final String e : eventType) {
			CountDownLatch latch = new CountDownLatch(1);
			countDownLatchEvents.put(e, latch);
			this.addEventListener(e, new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("Event: {}", event);
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

	public void addEventListener(final String eventType,
			final EventListener eventListener) {
		Thread t = new Thread() {
			public void run() {
				((JavascriptExecutor) driver)
						.executeScript("video.addEventListener('" + eventType
								+ "', videoEvent, false);");
				(new WebDriverWait(driver, timeout))
						.until(new ExpectedCondition<Boolean>() {
							public Boolean apply(WebDriver d) {
								return d.findElement(By.id("status"))
										.getAttribute("value")
										.equalsIgnoreCase(eventType);
							}
						});
				eventListener.onEvent(eventType);
			}
		};
		callbackThreads.add(t);
		t.setDaemon(true);
		t.start();
	}

	public void start() {
		if (driver instanceof JavascriptExecutor) {
			((JavascriptExecutor) driver).executeScript("play('" + videoUrl
					+ "', false);");
		}
	}

	public void showSpinners() {
		if (driver instanceof JavascriptExecutor) {
			((JavascriptExecutor) driver)
					.executeScript("showSpinner('local');");
			((JavascriptExecutor) driver)
					.executeScript("showSpinner('video');");
		}
	}

	public void stop() {
		if (driver instanceof JavascriptExecutor) {
			((JavascriptExecutor) driver).executeScript("terminate();");
		}
	}

	public void startRcvOnly() {
		if (driver instanceof JavascriptExecutor) {
			((JavascriptExecutor) driver).executeScript("play('" + videoUrl
					+ "', true);");
		}
	}

	@SuppressWarnings("deprecation")
	public void close() {
		for (Thread t : callbackThreads) {
			t.stop();
		}
		driver.quit();
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

	public boolean color(Color expectedColor, final double seconds, int x, int y) {
		// Wait to be in the right time
		(new WebDriverWait(driver, timeout))
				.until(new ExpectedCondition<Boolean>() {
					public Boolean apply(WebDriver d) {
						double time = Double.parseDouble(d.findElement(
								By.id("currentTime")).getAttribute("value"));
						return time > seconds;
					}
				});

		setColorCoordinates(x, y);
		// Guard time to wait JavaScript function to detect the color (otherwise
		// race conditions could appear)
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			log.trace("InterruptedException in guard condition ({})",
					e.getMessage());
		}
		return colorSimilarTo(expectedColor);
	}

	public boolean colorSimilarTo(Color expectedColor) {
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

		log.info("Color comparision: real {}, expected {}, distance {}",
				realColor, expectedColor, distance);

		return distance <= getMaxDistance();
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public void connectToWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint,
			WebRtcChannel channel) {
		if (driver instanceof JavascriptExecutor) {
			String getSdpOffer = "getSdpOffer(" + channel.getAudio() + ","
					+ channel.getVideo();
			if (audio != null) {
				getSdpOffer += ",'" + audio + "');";
			} else {
				getSdpOffer += ");";
			}

			((JavascriptExecutor) driver).executeScript(getSdpOffer);

			// Wait to valid sdpOffer
			(new WebDriverWait(driver, timeout))
					.until(new ExpectedCondition<Boolean>() {
						public Boolean apply(WebDriver d) {
							return ((JavascriptExecutor) driver)
									.executeScript("return sdpOffer;") != null;
						}
					});
			String sdpOffer = (String) ((JavascriptExecutor) driver)
					.executeScript("return sdpOffer;");
			String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

			// Encode to base64 to avoid parsing error in Javascript due to
			// break lines
			sdpAnswer = new String(Base64.encodeBase64(sdpAnswer.getBytes()));

			((JavascriptExecutor) driver).executeScript("setSdpAnswer('"
					+ sdpAnswer + "');");
		}
	}

	public static class Builder {
		private String video;
		private String audio;
		private int serverPort;
		private Client client;
		private Browser browser;
		private boolean usePhysicalCam;
		private Node remoteNode;
		private int recordAudio; // seconds
		private int audioSampleRate; // samples per seconds (e.g. 8000, 16000)
		private AudioChannel audioChannel; // stereo, mono

		public Builder() {
			this.serverPort = KurentoServicesTestHelper.getAppHttpPort();

			// By default physical camera will not be used; instead synthetic
			// videos will be used for testing
			this.usePhysicalCam = false;

			// By default is not a remote test
			this.remoteNode = null;

			// By default, not recording audio (0 seconds)
			this.recordAudio = 0;
		}

		public Builder(int serverPort) {
			this.serverPort = serverPort;
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
}
