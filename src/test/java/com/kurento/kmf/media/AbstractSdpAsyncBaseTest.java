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
package com.kurento.kmf.media;

import static com.kurento.kmf.media.SyncMediaServerTest.URL_BARCODES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.Semaphore;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.kurento.kms.thrift.api.KmsMediaType;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @param <T>
 * 
 */
public abstract class AbstractSdpAsyncBaseTest<T extends SdpEndPoint> extends
		AbstractAsyncBaseTest {

	protected T sdp;

	// TODO connect a local sdp or fails
	@Test
	public void testGetLocalSdpMethod() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);
		sdp.getLocalSessionDescriptor(new Continuation<String>() {

			@Override
			public void onSuccess(String result) {
				if (!result.isEmpty()) {
					sem.release();
				}
			}

			@Override
			public void onError(Throwable cause) {
				// TODO Auto-generated method stub

			}
		});

		Assert.assertTrue(sem.tryAcquire(500, MILLISECONDS));
	}

	// TODO connect a remote sdp or fails
	@Test
	public void testGetRemoteSdpMethod() {
		String removeDescriptor = sdp.getRemoteSessionDescriptor();
		Assert.assertFalse(removeDescriptor.isEmpty());
	}

	@Test
	public void testGenerateSdpOfferMethod() {
		String offer = sdp.generateOffer();
		Assert.assertFalse(offer.isEmpty());
	}

	// TODO This test shuts down the remote KMS!
	@Ignore
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

	// TODO This test shuts down the remote KMS!
	@Ignore
	@Test
	public void testProcessAnswerMethod() {
		// TODO
		String answer = "";
		String ret = sdp.processAnswer(answer);
		Assert.assertFalse(ret.isEmpty());
	}

	// TODO This test shuts down the remote KMS!
	@Ignore
	@Test
	public void testRtpEndPointSimulatingAndroidSdp()
			throws InterruptedException {

		PlayerEndPoint player = pipeline.createPlayerEndPoint(URL_BARCODES);

		RtpEndPoint rtpEndPoint = pipeline.createRtpEndPoint();

		String requestSdp = "v=0\r\n"
				+ "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n"
				+ "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";

		rtpEndPoint.processOffer(requestSdp);
		player.connect(rtpEndPoint, KmsMediaType.VIDEO);
		player.play();

		// just a little bit of time before destroying
		Thread.sleep(2000);
	}
}
