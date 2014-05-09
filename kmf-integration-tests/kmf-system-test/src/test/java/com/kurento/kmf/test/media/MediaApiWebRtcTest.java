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
package com.kurento.kmf.test.media;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a WebRTC in loopback.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiWebRtcTest extends MediaApiTest {

	@Test
	public void testWebRtcLoopback() throws InterruptedException {

		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		webRtcEndpoint.connect(webRtcEndpoint);

		try (BrowserClient browser = new BrowserClient(getServerPort(),
				Browser.CHROME, Client.WEBRTC)) {

			browser.subscribeEvents("playing");
			browser.connectToWebRtcEndpoint(webRtcEndpoint);

			// Wait until event playing in the remote stream
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// Guard time to see the loopback
			Thread.sleep(5000);
		}
	}
}
