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
package org.kurento.client.test.util;

import static org.kurento.client.test.RtpEndpoint2Test.URL_BARCODES;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.MediaType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.SdpEndpoint;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @param <T>
 *
 */
public abstract class SdpBaseTest<T extends SdpEndpoint> extends
		MediaPipelineBaseTest {

	protected T sdp;
	protected T sdp2;

	@After
	public void teardownMediaElements() {
		sdp.release();
		sdp2.release();
	}

	@Test
	public void testGetLocalSdpMethod() {
		sdp.generateOffer();
		String localDescriptor = sdp.getLocalSessionDescriptor();
		Assert.assertFalse(localDescriptor.isEmpty());
	}

	@Test
	public void testGetRemoteSdpMethod() {
		String offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n"
				+ "s=-\r\n" + "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";
		sdp.processOffer(offer);
		String removeDescriptor = sdp.getRemoteSessionDescriptor();
		Assert.assertFalse(removeDescriptor.isEmpty());
	}

	@Test
	public void testGenerateSdpOfferMethod() {
		String offer = sdp.generateOffer();
		Assert.assertFalse(offer.isEmpty());
	}

	@Test
	public void testProcessOfferMethod() {
		String offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n"
				+ "s=-\r\n" + "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";
		String ret = sdp.processOffer(offer);
		Assert.assertFalse(ret.isEmpty());
	}

	@Test
	public void testProcessAnswerMethod() {
		String offer = sdp.generateOffer();
		String answer = sdp2.processOffer(offer);
		String ret = sdp.processAnswer(answer);
		Assert.assertFalse(ret.isEmpty());
	}

	@Test
	public void testRtpEndpointSimulatingAndroidSdp()
			throws InterruptedException {

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				URL_BARCODES).build();

		String requestSdp = "v=0\r\n"
				+ "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n"
				+ "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";

		player.connect(sdp, MediaType.VIDEO);
		sdp.processOffer(requestSdp);
		player.play();

		// just a little bit of time before destroying
		Thread.sleep(2000);
	}
}
