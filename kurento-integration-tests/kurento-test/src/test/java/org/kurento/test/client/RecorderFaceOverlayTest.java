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
package org.kurento.test.client;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * 
 * <strong>Description</strong>: Test of a HTTP Recorder, using the stream
 * source from a PlayerEndpoint with FaceOverlayFilter through an
 * HttpGetEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> FaceOverlayFilter -> RecorderEndpoint & HttpGetEndpoint
 * </li>
 * <li>PlayerEndpoint -> HttpGetEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Play time should be the expected</li>
 * <li>Color above the head of the video should be the expected (image overlaid)
 * </li>
 * <li>Browser ends before default timeout</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderFaceOverlayTest extends BrowserKurentoClientTest {

	private static final int PLAYTIME = 30; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";

	@Test
	public void testRecorderFaceOverlayChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Ignore
	@Test
	public void testRecorderFaceOverlayFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/fiwarecut.mp4").build();
		HttpGetEndpoint httpEP = new HttpGetEndpoint.Builder(mp)
				.terminateOnEOS().build();
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				FILE_SCHEMA + getDefaultFileForRecording()).build();
		final FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp)
				.build();
		filter.setOverlayedImage(
				"http://files.kurento.org/imgs/red-square.png", -0.2F, -1.2F,
				1.6F, 1.6F);

		playerEP.connect(filter);
		filter.connect(httpEP);
		filter.connect(recorderEP);

		// Test execution #1. Play and record
		launchBrowser(browserType, httpEP, playerEP, recorderEP);

		// Release Media Pipeline #1
		mp.release();

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2, FILE_SCHEMA
				+ getDefaultFileForRecording()).build();
		HttpGetEndpoint httpEP2 = new HttpGetEndpoint.Builder(mp2)
				.terminateOnEOS().build();
		playerEP2.connect(httpEP2);

		// Test execution #2. Play the recorded video
		launchBrowser(browserType, httpEP2, playerEP2, null);

		// Release Media Pipeline #2
		mp2.release();
	}

	private void launchBrowser(Browser browserType, HttpGetEndpoint httpEP,
			PlayerEndpoint playerEP, RecorderEndpoint recorderEP)
			throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			if (recorderEP != null) {
				recorderEP.record();
			}
			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// Color in the video in the position 420x45 must be red
			Assert.assertTrue(
					"Color above the head must be red (FaceOverlayFilter)",
					browser.similarColorAt(Color.RED, 420, 45));

			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time of HTTP player (expected: "
					+ PLAYTIME + " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));

			// Assess video/audio codec of the recorded video
			AssertMedia.assertCodecs(getDefaultFileForRecording(),
					EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
		}
	}
}
