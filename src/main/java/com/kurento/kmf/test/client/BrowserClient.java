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

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

	public BrowserClient(int serverPort, Browser browser, Client client) {
		// Setup
		countDownLatchEvents = new HashMap<>();
		timeout = 100; // default (100 seconds)

		// Browser
		switch (browser) {
		case FIREFOX:
			setup(FirefoxDriver.class);
			break;
		case CHROME:
		default:
			setup(ChromeDriver.class);
			break;
		}
		driver.manage().timeouts().setScriptTimeout(timeout, TimeUnit.SECONDS);

		// Exercise test
		driver.get("http://localhost:" + serverPort + client.toString());
	}

	private void setup(Class<? extends WebDriver> driverClass) {
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
			driver = new ChromeDriver(options);
		}
	}

	public void setURL(String videoUrl) {
		this.videoUrl = videoUrl;
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

}
