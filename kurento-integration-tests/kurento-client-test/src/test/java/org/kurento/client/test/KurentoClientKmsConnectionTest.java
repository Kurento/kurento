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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.services.KurentoMediaServerManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KurentoClientKmsConnectionTest {

	private static Logger log = LoggerFactory
			.getLogger(KurentoClientKmsConnectionTest.class);

	@Test
	public void errorSendingClosedKmsTest() throws Exception {

		KurentoMediaServerManager kms = KurentoServicesTestHelper
				.startKurentoMediaServer();

		String kmsUrl = kms.getLocalhostWsUrl();

		KurentoClient kurento = KurentoClient.create(kmsUrl,
				new KurentoConnectionListener() {

					@Override
					public void reconnected(boolean sameServer) {
					}

					@Override
					public void disconnected() {
						log.info("Disconnected");
					}

					@Override
					public void connectionFailed() {
					}

					@Override
					public void connected() {
					}
				});

		kurento.createMediaPipeline();

		kms.destroy();

		try {
			kurento.createMediaPipeline();
			fail("KurentoException should be thrown");
		} catch (KurentoException e) {
			assertThat(e.getMessage(),
					containsString("Exception connecting to WebSocket"));
		}
	}
}
