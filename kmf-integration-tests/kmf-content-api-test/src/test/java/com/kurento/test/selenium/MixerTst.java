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
package com.kurento.test.selenium;

import java.io.File;

import org.apache.commons.lang.SystemUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.kurento.test.base.BaseArquillianTst;

/**
 * Selenium tests for Media Mixer features.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.1.1
 */
@RunWith(Arquillian.class)
public class MixerTst extends BaseArquillianTst {

	public final static int TIMEOUT = 25; // seconds

	private WebDriver createDriver(Class<? extends WebDriver> driverClass) {
		WebDriver driver = null;
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

			// This flag avoids a warning in chrome. See:
			// https://code.google.com/p/chromedriver/issues/detail?id=799
			options.addArguments("--test-type");

			// This flag avoids grant the camera
			options.addArguments("--use-fake-ui-for-media-stream");

			// This flag makes using a synthetic video (green with
			// spinner) in webrtc. Or it is needed to combine with
			// use-file-for-fake-video-capture to use a file faking the
			// cam
			options.addArguments("--use-fake-device-for-media-stream");

			driver = new ChromeDriver(options);
		}
		return driver;
	}

	public void dispatcherTest(WebDriver driver, final String clientPage,
			String userName) {
		RunnableMixerTst runnableTst = new RunnableMixerTst(driver, clientPage,
				userName);
		runnableTst.run();
	}

	@Test
	public void testDispatcherChrome() throws Exception {
		WebDriver driver = createDriver(ChromeDriver.class);
		dispatcherTest(driver, "mixer/dispatcher.html", "user1");

		// Teardown
		driver.quit();
		driver = null;
	}

	@Test
	public void testCompositeChrome() throws Exception {
		WebDriver driver = createDriver(ChromeDriver.class);
		dispatcherTest(driver, "mixer/compositeWebRTC.html", "user1");

		// Teardown
		driver.quit();
		driver = null;
	}

	class RunnableMixerTst implements Runnable {
		private WebDriver driver;
		private String clientPage;
		private String userName;

		public RunnableMixerTst(WebDriver driver, String clientPage,
				String userName) {
			this.driver = driver;
			this.clientPage = clientPage;
			this.userName = userName;

		}

		@Override
		public void run() {
			driver.get("http://localhost:" + getServerPort()
					+ "/kmf-content-api-test/" + clientPage);

			WebElement name = driver.findElement(By.id("name"));
			name.sendKeys(userName);
			driver.findElement(By.id("start")).click();

			// Wait user to be joined
			(new WebDriverWait(driver, TIMEOUT))
					.until(new ExpectedCondition<Boolean>() {
						public Boolean apply(WebDriver d) {
							return d.findElement(By.id("console")).getText()
									.contains(userName + " has joined");
						}
					});

			// Wait button to connect user
			(new WebDriverWait(driver, TIMEOUT))
					.until(new ExpectedCondition<Boolean>() {
						public Boolean apply(WebDriver d) {
							return d.findElement(By.linkText(userName))
									.getText().contains(userName);
						}
					});

			driver.findElement(By.linkText(userName)).click();

			try {
				// 3 seconds guard (waiting for remote connection)
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			driver.findElement(By.id("terminate")).click();
		}
	}
}
