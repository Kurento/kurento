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
public class AddRemoveUsersRoomDemoTest extends BaseRoomDemoTest {

	private Logger log = LoggerFactory
			.getLogger(AddRemoveUsersRoomDemoTest.class);

	private static final int PLAY_TIME = 5; // seconds

	private static final int NUM_USERS = 4;
	private static final String ROOM_NAME = "room";

	protected static final int ITERATIONS = 2;

	@Test
	public void test() throws Exception {

		final boolean[] activeUsers = new boolean[NUM_USERS];
		final Object browsersLock = new Object();

		// parallelUsers(NUM_USERS, (numUser, browser) -> {

		parallelUsers(NUM_USERS, new UserLifecycle() {
			public void run(int numUser, final WebDriver browser)
					throws InterruptedException, ExecutionException {

				final String userName = "user" + numUser;

				for (int i = 0; i < ITERATIONS; i++) {

					sleep(numUser * 1000);

					synchronized (browsersLock) {
						joinToRoom(browser, userName, ROOM_NAME);
						log.info("User '{}' joined to room '{}'", userName,
								ROOM_NAME);
						activeUsers[numUser] = true;
						verify(browsers, activeUsers);
					}

					sleep(PLAY_TIME * 1000);

					synchronized (browsersLock) {

						log.info("User '{}' exiting from room '{}'", userName,
								ROOM_NAME);
						exitFromRoom(browser);
						log.info("User '{}' exited from room '{}'", userName,
								ROOM_NAME);
						activeUsers[numUser] = false;
						verify(browsers, activeUsers);
					}
				}

				// Scanner s = new Scanner(System.in).useDelimiter("\n");
				// s.next();

				log.info("User '{}' close browser", userName);
			}
		});
	}

}
