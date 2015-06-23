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
package org.kurento.tutorial.pointerdetector.test;

import io.github.bonigarcia.wdm.ChromeDriverManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.tutorial.pointerdetector.PointerDetectorApp;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Hello World integration test.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PointerDetectorApp.class)
@WebAppConfiguration
@IntegrationTest
public class PointerDetectorIT {

	protected WebDriver driver;

	protected final static int TEST_TIMEOUT = 60; // seconds
	protected final static int PLAY_TIME = 5; // seconds

	@BeforeClass
	public static void setupClass() {
		new ChromeDriverManager().setup();
	}

	@Before
	public void setup() {
		ChromeOptions options = new ChromeOptions();
		// This flag avoids granting camera/microphone
		options.addArguments("--use-fake-ui-for-media-stream");
		// This flag makes using a synthetic video (green with spinner) in
		// WebRTC instead of real media from camera/microphone
		options.addArguments("--use-fake-device-for-media-stream");

		driver = new ChromeDriver(options);
	}

	@Test
	public void testHelloWorld() throws InterruptedException {
		// Open web application
		driver.get("http://localhost:8080/");

		// Start application
		driver.findElement(By.id("start")).click();

		// Assessment #1: Local video tag should play media
		waitForStream("videoInput");

		// Assessment #2: Remote video tag should play media
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
		driver.close();
	}
}
