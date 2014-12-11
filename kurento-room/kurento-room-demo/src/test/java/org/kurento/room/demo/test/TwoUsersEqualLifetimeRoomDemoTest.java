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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.room.demo.KurentoRoomServerApp;
import org.openqa.selenium.WebDriver;
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
public class TwoUsersEqualLifetimeRoomDemoTest extends BaseRoomDemoTest {

	private static final int PLAY_TIME = 5; // seconds

	private static final String USER1_NAME = "user1";
	private static final String USER2_NAME = "user2";
	private static final String ROOM_NAME = "room";

	private WebDriver user1Browser;
	private WebDriver user2Browser;

	@Before
	public void setup() {
		user1Browser = newWebDriver();
		user2Browser = newWebDriver();
	}

	@Test
	public void twoUsersRoomTest() throws InterruptedException {

		joinToRoom(user1Browser, USER1_NAME, ROOM_NAME);
		joinToRoom(user2Browser, USER2_NAME, ROOM_NAME);

		waitForStream(user1Browser, "video-" + USER1_NAME);
		waitForStream(user2Browser, "video-" + USER2_NAME);

		// Guard time to see application in action
		Thread.sleep(PLAY_TIME * 1000);

		// Stop application by caller
		exitFromRoom(user1Browser);
		exitFromRoom(user2Browser);
	}

	@After
	public void end() {
		user1Browser.close();
		user2Browser.close();
	}
}
