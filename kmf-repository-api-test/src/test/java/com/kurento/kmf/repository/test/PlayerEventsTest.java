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

package com.kurento.kmf.repository.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.kurento.kmf.repository.HttpSessionStartedEvent;
import com.kurento.kmf.repository.HttpSessionTerminatedEvent;
import com.kurento.kmf.repository.RepositoryHttpEventListener;
import com.kurento.kmf.repository.RepositoryHttpPlayer;
import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.test.util.HttpRepositoryTest;
import com.kurento.kmf.repository.test.util.TestUtils;

public class PlayerEventsTest extends HttpRepositoryTest {

	@Test
	public void testFileUploadAndDownload() throws Exception {

		RepositoryItem repositoryItem = getRepository().createRepositoryItem();

		String id = repositoryItem.getId();

		File fileToUpload = new File("test-files/sample.txt");

		uploadWithEvents(repositoryItem, fileToUpload);

		File downloadedFile = downloadWithEvents(id);

		assertTrue(
				"The uploaded file and the result of download it again are different",
				TestUtils.equalFiles(fileToUpload, downloadedFile));
	}

	private void uploadWithEvents(RepositoryItem repositoryItem,
			File fileToUpload) throws URISyntaxException,
			FileNotFoundException, IOException, InterruptedException {
		RepositoryHttpRecorder recorder = repositoryItem
				.createRepositoryHttpRecorder();

		final CountDownLatch started = new CountDownLatch(1);
		recorder.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
			@Override
			public void onEvent(HttpSessionStartedEvent event) {
				started.countDown();
			}
		});

		final CountDownLatch terminated = new CountDownLatch(1);
		recorder.addSessionTerminatedListener(new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
			@Override
			public void onEvent(HttpSessionTerminatedEvent event) {
				terminated.countDown();
			}
		});

		uploadFileWithPOST(recorder.getURL(), fileToUpload);

		// TODO We need to be sure that this events appear in the order
		// specified. This test doesn't control this

		assertTrue("Started event didn't sent in 10 seconds",
				started.await(10, TimeUnit.SECONDS));
		assertTrue("Terminated event didn't sent in 10 seconds",
				terminated.await(10, TimeUnit.SECONDS));
	}

	private File downloadWithEvents(String id) throws Exception,
			InterruptedException {

		RepositoryItem newRepositoryItem = getRepository()
				.findRepositoryItemById(id);

		RepositoryHttpPlayer player = newRepositoryItem
				.createRepositoryHttpPlayer();

		final CountDownLatch started = new CountDownLatch(1);
		player.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
			@Override
			public void onEvent(HttpSessionStartedEvent event) {
				started.countDown();
			}
		});

		final CountDownLatch terminated = new CountDownLatch(1);
		player.addSessionTerminatedListener(new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
			@Override
			public void onEvent(HttpSessionTerminatedEvent event) {
				terminated.countDown();
			}
		});

		File downloadedFile = new File("test-files/tmp/" + id);
		downloadFromURL(player.getURL(), downloadedFile);

		// TODO We need to be sure that this events appear in the order
		// specified. This test doesn't control this

		assertTrue("Started event didn't sent in 10 seconds",
				started.await(10, TimeUnit.SECONDS));
		assertTrue("Terminated event didn't sent in 10 seconds",
				terminated.await(10, TimeUnit.SECONDS));
		return downloadedFile;
	}

}