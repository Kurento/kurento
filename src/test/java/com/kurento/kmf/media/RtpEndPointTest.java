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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kms.thrift.api.KmsMediaType;

/**
 * {@link RtpEndPoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RtpEndPoint#getLocalSessionDescriptor()}
 * <li>{@link RtpEndPoint#getRemoteSessionDescriptor()}
 * <li>{@link RtpEndPoint#generateOffer()}
 * <li>{@link RtpEndPoint#processOffer(String)}
 * <li>{@link RtpEndPoint#processAnswer(String)}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link RtpEndPoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link RtpEndPoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 * 
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class RtpEndPointTest {

	@Autowired
	private MediaPipelineFactory pipelineFactory;

	private MediaPipeline pipeline;

	private RtpEndPoint rtp;

	@Before
	public void setUpBeforeClass() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
		rtp = pipeline.createRtpEndPoint();
	}

	@After
	public void afterClass() {
		pipeline.release();
		rtp.release();
	}

	@Test
	private void testGetLocalSdpMethod() {
		String localDescriptor = rtp.getLocalSessionDescriptor();
		Assert.assertFalse(localDescriptor.isEmpty());
	}

	@Test
	private void testGetRemoteSdpMethod() {
		String removeDescriptor = rtp.getRemoteSessionDescriptor();
		Assert.assertFalse(removeDescriptor.isEmpty());
	}

	@Test
	private void testGenerateSdpOfferMethod() {
		String offer = rtp.generateOffer();
		Assert.assertFalse(offer.isEmpty());
	}

	@Test
	private void testProcessOfferMethod() {
		String offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n"
				+ "s=-\r\n" + "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";
		String ret = rtp.processOffer(offer);
		Assert.assertFalse(ret.isEmpty());
	}

	@Test
	private void testProcessAnswerMethod() {
		// TODO
		String answer = "";
		String ret = rtp.processAnswer(answer);
		Assert.assertFalse(ret.isEmpty());
	}

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
