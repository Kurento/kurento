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
package com.kurento.kmf.test.rabbitmq;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.factory.KmfMediaApiProperties;
import com.kurento.kmf.media.factory.MediaPipelineFactory;
import com.kurento.kmf.rabbitmq.client.JsonRpcClientRabbitMq;
import com.kurento.kmf.test.client.*;

/**
 * Client for MultipleClientsAndServersTest.
 * 
 * @see {@link MultipleClientsAndServersTest}
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class ClientApp {

	private static final Logger log = LoggerFactory.getLogger(ClientApp.class);

	public final static int TIMEOUT = 60;

	private MediaPipelineFactory mpf;

	private CountDownLatch finished = new CountDownLatch(1);

	private String logId;

	public ClientApp(String logId) {
		this.mpf = new MediaPipelineFactory(new JsonRpcClientRabbitMq(
				KmfMediaApiProperties.getRabbitMqAddress()));
	}

	public void start() {

		new Thread(logId) {
			@Override
			public void run() {
				try {
					useMediaAPI();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void useMediaAPI() throws InterruptedException {

		MediaPipeline mp = mpf.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/small.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP.connect(httpEP);
		String url = httpEP.getUrl();
		log.info("url: {}", url);

		final CountDownLatch endOfStreamEvent = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				endOfStreamEvent.countDown();
			}
		});

		// Test execution
		final CountDownLatch startEvent = new CountDownLatch(1);
		final CountDownLatch terminationEvent = new CountDownLatch(1);

		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.PLAYER).build()) {
			browser.setURL(url);
			browser.addEventListener("playing", new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("*** playing ***");
					startEvent.countDown();
				}
			});
			browser.addEventListener("ended", new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("*** ended ***");
					terminationEvent.countDown();
				}
			});
			playerEP.play();
			browser.start();

			Assert.assertTrue("Timeout waiting playing event",
					startEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long startTime = System.currentTimeMillis();
			Assert.assertTrue("Timeout waiting ended event",
					terminationEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long duration = System.currentTimeMillis() - startTime;
			log.info("Video duration: " + (duration / 60) + " seconds");
		}

		Assert.assertTrue("The player should fire 'EndOfStreamEvent'",
				endOfStreamEvent.await(TIMEOUT, TimeUnit.SECONDS));

		playerEP.release();
		httpEP.release();
		mp.release();

		finished.countDown();
	}

	public void await() throws InterruptedException {
		finished.await(TIMEOUT, TimeUnit.SECONDS);
	}
}
