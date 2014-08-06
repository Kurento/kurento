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

package org.kurento.kmf.repository.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kurento.kmf.repository.HttpSessionErrorEvent;
import org.kurento.kmf.repository.HttpSessionStartedEvent;
import org.kurento.kmf.repository.RepositoryHttpEventListener;
import org.kurento.kmf.repository.RepositoryHttpRecorder;
import org.kurento.kmf.repository.RepositoryItem;
import org.kurento.kmf.repository.test.util.HttpRepositoryTest;

public class ErrorEventsTest extends HttpRepositoryTest {

	private static final Logger log = LoggerFactory
			.getLogger(ErrorEventsTest.class);

	@Test
	public void testFileUploadAndDownload() throws Exception {

		RepositoryItem item = getRepository().createRepositoryItem();

		final RepositoryHttpRecorder recorder = item
				.createRepositoryHttpRecorder();

		final CountDownLatch started = new CountDownLatch(1);
		recorder.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
			@Override
			public void onEvent(HttpSessionStartedEvent event) {
				started.countDown();
			}
		});

		final CountDownLatch errorLatch = new CountDownLatch(1);
		recorder.addSessionErrorListener(new RepositoryHttpEventListener<HttpSessionErrorEvent>() {
			@Override
			public void onEvent(HttpSessionErrorEvent event) {
				log.info("Error event sent");
				log.info("Exception:" + event.getCause());
				errorLatch.countDown();
			}
		});

		log.info("Start writing to URL " + recorder.getURL()
				+ " the item with id '" + item.getId() + "'");

		new Thread() {
			public void run() {
				try {
					uploadFileWithPOST(recorder.getURL(), new File(
							"test-files/logo.png"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		started.await();

		// Sleep to give time to open the outputStream to write the uploading
		// file.
		Thread.sleep(2000);

		getRepository().remove(item);

		assertTrue(
				"Error event was not fired in the next 5 seconds before deletion of the file",
				errorLatch.await(5, TimeUnit.SECONDS));

	}

}