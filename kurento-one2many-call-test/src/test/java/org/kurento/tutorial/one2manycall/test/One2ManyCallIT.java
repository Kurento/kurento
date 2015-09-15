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
 */
package org.kurento.tutorial.one2manycall.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.test.client.BrowserClient;
import org.kurento.tutorial.one2manycall.One2ManyCallApp;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import io.github.bonigarcia.wdm.ChromeDriverManager;

/**
 * One to many call integration test.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = One2ManyCallApp.class)
@WebAppConfiguration
@IntegrationTest
public class One2ManyCallIT {

	protected WebDriver master;
	protected List<WebDriver> viewers;

	protected final static int TEST_TIMEOUT = 200; // seconds
	protected final static int PLAY_TIME = 5; // seconds
	protected final static String DEFAULT_NUM_VIEWERS = "3";
	protected final static String APP_URL = "http://localhost:8080/";

	@BeforeClass
	public static void setupClass() {
		ChromeDriverManager.getInstance().setup();
	}

	@Before
	public void setup() {
		master = newWebDriver();

		final int numViewers = Integer.parseInt(System.getProperty("test.num.viewers", DEFAULT_NUM_VIEWERS));
		viewers = new ArrayList<>(numViewers);
		for (int i = 0; i < numViewers; i++) {
			viewers.add(newWebDriver());
		}
	}

	private static WebDriver newWebDriver() {
		ChromeOptions options = new ChromeOptions();
		// This flag avoids granting camera/microphone
		options.addArguments("--use-fake-ui-for-media-stream");
		// This flag makes using a synthetic video (green with spinner) in
		// WebRTC instead of real media from camera/microphone
		options.addArguments("--use-fake-device-for-media-stream");

		return BrowserClient.newChromeDriver(options);
	}

	@Test
	public void testOne2Many() throws InterruptedException {
		// MASTER
		// Open web application
		master.get(APP_URL);

		// Start application as master
		master.findElement(By.id("call")).click();

		// Assessment #1: Master video tag should play media
		waitForStream(master, "video");

		// VIEWERS
		for (WebDriver viewer : viewers) {
			// Open web application
			viewer.get(APP_URL);

			// Start application as viewer
			viewer.findElement(By.id("viewer")).click();

			// Assessment #2: Viewer video tag should play media
			waitForStream(viewer, "video");
		}

		// Guard time to see application in action
		Thread.sleep(PLAY_TIME * 1000);

		// Stop application (master)
		master.findElement(By.id("terminate")).click();
	}

	private static void waitForStream(WebDriver driver, String videoTagId) throws InterruptedException {
		WebElement video = driver.findElement(By.id(videoTagId));
		int i = 0;
		for (; i < TEST_TIMEOUT; i++) {
			if (video.getAttribute("src").startsWith("blob")) {
				break;
			}

			Thread.sleep(1000);

		}
		if (i == TEST_TIMEOUT) {
			Assert.fail("Video tag '" + videoTagId + "' is not playing media after " + TEST_TIMEOUT + " seconds");
		}
	}

	@After
	public void end() {
		for (WebDriver viewer : viewers) {
			if (viewer != null) {
				viewer.close();
			}
		}
		if (master != null) {
			master.close();
		}
	}
}
