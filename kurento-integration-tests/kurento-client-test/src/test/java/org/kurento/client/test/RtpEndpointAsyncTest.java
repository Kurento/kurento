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

import static org.kurento.client.test.RtpEndpoint2Test.URL_BARCODES;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.MediaType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.client.test.util.SdpAsyncBaseTest;

/**
 * {@link RtpEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RtpEndpoint#getLocalSessionDescriptor()}
 * <li>{@link RtpEndpoint#getRemoteSessionDescriptor()}
 * <li>{@link RtpEndpoint#generateOffer()}
 * <li>{@link RtpEndpoint#processOffer(String)}
 * <li>{@link RtpEndpoint#processAnswer(String)}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link RtpEndpoint#addMediaSessionStartedListener(EventListener)}
 * <li>
 * {@link RtpEndpoint#addMediaSessionTerminatedListener(EventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class RtpEndpointAsyncTest extends SdpAsyncBaseTest<RtpEndpoint> {

	@Before
	public void setupMediaElements() throws InterruptedException {

		AsyncResultManager<RtpEndpoint> async = new AsyncResultManager<>(
				"RtpEndpoint creation");
		new RtpEndpoint.Builder(pipeline).buildAsync(async.getContinuation());
		sdp = async.waitForResult();
		Assert.assertNotNull(sdp);

		AsyncResultManager<RtpEndpoint> async2 = new AsyncResultManager<>(
				"RtpEndpoint creation");
		new RtpEndpoint.Builder(pipeline).buildAsync(async2.getContinuation());
		sdp2 = async2.waitForResult();
		Assert.assertNotNull(sdp2);
	}

	@Test
	public void testRtpEndpointSimulatingAndroidSdp()
			throws InterruptedException {

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				URL_BARCODES).build();

		RtpEndpoint rtpEndpoint = new RtpEndpoint.Builder(pipeline).build();

		String requestSdp = "v=0\r\n"
				+ "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n"
				+ "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";

		rtpEndpoint.processOffer(requestSdp);
		player.connect(rtpEndpoint, MediaType.VIDEO);
		player.play();

		// just a little bit of time before destroying
		Thread.sleep(2000);
	}

}