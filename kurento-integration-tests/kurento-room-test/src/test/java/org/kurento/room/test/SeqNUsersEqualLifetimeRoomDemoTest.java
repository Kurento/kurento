package org.kurento.room.test;

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

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.room.demo.KurentoRoomServerApp;
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
@IntegrationTest
public class SeqNUsersEqualLifetimeRoomDemoTest extends BaseRoomDemoTest {

	private Logger log = LoggerFactory
			.getLogger(SeqNUsersEqualLifetimeRoomDemoTest.class);

	private static final int PLAY_TIME = 5; // seconds

	private static final String USER1_NAME = "user1";
	private static final String USER2_NAME = "user2";
	private static final String ROOM_NAME = "room";

	@Test
	public void twoUsersRoomTest() throws InterruptedException,
			ExecutionException {

		List<WebDriver> browsers = createBrowsers(2);

		try {

			joinToRoom(browsers.get(0), USER1_NAME, ROOM_NAME);
			joinToRoom(browsers.get(1), USER2_NAME, ROOM_NAME);

			waitForStream(browsers.get(0), "native-video-" + USER2_NAME);
			log.debug("Received media from " + USER2_NAME + " in " + USER1_NAME);

			waitForStream(browsers.get(1), "native-video-" + USER1_NAME);
			log.debug("Received media from " + USER1_NAME + " in " + USER2_NAME);

			// Guard time to see application in action
			Thread.sleep(PLAY_TIME * 1000);

			// Stop application by caller
			exitFromRoom(browsers.get(0));
			exitFromRoom(browsers.get(1));

		} finally {

			closeBrowsers(browsers);
		}
	}
}
