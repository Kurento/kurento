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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.MainMixerImpl;
import com.kurento.kms.thrift.api.KmsMediaType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class SyncMediaServerTest {

	private static final Logger log = LoggerFactory
			.getLogger(SyncMediaServerTest.class);

	@Autowired
	@Qualifier("mediaPipelineFactory")
	private MediaPipelineFactory mediaPipelineFactory;

	private MediaPipeline mediaPipeline;

	public static final String URL_BARCODES = "https://ci.kurento.com/video/barcodes.webm";
	public static final String URL_FIWARECUT = "https://ci.kurento.com/video/fiwarecut.webm";
	public static final String URL_SMALL = "https://ci.kurento.com/video/small.webm";

	@Before
	public void setUpBeforeClass() throws KurentoMediaFrameworkException {
		mediaPipeline = mediaPipelineFactory.create();
	}

	@After
	public void afterClass() {
		mediaPipeline.release();
	}

	public void testCampusPartySimulatedPipeline() throws InterruptedException,
			KurentoMediaFrameworkException {
		log.info("Creating RtpEndPoint ...");
		RtpEndPoint rtpEndPoint = mediaPipeline.createRtpEndPoint();

		String requestSdp = "v=0\r\n"
				+ "o=- 12345 12345 IN IP4 192.168.1.18\r\n" + "s=-\r\n"
				+ "c=IN IP4 192.168.1.18\r\n" + "t=0 0\r\n"
				+ "m=video 45936 RTP/AVP 96\r\n"
				+ "a=rtpmap:96 H263-1998/90000\r\n" + "a=sendrecv\r\n"
				+ "b=AS:3000\r\n";

		log.info("Offering SDP\n" + requestSdp);
		String answerSdp = rtpEndPoint.processOffer(requestSdp);
		log.info("AnswerSDP is " + answerSdp);
		log.info("Connecting loopback");
		rtpEndPoint
				.getMediaSrcs(KmsMediaType.VIDEO)
				.iterator()
				.next()
				.connect(
						rtpEndPoint.getMediaSinks(KmsMediaType.VIDEO)
								.iterator().next());

		// Wait some time simulating the connection to the player app
		Thread.sleep(1000);

		log.info("Creating HttpEndPoint ...");
		HttpEndPoint httpEndPoint = mediaPipeline.createHttpEndPoint();

		log.info("Connecting HttpEndPoint ...");
		rtpEndPoint
				.getMediaSrcs(KmsMediaType.VIDEO)
				.iterator()
				.next()
				.connect(
						httpEndPoint.getMediaSinks(KmsMediaType.VIDEO)
								.iterator().next());

		log.info("HttpEndPoint ready to serve at " + httpEndPoint.getUrl());
	}

	@Test
	public void testJoinable() throws KurentoMediaFrameworkException {
		RtpEndPoint streamA = mediaPipeline.createRtpEndPoint();
		RtpEndPoint streamB = mediaPipeline.createRtpEndPoint();

		log.debug("MediaSrcs: " + streamA.getMediaSrcs());
		log.debug("MediaSinks: " + streamA.getMediaSinks());

		log.debug("MediaSrcs audio: "
				+ streamA.getMediaSrcs(KmsMediaType.AUDIO));
		log.debug("MediaSrcs video: "
				+ streamA.getMediaSrcs(KmsMediaType.VIDEO));

		log.debug("MediaSinks audio: "
				+ streamA.getMediaSinks(KmsMediaType.AUDIO));
		log.debug("MediaSinks video: "
				+ streamA.getMediaSinks(KmsMediaType.VIDEO));

		streamA.release();
		streamB.release();
	}

	// TODO: Enable this test when mixer is implemented
	@Ignore
	@Test
	public void testMixer() throws KurentoMediaFrameworkException {
		MainMixer mixer = (MainMixer) mediaPipeline
				.createMediaMixer(MainMixerImpl.TYPE);
		mixer.release();
	}

}
