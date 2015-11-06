/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.test.functional.player;

import static org.kurento.test.config.Protocol.HTTP;

import java.awt.Color;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.config.VideoFormat;

/**
 * Base for player tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class SimplePlayer extends FunctionalTest {

	public SimplePlayer(TestScenario testScenario) {
		super(testScenario);
	}

	public void testPlayerWithRtsp(WebRtcChannel webRtcChannel)
			throws Exception {
		testPlayer(
				"rtsp://r6---sn-cg07luez.c.youtube.com/CiILENy73wIaGQm2gbECn1Hi5RMYDSANFEgGUgZ2aWRlb3MM/0/0/0/video.3gp",
				webRtcChannel, 0, 50, 50, Color.WHITE);
	}

	public void testPlayerWithSmallFileVideoOnly(Protocol protocol,
			VideoFormat videoFormat, WebRtcChannel webRtcChannel)
					throws InterruptedException {
		testPlayerWithSmallFile(protocol, videoFormat, webRtcChannel, true);
	}

	public void testPlayerWithSmallFile(Protocol protocol,
			VideoFormat videoFormat, WebRtcChannel webRtcChannel)
					throws InterruptedException {
		testPlayerWithSmallFile(protocol, videoFormat, webRtcChannel, false);
	}

	private void testPlayerWithSmallFile(Protocol protocol,
			VideoFormat videoFormat, WebRtcChannel webRtcChannel,
			boolean videoOnly) throws InterruptedException {
		// Reduce threshold time per test
		getPage().setThresholdTime(5); // seconds

		String mediaUrl = protocol.toString();
		mediaUrl += protocol == HTTP ? "files.kurento.org" : getPathTestFiles();
		mediaUrl += "/video/format/";
		mediaUrl += videoOnly ? "small_video_only." : "small.";
		mediaUrl += videoFormat.toString();

		log.debug(">>>> Playing small video ({}) on {}", webRtcChannel,
				mediaUrl);
		testPlayer(mediaUrl, webRtcChannel, 5, 50, 50, new Color(99, 65, 40));
	}

	public void testPlayer(String mediaUrl, WebRtcChannel webRtcChannel,
			int playtime) throws InterruptedException {
		testPlayer(mediaUrl, webRtcChannel, playtime, 0, 0, null);
	}

	public void testPlayer(String mediaUrl, WebRtcChannel webRtcChannel,
			int playtime, int x, int y, Color expectedColor)
					throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, mediaUrl)
				.build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
		playerEP.connect(webRtcEP);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, webRtcChannel, WebRtcMode.RCV_ONLY);
		playerEP.play();

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));
		if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
			Assert.assertTrue(
					"The color of the video should be " + expectedColor,
					getPage().similarColorAt(expectedColor, x, y));
		}
		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
		double currentTime = getPage().getCurrentTime();
		if (playtime > 0) {
			Assert.assertTrue(
					"Error in play time (expected: " + playtime + " sec, real: "
							+ currentTime + " sec)",
					getPage().compare(playtime, currentTime));
		}

		// Release Media Pipeline
		mp.release();
	}

}
