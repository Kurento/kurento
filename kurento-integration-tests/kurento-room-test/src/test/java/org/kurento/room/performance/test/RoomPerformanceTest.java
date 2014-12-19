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
package org.kurento.room.performance.test;

import static org.kurento.commons.PropertiesManager.getProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.room.demo.KurentoRoomServerApp;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserRunner;
import org.kurento.test.client.Client;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Room demo integration test.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = KurentoRoomServerApp.class)
@WebAppConfiguration
@IntegrationTest("server.port:"
		+ KurentoServicesTestHelper.APP_HTTP_PORT_DEFAULT)
public class RoomPerformanceTest extends PerformanceTest {

	private Logger log = LoggerFactory.getLogger(RoomPerformanceTest.class);

	private static final int DEFAULT_NODES = 1; // Number of nodes
	private static final int DEFAULT_NBROWSERS = 4; // Browser per node
	private static final int DEFAULT_CLIENT_RATE = 1000; // milliseconds
	private static final int DEFAULT_HOLD_TIME = 10000; // milliseconds

	private static final String ROOM_NAME = "room";

	private int holdTime;

	public RoomPerformanceTest() {

		int numNodes = getProperty("test.webrtcgrid.numnodes", DEFAULT_NODES);

		int numBrowsers = getProperty("test.webrtcgrid.numbrowsers",
				DEFAULT_NBROWSERS);

		holdTime = getProperty("test.webrtcgrid.holdtime", DEFAULT_HOLD_TIME);

		setNumBrowsersPerNode(numBrowsers);

		setBrowserCreationRate(getProperty("test.webrtcgrid.clientrate",
				DEFAULT_CLIENT_RATE));

		setNodes(getRandomNodes(numNodes, Browser.CHROME, getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m", null, numBrowsers));
	}

	protected void joinToRoom(BrowserClient browser, String userName,
			String roomName) {

		WebDriver driver = browser.getWebDriver();

		driver.findElement(By.id("name")).sendKeys(userName);
		driver.findElement(By.id("roomName")).clear();
		driver.findElement(By.id("roomName")).sendKeys(roomName);
		((JavascriptExecutor) driver).executeScript("register()");

		log.info("User '" + userName + "' joined to room '" + roomName + "'");
	}

	protected void exitFromRoom(BrowserClient browser) {
		try {
			browser.getWebDriver().findElement(By.id("button-leave")).click();
		} catch (ElementNotVisibleException e) {
			log.warn("Button leave is not visible. Session can't be closed");
		}
	}

	@Test
	public void test() throws Exception {
		parallelBrowsers(new BrowserRunner() {
			public void run(BrowserClient browser, int num, String name)
					throws Exception {

				final String userName = "user" + num;

				joinToRoom(browser, userName, ROOM_NAME);

				log.info("User '{}' joined to room '{}'", userName, ROOM_NAME);

				Thread.sleep(holdTime);

				log.info("User '{}' exiting from room '{}'", userName,
						ROOM_NAME);
				exitFromRoom(browser);
				log.info("User '{}' exited from room '{}'", userName, ROOM_NAME);
			}
		}, Client.ROOM);
	}
}
