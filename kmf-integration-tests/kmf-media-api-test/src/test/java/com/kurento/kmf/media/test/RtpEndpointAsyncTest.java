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
package com.kurento.kmf.media.test;

import static com.kurento.kmf.media.test.RtpEndpoint2Test.URL_BARCODES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.junit.*;

import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.test.base.SdpAsyncBaseTest;

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
 * <li>{@link RtpEndpoint#addMediaSessionStartedListener(MediaEventListener)}
 * <li>
 * {@link RtpEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
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
		pipeline.newRtpEndpoint().buildAsync(cont);
		pipeline.newRtpEndpoint().buildAsync(cont);

		sdp = creationResults.poll(500, MILLISECONDS);
		sdp2 = creationResults.poll(500, MILLISECONDS);
		Assert.assertNotNull(sdp);
		Assert.assertNotNull(sdp2);
	}

	@Test
	public void testRtpEndpointSimulatingAndroidSdp()
			throws InterruptedException {

		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_BARCODES)
				.build();

		RtpEndpoint rtpEndpoint = pipeline.newRtpEndpoint().build();

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
