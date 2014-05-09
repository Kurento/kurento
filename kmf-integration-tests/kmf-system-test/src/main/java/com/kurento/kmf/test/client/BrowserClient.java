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

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
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
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.WebRtcEndpoint;

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

	public BrowserClient(int serverPort, Browser browser, Client client) {
		// Setup
		countDownLatchEvents = new HashMap<>();
		timeout = 60; // default (60 seconds)
		maxDistance = 300.0; // default distance (for color comparison)

		// Browser
		switch (browser) {
		case FIREFOX:
			setup(FirefoxDriver.class, false);
			break;
		case CHROME:
			setup(ChromeDriver.class, false);
			break;
		case CHROME_FOR_TEST:
		default:
			setup(ChromeDriver.class, true);
			break;
		}
		driver.manage().timeouts().setScriptTimeout(timeout, TimeUnit.SECONDS);

		// Exercise test
		driver.get("http://localhost:" + serverPort + client.toString());
	}

	private void setup(Class<? extends WebDriver> driverClass, boolean flags) {
		if (driverClass.equals(FirefoxDriver.class)) {
			driver = new FirefoxDriver();

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
			if (flags) {
				options.addArguments("--disable-web-security",
						"--use-fake-device-for-media-stream",
						"--use-fake-ui-for-media-stream");
			}
			driver = new ChromeDriver(options);
		}
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
		return Double.parseDouble(driver.findElement(By.id("currentTime"))
				.getAttribute("value"));
	}

	public boolean colorSimilarTo(Color expectedColor) {
		String[] realColor = driver.findElement(By.id("color"))
				.getAttribute("value").split(",");
		int red = Integer.parseInt(realColor[0]);
		int green = Integer.parseInt(realColor[1]);
		int blue = Integer.parseInt(realColor[2]);

		double distance = (red - expectedColor.getRed())
				* (red - expectedColor.getRed())
				+ (green - expectedColor.getGreen())
				* (green - expectedColor.getGreen())
				+ (blue - expectedColor.getBlue())
				* (blue - expectedColor.getBlue());

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
			((JavascriptExecutor) driver).executeScript("getSdpOffer("
					+ channel.getAudio() + "," + channel.getVideo() + ");");

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

}
