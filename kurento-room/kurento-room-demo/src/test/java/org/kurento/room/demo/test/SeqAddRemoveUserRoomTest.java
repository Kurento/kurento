package org.kurento.room.demo.test;

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
public class SeqAddRemoveUserRoomTest extends BaseRoomDemoTest {

	private static final int WAIT_TIME = 500;

	private Logger log = LoggerFactory
			.getLogger(SeqAddRemoveUserRoomTest.class);

	private static final int PLAY_TIME = 5; // seconds

	private static final String ROOM_NAME = "room";

	private static final int NUM_USERS = 4;

	private static final int NUM_ITERATIONS = 2;

	@Test
	public void nUsersRoomTest() throws InterruptedException,
			ExecutionException {

		boolean[] activeUsers = new boolean[NUM_USERS];

		List<WebDriver> browsers = createBrowsers(NUM_USERS);

		try {

			for (int cycle = 0; cycle < NUM_ITERATIONS; cycle++) {

				for (int i = 0; i < NUM_USERS; i++) {
					String userName = "user" + i;
					joinToRoom(browsers.get(i), userName, ROOM_NAME);
					activeUsers[i] = true;
					sleep(WAIT_TIME);
					verify(browsers, activeUsers);
				}

				for (int i = 0; i < NUM_USERS; i++) {
					for (int j = 0; j < NUM_USERS; j++) {
						waitForStream(browsers.get(i), "video-user" + j);
						log.debug("Received media from user" + j + " in user"
								+ i);
					}
				}

				// Guard time to see application in action
				Thread.sleep(PLAY_TIME * 1000);

				// Stop application by caller
				for (int i = 0; i < NUM_USERS; i++) {
					exitFromRoom(browsers.get(i));
					activeUsers[i] = false;
					sleep(WAIT_TIME);
					verify(browsers, activeUsers);
				}
			}

		} finally {
			closeBrowsers(browsers);
		}
	}

}
