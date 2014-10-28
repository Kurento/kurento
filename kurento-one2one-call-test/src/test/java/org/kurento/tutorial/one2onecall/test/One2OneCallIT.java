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
package org.kurento.tutorial.one2onecall.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.tutorial.one2onecall.One2OneCallApp;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * One to one call integration test.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = One2OneCallApp.class)
@WebAppConfiguration
@IntegrationTest
public class One2OneCallIT {

	protected WebDriver caller;
	protected WebDriver callee;

	protected final static int TEST_TIMEOUT = 60; // seconds
	protected final static int PLAY_TIME = 5; // seconds
	protected final static String APP_URL = "http://localhost:8080/";
	protected final static String CALLER_NAME = "user1";
	protected final static String CALLEE_NAME = "user2";

	@Before
	public void setup() {
		caller = newWebDriver();
		callee = newWebDriver();
	}

	private WebDriver newWebDriver() {
		ChromeOptions options = new ChromeOptions();
		// This flag avoids a warning in Chrome. See:
		// https://code.google.com/p/chromedriver/issues/detail?id=799
		options.addArguments("--test-type");
		// This flag avoids granting camera/microphone
		options.addArguments("--use-fake-ui-for-media-stream");
		// This flag makes using a synthetic video (green with spinner) in
		// WebRTC instead of real media from camera/microphone
		options.addArguments("--use-fake-device-for-media-stream");

		return new ChromeDriver(options);
	}

	@Test
	public void testOne2One() throws InterruptedException {
		// Open caller web application
		caller.get(APP_URL);

		// Register caller
		caller.findElement(By.id("name")).sendKeys(CALLER_NAME);
		caller.findElement(By.id("register")).click();

		// Open callee web application
		callee.get(APP_URL);

		// Register caller
		callee.findElement(By.id("name")).sendKeys(CALLEE_NAME);
		callee.findElement(By.id("register")).click();

		// Caller calls callee
		caller.findElement(By.id("peer")).sendKeys(CALLEE_NAME);
		caller.findElement(By.id("call")).click();

		// Callee accepts call
		waitForIncomingCallDialog(callee);
		callee.switchTo().alert().accept();

		// Assessments: local and remote video tags of caller and callee should
		// play media
		waitForStream(caller, "videoInput");
		waitForStream(caller, "videoOutput");
		waitForStream(callee, "videoInput");
		waitForStream(callee, "videoOutput");

		// Guard time to see application in action
		Thread.sleep(PLAY_TIME * 1000);

		// Stop application by caller
		caller.findElement(By.id("terminate")).click();
	}

	private void waitForStream(WebDriver driver, String videoTagId)
			throws InterruptedException {
		WebElement video = driver.findElement(By.id(videoTagId));
		int i = 0;
		for (; i < TEST_TIMEOUT; i++) {
			if (video.getAttribute("src").startsWith("blob")) {
				break;
			} else {
				Thread.sleep(1000);
			}
		}
		if (i == TEST_TIMEOUT) {
			Assert.fail("Video tag '" + videoTagId + "' is not playing media");
		}
	}

	private void waitForIncomingCallDialog(WebDriver driver)
			throws InterruptedException {
		int i = 0;
		for (; i < TEST_TIMEOUT; i++) {
			try {
				driver.switchTo().alert();
				break;
			} catch (NoAlertPresentException e) {
				Thread.sleep(1000);
			}
		}
		if (i == TEST_TIMEOUT) {
			throw new RuntimeException("Timeout (" + TEST_TIMEOUT
					+ " seconds) waiting for incoming call");
		}
	}

	@After
	public void end() {
		caller.close();
		callee.close();
	}
}
