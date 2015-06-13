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
import org.kurento.client.HttpPostEndpoint;
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
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

		final CountDownLatch disconnectedLatch = new CountDownLatch(1);

		String kmsUrl = kms.getLocalhostWsUrl();

		log.info("Connecting to KMS in " + kmsUrl);

		KurentoClient kurentoClient = KurentoClient.create(kmsUrl,
				new KurentoConnectionListener() {

					@Override
					public void disconnected() {
						log.info("disconnected from KMS");
						disconnectedLatch.countDown();
					}

					@Override
					public void connectionFailed() {

					}

					@Override
					public void connected() {

					}

					@Override
					public void reconnected(boolean sameServer) {
												
					}
				});

		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		HttpPostEndpoint httpEndpoint = new HttpPostEndpoint.Builder(pipeline)
				.build();

		player.connect(httpEndpoint);

		try {
			kms.destroy();
		} catch (Exception e) {
			fail("Exception thrown when destroying kms. " + e);
		}

		log.debug("Waiting for disconnection event");
		if (!disconnectedLatch.await(60, TimeUnit.SECONDS)) {
			fail("Event disconnected should be thrown when kcs is destroyed");
		}
		log.debug("Disconnection event received");
	}

	@Test
	public void reconnectTest() throws InterruptedException, IOException {

		KurentoMediaServerManager kms = KurentoServicesTestHelper
				.startKurentoMediaServer();

		String kmsUrl = kms.getLocalhostWsUrl();

		log.info("Connecting to KMS in " + kmsUrl);

		KurentoClient kurentoClient = KurentoClient.create(kmsUrl);

		MediaPipeline pipeline1 = kurentoClient.createMediaPipeline();

		kms.destroy();

		Thread.sleep(3000);

		kms = KurentoServicesTestHelper.startKurentoMediaServer();

		MediaPipeline pipeline2 = kurentoClient.createMediaPipeline();

		kms.destroy();
	}
}
