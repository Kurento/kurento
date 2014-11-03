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
package org.kurento.client.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.test.services.KurentoControlServerManager;
import org.kurento.test.services.KurentoMediaServerManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListenerTest {

	private static Logger log = LoggerFactory
			.getLogger(ConnectionListenerTest.class);

	@Test
	public void disconnectionEventTest() throws InterruptedException,
			IOException {

		KurentoMediaServerManager kms = KurentoServicesTestHelper
				.startKurentoMediaServer();

		KurentoControlServerManager kcs = KurentoServicesTestHelper
				.startKurentoControlServer("ws://localhost:9999/kurento");

		final CountDownLatch disconnectedLatch = new CountDownLatch(1);

		String kcsUrl = kcs.getLocalhostWsUrl();

		log.info("Connecting to KMS in " + kcsUrl);

		KurentoClient kurentoClient = KurentoClient.create(kcsUrl,
				new KurentoConnectionListener() {

					@Override
					public void disconnected() {
						log.info("disconnected from KCS");
						disconnectedLatch.countDown();
					}

					@Override
					public void connectionTimeout() {

					}

					@Override
					public void connected() {

					}
				});

		MediaPipeline pipeline = new MediaPipeline.Builder(kurentoClient).build();

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build();

		player.connect(httpGetEndpoint);

		kcs.destroy();

		if (!disconnectedLatch.await(60, TimeUnit.SECONDS)) {
			fail("Event disconnected should be thrown when kcs is destroyed");
		}

		kms.destroy();
	}
}
