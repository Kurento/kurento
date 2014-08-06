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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.SdpEndpoint;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @param <T>
 *
 */
public abstract class SdpAsyncBaseTest<T extends SdpEndpoint> extends
		MediaPipelineAsyncBaseTest {

	protected T sdp;
	protected T sdp2;

	@After
	public void teardownMediaElements() throws InterruptedException {
		releaseMediaObject(sdp);
		releaseMediaObject(sdp2);
	}

	// TODO connect a local sdp or fails
	@Test
	public void testGetLocalSdpMethod() throws InterruptedException {

		AsyncResultManager<String> async = new AsyncResultManager<String>(
				"sdp.generateOffer() invocation");
		sdp.generateOffer(async.getContinuation());
		async.waitForResult();

		AsyncResultManager<String> async2 = new AsyncResultManager<String>(
				"sdp.getLocalSessionDescriptor() invocation");
		sdp.getLocalSessionDescriptor(async2.getContinuation());
		async2.waitForResult();
	}

	// TODO connect a remote sdp or fails
	@Test
	public void testGetRemoteSdpMethod() throws InterruptedException {

		String offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n"
				+ "s=-\r\n" + "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";

		AsyncResultManager<String> async = new AsyncResultManager<String>(
				"sdp.processOffer() invocation");

		sdp.processOffer(offer, async.getContinuation());

		String result = async.waitForResult();

		Assert.assertFalse(result.isEmpty());
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

}
