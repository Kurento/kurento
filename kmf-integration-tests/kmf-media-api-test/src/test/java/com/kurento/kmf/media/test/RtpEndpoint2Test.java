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

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.media.*;
import com.kurento.kmf.media.test.base.MediaPipelineBaseTest;

public class RtpEndpoint2Test extends MediaPipelineBaseTest {

	public static final String URL_BARCODES = "https://ci.kurento.com/video/barcodes.webm";
	public static final String URL_FIWARECUT = "https://ci.kurento.com/video/fiwarecut.webm";
	public static final String URL_SMALL = "https://ci.kurento.com/video/small.webm";
	public static final String URL_PLATES = "https://ci.kurento.com/video/plates.webm";
	public static final String URL_POINTER_DETECTOR = "https://ci.kurento.com/video/pointerDetector.mp4";

	public void testCampusPartySimulatedPipeline() throws InterruptedException {

		RtpEndpoint rtpEndpoint = pipeline.newRtpEndpoint().build();

		String requestSdp = "v=0\r\n"
				+ "o=- 12345 12345 IN IP4 192.168.1.18\r\n" + "s=-\r\n"
				+ "c=IN IP4 192.168.1.18\r\n" + "t=0 0\r\n"
				+ "m=video 45936 RTP/AVP 96\r\n"
				+ "a=rtpmap:96 H263-1998/90000\r\n" + "a=sendrecv\r\n"
				+ "b=AS:3000\r\n";

		rtpEndpoint.processOffer(requestSdp);
		rtpEndpoint
				.getMediaSrcs(MediaType.VIDEO)
				.iterator()
				.next()
				.connect(
						rtpEndpoint.getMediaSinks(MediaType.VIDEO).iterator()
								.next());

		// Wait some time simulating the connection to the player app
		Thread.sleep(1000);

		HttpEndpoint httpEndpoint = pipeline.newHttpGetEndpoint().build();

		rtpEndpoint.connect(httpEndpoint, MediaType.VIDEO);
	}

	@Test
	public void testSourceSinks() {

		RtpEndpoint rtp = pipeline.newRtpEndpoint().build();

		Collection<MediaSource> videoSrcsA = rtp.getMediaSrcs(MediaType.VIDEO);
		Assert.assertFalse(videoSrcsA.isEmpty());

		Collection<MediaSink> videoSinksA = rtp.getMediaSinks(MediaType.VIDEO);
		Assert.assertFalse(videoSinksA.isEmpty());

		Collection<MediaSource> audioSrcsA = rtp.getMediaSrcs(MediaType.AUDIO);
		Assert.assertFalse(audioSrcsA.isEmpty());

		Collection<MediaSink> audioSinksA = rtp.getMediaSinks(MediaType.AUDIO);
		Assert.assertFalse(audioSinksA.isEmpty());

		rtp.release();
	}

	@Test
	public void testConnect() throws KurentoException {
		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL).build();
		HttpEndpoint http = pipeline.newHttpGetEndpoint().build();

		player.connect(http);

		player.play();
		http.release();
		player.release();
	}

	@Test
	public void testConnectByType() throws KurentoException {
		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL).build();
		HttpEndpoint http = pipeline.newHttpGetEndpoint().build();

		player.connect(http, MediaType.AUDIO);
		player.connect(http, MediaType.VIDEO);

		player.play();
		http.release();
		player.release();
	}

}
