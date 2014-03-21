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

import java.io.File;

import org.apache.commons.lang.SystemUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.kurento.test.base.BaseArquillianTst;

/**
 * Selenium tests for WebRtc features.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.1
 */
@RunWith(Arquillian.class)
public class WebRtcTst extends BaseArquillianTst {

	private WebDriver driver;

	private final String HTMLTEST = "webrtc.html";

	private final static int TIMEOUT = 80; // seconds

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

	public void webRtcTest(Class<? extends WebDriver> driverClass,
			String handler) throws InterruptedException {
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
		driver.findElement(By.id("start")).click();

		// Wait test result, watching field "status" (TIMEOUT seconds at the
		// most)
		(new WebDriverWait(driver, TIMEOUT))
				.until(new ExpectedCondition<Boolean>() {
					public Boolean apply(WebDriver d) {
						return d.findElement(By.id("status"))
								.getAttribute("value")
								.endsWith("Connection started");
					}
				});

		// Wait a guard time to see remote stream
		Thread.sleep(3000);
	}

	@Test
	public void testWebRtcLoopbackChrome() throws Exception {
		webRtcTest(ChromeDriver.class, "./webRtcLoopback");
	}

	@Test
	public void testWebRtcLoopbackJackVaderChrome() throws Exception {
		webRtcTest(ChromeDriver.class, "./webRtcJackVaderLoopback");
	}

	@Test
	public void testWebRtcMixerChrome() throws Exception {
		webRtcTest(ChromeDriver.class, "../dispatcher");
	}

}
