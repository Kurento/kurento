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

package com.kurento.kmf.repository.main;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.kurento.kmf.repository.HttpSessionStartedEvent;
import com.kurento.kmf.repository.HttpSessionTerminatedEvent;
import com.kurento.kmf.repository.Repository;
import com.kurento.kmf.repository.RepositoryApiConfiguration;
import com.kurento.kmf.repository.RepositoryHttpEventListener;
import com.kurento.kmf.repository.RepositoryHttpPlayer;
import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class OneRecordingServer {

	private static final Logger log = LoggerFactory
			.getLogger(OneRecordingServer.class);

	private ConfigurableApplicationContext context;

	public void execute() throws Exception {

		startServer();

		RepositoryItem repositoryItem = getRepository().createRepositoryItem();

		prepareToUploadVideo(repositoryItem);
		prepareToDownloadVideo(repositoryItem);

		stopServer();
	}

	public synchronized void startServer() throws Exception {
		if (context == null) {
			context = BootApplication.start();
		}
	}

	private synchronized void stopServer() {

		if (context != null) {

			KurentoApplicationContextUtils
					.closeAllKurentoApplicationContexts(((WebApplicationContext) context)
							.getServletContext());

			context.close();
			context = null;
		}
	}

	private void prepareToDownloadVideo(RepositoryItem repositoryItem)
			throws InterruptedException {
		RepositoryHttpPlayer player = repositoryItem
				.createRepositoryHttpPlayer("video-download");
		log.info("The video can be downloaded with GET from the URL: "
				+ player.getURL());

		player.setAutoTerminationTimeout(30 * 60 * 1000);
		log.info("The player will be auto-terminated 30 min after the last downloading of content (http GET)");

		final CountDownLatch terminatedLatch = new CountDownLatch(1);

		player.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
			@Override
			public void onEvent(HttpSessionStartedEvent event) {
				log.info("Downloading started");
			}
		});

		player.addSessionTerminatedListener(new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
			@Override
			public void onEvent(HttpSessionTerminatedEvent event) {
				log.info("Downloading terminated");
				terminatedLatch.countDown();
			}
		});

		try {
			terminatedLatch.await();
		} catch (InterruptedException e) {
		}
	}

	private void prepareToUploadVideo(RepositoryItem repositoryItem)
			throws InterruptedException {

		RepositoryHttpRecorder recorder = repositoryItem
				.createRepositoryHttpRecorder("video-upload");

		log.info("The video must be uploaded with PUT or POST to the URL: "
				+ recorder.getURL());

		readyToUploadWatch.countDown();

		recorder.setAutoTerminationTimeout(5 * 1000);
		log.info("The recorder will be auto-terminated 5 seconds after the last uploading of content (http PUT or POST)");

		final CountDownLatch terminatedLatch = new CountDownLatch(1);

		recorder.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
			@Override
			public void onEvent(HttpSessionStartedEvent event) {
				log.info("Uploading started");
			}
		});

		recorder.addSessionTerminatedListener(new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
			@Override
			public void onEvent(HttpSessionTerminatedEvent event) {
				log.info("Uploading terminated");
				terminatedLatch.countDown();
			}
		});

		terminatedLatch.await();
	}

	protected Repository getRepository() {
		return (Repository) KurentoApplicationContextUtils
				.getBean("repository");
	}

	// Convenience static methods and attributes

	private static CountDownLatch readyToUploadWatch = new CountDownLatch(1);
	private static OneRecordingServer server;
	private static Thread thread;

	public static void main(String[] args) throws Exception {
		server = new OneRecordingServer();
		server.execute();
	}

	public static String getPublicWebappURL() {
		String web = server.context.getBean(RepositoryApiConfiguration.class)
				.getWebappPublicURL();
		// String web = "http://localhost:8080/";

		log.info("web: " + web);
		return web;
	}

	public static void startServerAndWait() {

		thread = new Thread() {
			public void run() {
				try {
					OneRecordingServer.main(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		thread.start();

		try {
			readyToUploadWatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void stop() {
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
			thread.interrupt();
		}
	}
}
