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
package org.kurento.tutorial.crowddetector.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.demo.CrowdDetectorApp;
import org.kurento.test.client.BrowserClient;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import io.github.bonigarcia.wdm.ChromeDriverManager;

/**
 * Hello World integration test.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CrowdDetectorApp.class)
@WebAppConfiguration
@IntegrationTest
public class CrowdDetectorIT {

	protected WebDriver driver;
	protected static final Logger log = LoggerFactory.getLogger(CrowdDetectorIT.class);

	protected final static int TEST_TIMEOUT = 60; // seconds
	protected final static int ALERT_TIMEOUT = 10; // seconds
	protected final static int PLAY_TIME = 5; // seconds

	@BeforeClass
	public static void setupClass() {
		ChromeDriverManager.getInstance().setup();
	}

	@Before
	public void setup() {
		ChromeOptions options = new ChromeOptions();
		// This flag avoids granting camera/microphone
		options.addArguments("--use-fake-ui-for-media-stream");
		// This flag makes using a synthetic video (green with spinner) in
		// WebRTC instead of real media from camera/microphone
		options.addArguments("--use-fake-device-for-media-stream");

		driver = BrowserClient.newWebDriver(options);
	}

	@Test
	public void testCrowdDetector() throws InterruptedException {
		// Open web application
		driver.get("http://localhost:8080/");

		// Start application
		driver.findElement(By.id("start")).click();

		// Check alert
		WebDriverWait wait = new WebDriverWait(driver, ALERT_TIMEOUT);
		try {
			if (wait.until(ExpectedConditions.alertIsPresent()) != null) {
				driver.switchTo().alert().accept();
				driver.findElement(By.id("address")).sendKeys("rtsp://195.55.223.100/axis-media/media.amp");
				driver.findElement(By.id("changeFeed")).click();
				if (wait.until(ExpectedConditions.alertIsPresent()) != null) {
					driver.switchTo().alert().accept();
					driver.findElement(By.id("start")).click();
				}
			}
		} catch (TimeoutException te) {
			log.warn(te.getMessage());
		}

		// Assessment #1: Remote video tag should play media
		waitForStream("videoOutput");

		// Guard time to see application in action
		Thread.sleep(PLAY_TIME * 1000);

		// Stop application
		driver.findElement(By.id("stop")).click();
	}

	private void waitForStream(String videoTagId) throws InterruptedException {
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

	@After
	public void end() {
		if (driver != null) {
			driver.close();
		}
	}
}
