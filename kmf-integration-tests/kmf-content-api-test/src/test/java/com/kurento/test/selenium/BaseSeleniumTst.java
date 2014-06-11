/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.test.selenium;

import static com.kurento.demo.internal.EventListener.HANDLER_ON_CONTENT_COMMAND;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.kurento.demo.internal.EventListener;
import com.kurento.test.base.BaseArquillianTst;

/**
 * Base class for Selenium tests; it contains the generic test cases for this
 * kind of tests, literals for content events (for JavaScript and Handler
 * sides), and startup/release for Selenium Webdriver.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see <a href="http://www.seleniumhq.org/">Selenium</a>
 */
public class BaseSeleniumTst extends BaseArquillianTst {

	private WebDriver driver;

	private final String HTMLTEST = "playerJson.html";

	private int timeout = 80; // seconds
	private boolean waitEnd = true;
	private String expectedStatus = null;

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isWaitEnd() {
		return waitEnd;
	}

	public void setWaitEnd(boolean waitEnd) {
		this.waitEnd = waitEnd;
	}

	public String getExpectedStatus() {
		return expectedStatus;
	}

	public void setExpectedStatus(String expectedStatus) {
		this.expectedStatus = expectedStatus;
	}

	public void seleniumTest(Class<? extends WebDriver> driverClass,
			String handler, String video) throws IllegalArgumentException,
			SecurityException, IllegalAccessException, NoSuchFieldException,
			InterruptedException {
		seleniumTest(driverClass, handler, video, null, null, null);
	}

	public void seleniumTest(Class<? extends WebDriver> driverClass,
			String handler, String video, String[] expectedEvents)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException, InterruptedException {
		seleniumTest(driverClass, handler, video, null, null, expectedEvents);
	}

	public void seleniumTest(Class<? extends WebDriver> driverClass,
			String handler, String video, String[] expectedHandlerFlow,
			String[] expectedJavaScriptFlow, String[] expectedEvents)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException, InterruptedException {
		// Setup
		setup(driverClass);
		Assert.assertNotNull("Null web driver for " + driverClass.getName(),
				driver);

		// Exercise test
		driver.get("http://localhost:" + getServerPort()
				+ "/kmf-content-api-test/" + HTMLTEST);
		if (handler != null) {
			Select handlerSelect = new Select(driver.findElement(By
					.id("handler")));
			handlerSelect.selectByValue(handler);
		}
		if (video != null) {
			Select videoSelect = new Select(driver.findElement(By.id("url")));
			videoSelect.selectByValue(video);
		}
		driver.findElement(By.id("play")).click();

		// Specific case: send commands
		if (expectedHandlerFlow != null
				&& Arrays.asList(expectedHandlerFlow).contains(
						HANDLER_ON_CONTENT_COMMAND)) {
			// Wait the video to be started (TIMEOUT seconds at the most)
			(new WebDriverWait(driver, timeout))
					.until(new ExpectedCondition<Boolean>() {
						@Override
						public Boolean apply(WebDriver d) {
							return d.findElement(By.id("status"))
									.getAttribute("value").startsWith("play");
						}
					});
			driver.findElement(By.id("sendCommands")).click();
		}

		// Wait test result, watching field "status" (TIMEOUT seconds at the
		// most)
		try {
			(new WebDriverWait(driver, timeout))
					.until(new ExpectedCondition<Boolean>() {
						@Override
						public Boolean apply(WebDriver d) {
							return d.findElement(By.id("status"))
									.getAttribute("value").startsWith("end");
						}
					});
		} catch (WebDriverException e) {
			if (!isWaitEnd()) {
				driver.findElement(By.id("stop")).click();
			} else {
				throw e;
			}
		}

		// FIXME: Dirty hack to avoid polling not receiving onTerminate event
		// See kmf-content-api class AbstractContentSession, line 796
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		if (expectedHandlerFlow != null) {
			final List<String> actualEventList = EventListener.getEventList();
			log.info("*** Flow (Handler) *** Actual: "
					+ Arrays.asList(actualEventList) + " ... Expected: "
					+ Arrays.asList(expectedHandlerFlow));
			Assert.assertArrayEquals(expectedHandlerFlow,
					actualEventList.toArray());
		}
		if (expectedJavaScriptFlow != null) {
			final String flows = driver.findElement(By.id("flows"))
					.getAttribute("value");
			String[] actualFlows = StringUtils.split(flows, "\n");
			log.info("*** Flow (JavaScript) *** Actual: "
					+ Arrays.asList(actualFlows) + " ... Expected: "
					+ Arrays.asList(expectedJavaScriptFlow));
			Assert.assertFalse(flows.isEmpty());
		}
		if (expectedEvents != null) {
			final String events = driver.findElement(By.id("events"))
					.getAttribute("value");
			String[] actualEvents = StringUtils.split(events, "\n");
			log.info("*** Events *** Actual: " + Arrays.asList(actualEvents)
					+ " ... Expected: " + Arrays.asList(expectedEvents));
			Assert.assertTrue(actualEvents != null);
		}

		if (expectedHandlerFlow == null || expectedJavaScriptFlow == null) {
			// Assert status does not contain "error"
			final String status = driver.findElement(By.id("status"))
					.getAttribute("value");
			if (expectedStatus != null) {
				Assert.assertEquals(expectedStatus, status);
			} else {
				Assert.assertTrue(status, !status.contains("error"));
			}
		}
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

	@After
	public void end() {
		driver.quit();
		driver = null;
	}
}
