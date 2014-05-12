package com.kurento.kmf.broker.test;

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

import kmf.broker.Broker;
import kmf.broker.client.JsonRpcClientBroker;
import kmf.broker.server.MediaServerBroker;

import org.apache.catalina.LifecycleException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.test.PropertiesManager;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a HTTP Player, using directly a MediaPipeline and Selenium.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class MediaApiPlayerBrowserTestTI extends MediaApiTest {

	private static final long TIMEOUT = 5000;

	private Broker broker;
	private MediaServerBroker mediaServerBroker;

	@Override
	public void setupMediaPipelineFactory() {

		broker = new Broker("MA>");
		broker.init();
		pipelineFactory = new MediaPipelineFactory(new JsonRpcClientBroker(
				broker));
	}

	@Before
	public void setupMediaServerBrokerConnector() {

		String serverAddress = PropertiesManager.getSystemProperty(
				"kurento.serverAddress", "127.0.0.1");
		int serverPort = PropertiesManager.getSystemProperty(
				"kurento.serverPort", 9090);

		String handlerAddress = PropertiesManager.getSystemProperty(
				"kurento.handlerAddress", "127.0.0.1");
		int handlerPort = PropertiesManager.getSystemProperty(
				"kurento.handlerPort", 9393);

		mediaServerBroker = new MediaServerBroker(serverAddress, serverPort,
				handlerAddress, handlerPort);

	}

	@After
	public void teardownMediaPipeline() throws LifecycleException, IOException {
		if (broker != null) {
			broker.destroy();
		}

		if (mediaServerBroker != null) {
			mediaServerBroker.destroy();
		}
	}

	@Test
	public void testPlayer() throws Exception {

		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/color/blue.webm").build();
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
				.browser(Browser.CHROME_FOR_TEST).client(Client.PLAYER).build()) {
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
