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

import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.MainMixerImpl;
import com.kurento.kms.thrift.api.KmsMediaType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class SyncMediaServerTest {

	public static final String URL_BARCODES = "https://ci.kurento.com/video/barcodes.webm";
	public static final String URL_FIWARECUT = "https://ci.kurento.com/video/fiwarecut.webm";
	public static final String URL_SMALL = "https://ci.kurento.com/video/small.webm";

	@Autowired
	private MediaPipelineFactory pipelineFactory;

	private MediaPipeline pipeline;

	@Before
	public void setUpBeforeClass() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
	}

	@After
	public void afterClass() {
		pipeline.release();
	}

	public void testCampusPartySimulatedPipeline() throws InterruptedException,
			KurentoMediaFrameworkException {
		RtpEndpoint rtpEndpoint = pipeline.newRtpEndpoint().build();

		String requestSdp = "v=0\r\n"
				+ "o=- 12345 12345 IN IP4 192.168.1.18\r\n" + "s=-\r\n"
				+ "c=IN IP4 192.168.1.18\r\n" + "t=0 0\r\n"
				+ "m=video 45936 RTP/AVP 96\r\n"
				+ "a=rtpmap:96 H263-1998/90000\r\n" + "a=sendrecv\r\n"
				+ "b=AS:3000\r\n";

		rtpEndpoint.processOffer(requestSdp);
		rtpEndpoint
				.getMediaSrcs(KmsMediaType.VIDEO)
				.iterator()
				.next()
				.connect(
						rtpEndpoint.getMediaSinks(KmsMediaType.VIDEO)
								.iterator().next());

		// Wait some time simulating the connection to the player app
		Thread.sleep(1000);

		HttpEndpoint httpEndpoint = pipeline.newHttpEndpoint().build();

		rtpEndpoint.connect(httpEndpoint, KmsMediaType.VIDEO);
	}

	@Test
	public void testSourceSinks() throws KurentoMediaFrameworkException {
		RtpEndpoint rtp = pipeline.newRtpEndpoint().build();

		Collection<MediaSource> videoSrcsA = rtp
				.getMediaSrcs(KmsMediaType.VIDEO);
		Assert.assertFalse(videoSrcsA.isEmpty());

		Collection<MediaSink> videoSinksA = rtp
				.getMediaSinks(KmsMediaType.VIDEO);
		Assert.assertFalse(videoSinksA.isEmpty());

		Collection<MediaSource> audioSrcsA = rtp
				.getMediaSrcs(KmsMediaType.AUDIO);
		Assert.assertFalse(audioSrcsA.isEmpty());

		Collection<MediaSink> audioSinksA = rtp
				.getMediaSinks(KmsMediaType.AUDIO);
		Assert.assertFalse(audioSinksA.isEmpty());

		rtp.release();
	}

	@Test
	public void testConnect() throws KurentoMediaFrameworkException {
		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL).build();
		HttpEndpoint http = pipeline.newHttpEndpoint().build();

		player.connect(http);

		player.play();
		http.release();
		player.release();
	}

	@Test
	public void testConnectByType() throws KurentoMediaFrameworkException {
		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL).build();
		HttpEndpoint http = pipeline.newHttpEndpoint().build();

		player.connect(http, KmsMediaType.AUDIO);
		player.connect(http, KmsMediaType.VIDEO);

		player.play();
		http.release();
		player.release();
	}

	// TODO: Enable this test when mixer is implemented
	@Ignore
	@Test
	public void testMixer() throws KurentoMediaFrameworkException {
		MainMixer mixer = (MainMixer) pipeline
				.createMediaMixer(MainMixerImpl.TYPE);
		mixer.release();
	}

}
