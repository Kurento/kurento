/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.repository.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.test.util.HttpRepositoryTest;

public class TimeoutTests extends HttpRepositoryTest {

	private static final Logger log = LoggerFactory
			.getLogger(TimeoutTests.class);

	@Test
	public void playerAutoTerminationTest() throws Exception {

		String id = uploadFile(new File("test-files/sample.txt"));

		log.info("File uploaded");

		RepositoryHttpPlayer player = getRepository()
				.findRepositoryItemById(id).createRepositoryHttpPlayer();

		player.setAutoTerminationTimeout(1000);

		RestTemplate template = getRestTemplate();

		assertEquals(HttpStatus.OK,
				template.getForEntity(player.getURL(), byte[].class)
						.getStatusCode());
		log.info("Request 1 Passed");

		Thread.sleep(300);

		assertEquals(HttpStatus.OK,
				template.getForEntity(player.getURL(), byte[].class)
						.getStatusCode());
		log.info("Request 2 Passed");

		Thread.sleep(1500);

		assertEquals(HttpStatus.NOT_FOUND,
				template.getForEntity(player.getURL(), byte[].class)
						.getStatusCode());
		log.info("Request 3 Passed");

	}

}