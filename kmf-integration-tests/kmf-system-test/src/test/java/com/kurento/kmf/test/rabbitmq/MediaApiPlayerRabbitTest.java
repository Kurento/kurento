package com.kurento.kmf.test.rabbitmq;

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

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.LifecycleException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.factory.KmfMediaApiProperties;
import com.kurento.kmf.media.factory.MediaPipelineFactory;
import com.kurento.kmf.rabbitmq.client.JsonRpcClientRabbitMq;
import com.kurento.kmf.rabbitmq.server.RabbitMqConnectorManager;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a HTTP Player, using directly a MediaPipeline and Selenium.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class MediaApiPlayerRabbitTest extends BrowserMediaApiTest {

	private static final long TIMEOUT = 5000;

	private RabbitMqConnectorManager rabbitMediaConnector;

	@Override
	public void setupMediaPipelineFactory() {

		pipelineFactory = new MediaPipelineFactory(new JsonRpcClientRabbitMq(
				KmfMediaApiProperties.getRabbitMqAddress()));
	}

	@Before
	public void setupMediaServerBrokerConnector() {

		rabbitMediaConnector = new RabbitMqConnectorManager(
				KmfMediaApiProperties.getThriftKmsAddress(),
				KmfMediaApiProperties.getThriftKmfAddress(),
				KmfMediaApiProperties.getRabbitMqAddress());
	}

	@After
	public void teardownMediaServerBrokerConnector() throws LifecycleException,
			IOException {
		if (rabbitMediaConnector != null) {
			rabbitMediaConnector.destroy();
		}
	}

	@Test
	public void testPlayerRabbit() throws Exception {

		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"https://ci.kurento.com/video/color/blue.webm").build();
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
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			Assert.assertTrue("Playback time must be at least 3 seconds",
					browser.getCurrentTime() >= 3);
			Assert.assertTrue("The color of the video should be blue",
					browser.colorSimilarTo(Color.BLUE));
		}

		Assert.assertTrue("The player should fire 'EndOfStreamEvent'",
				endOfStreamEvent.await(TIMEOUT, TimeUnit.SECONDS));

		playerEP.release();
		httpEP.release();
		mp.release();
	}

}
