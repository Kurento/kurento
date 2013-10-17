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

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Assert;
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
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.internal.AbstractCodeFoundEventListener;
import com.kurento.kmf.media.events.internal.AbstractEndOfStreamEventListener;
import com.kurento.kmf.media.events.internal.AbstractMediaSessionStartedEventListener;
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
		HttpEndPoint httpEndPoint = mediaPipeline.createHttpEndPoint(0, 0);

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
	public void testRtpEndPointSimulatingAndroidSdp()
			throws KurentoMediaFrameworkException, InterruptedException {

		log.info("Creating PlayerEndPoint ...");
		PlayerEndPoint player = mediaPipeline
				.createPlayerEndPoint("https://ci.kurento.com/video/barcodes.webm");

		log.info("Creating RtpEndPoint ...");
		RtpEndPoint rtpEndPoint = mediaPipeline.createRtpEndPoint();

		String requestSdp = "v=0\r\n"
				+ "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n"
				+ "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n"
				+ "m=video 52126 RTP/AVP 96 97 98\r\n"
				+ "a=rtpmap:96 H264/90000\r\n"
				+ "a=rtpmap:97 MP4V-ES/90000\r\n"
				+ "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n"
				+ "b=AS:384\r\n";

		log.info("Offering SDP\n" + requestSdp);
		String answerSdp = rtpEndPoint.processOffer(requestSdp);

		log.info("Answer SDP\n " + answerSdp);

		log.info("Connecting element ...");
		player.connect(rtpEndPoint, KmsMediaType.VIDEO);

		log.info("PlayerEndPoint.play()");
		// TODO Enable this part when START command is implemented in
		// PlayerEndPoints
		// player.play();

		// just a little bit of time before destroying
		Thread.sleep(2000);
	}

	@Test
	public void testStreamSync() throws KurentoMediaFrameworkException {
		RtpEndPoint stream = mediaPipeline.createRtpEndPoint();
		log.debug("generateOffer sessionDecriptor: " + stream.generateOffer());
		log.debug("processOffer sessionDecriptor: "
				+ stream.processOffer("processOffer test"));
		log.debug("processAnswer sessionDecriptor: "
				+ stream.processAnswer("processAnswer test"));
		stream.release();
	}

	// TODO: Enable this test when uri endpoint is implemented
	@Ignore
	@Test
	public void testPlayer() throws KurentoMediaFrameworkException {
		PlayerEndPoint player = mediaPipeline.createPlayerEndPoint("");
		player.play();
		player.pause();
		player.stop();
		player.release();
	}

	// TODO: Enable this test when uri endpoint is implemented
	@Ignore
	@Test
	public void testRecorder() throws KurentoMediaFrameworkException {
		RecorderEndPoint recorder = mediaPipeline.createRecorderEndPoint("");
		recorder.record();
		recorder.pause();
		recorder.stop();
		recorder.release();
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

	@Test
	public void testZBar() throws KurentoMediaFrameworkException,
			InterruptedException {
		PlayerEndPoint player = mediaPipeline
				.createPlayerEndPoint("https://ci.kurento.com/video/barcodes.webm");
		ZBarFilter zbar = mediaPipeline.createZBarFilter();

		player.connect(zbar, KmsMediaType.VIDEO);

		final Semaphore sem = new Semaphore(0);

		zbar.addCodeFoundDataListener(new AbstractCodeFoundEventListener() {

			@Override
			public void onEvent(CodeFoundEvent event) {
				log.info("ZBar event received:\n" + event);
				sem.release();
			}
		});

		player.play();

		Assert.assertTrue(sem.tryAcquire(10, TimeUnit.SECONDS));

		player.stop();
		zbar.release();
		player.release();
	}

	@Test
	public void testJackVader() throws KurentoMediaFrameworkException,
			InterruptedException {
		PlayerEndPoint player = mediaPipeline
				.createPlayerEndPoint("https://ci.kurento.com/video/small.webm");
		JackVaderFilter jackVader = mediaPipeline.createJackVaderFilter();

		player.connect(jackVader, KmsMediaType.VIDEO);

		final Semaphore sem = new Semaphore(0);

		player.play();

		Assert.assertTrue(sem.tryAcquire(10, TimeUnit.SECONDS));

		player.stop();
		jackVader.release();
		player.release();
	}

	@Test
	public void testHttpEndPoint() throws KurentoMediaFrameworkException,
			InterruptedException {
		final PlayerEndPoint player = mediaPipeline
				.createPlayerEndPoint("https://ci.kurento.com/video/small.webm");
		HttpEndPoint httpEndPoint = mediaPipeline.createHttpEndPoint(0, 0);

		player.connect(httpEndPoint, KmsMediaType.VIDEO);

		final Semaphore sem = new Semaphore(0);

		player.addEndOfStreamListener(new AbstractEndOfStreamEventListener() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				sem.release();
			}
		});

		httpEndPoint
				.addMediaSessionStartListener(new AbstractMediaSessionStartedEventListener() {

					@Override
					public void onEvent(MediaSessionStartedEvent event) {
						log.info("received: " + event);
						// TODO correct this callback
						player.play();

					}
				});

		log.info("Url: -- " + httpEndPoint.getUrl());
		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {
			httpclient.execute(new HttpGet(httpEndPoint.getUrl()));
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TODO Change this by a try acquire when test is automated
		sem.acquire();

		player.release();
		httpEndPoint.release();
	}
}
