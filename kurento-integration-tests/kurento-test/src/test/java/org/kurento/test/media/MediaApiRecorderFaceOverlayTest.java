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
package org.kurento.test.media;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.test.base.BrowserMediaApiTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
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
public class MediaApiRecorderFaceOverlayTest extends BrowserMediaApiTest {

	private static final int VIDEO_LENGTH = 25; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";

	@Test
	public void testRecorderFaceOverlayChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testRecorderFaceOverlayFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/fiwarecut.mp4").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		RecorderEndpoint recorderEP = mp.newRecorderEndpoint(
				FILE_SCHEMA + getDefaultFileForRecording()).build();
		final FaceOverlayFilter filter = mp.newFaceOverlayFilter().build();
		filter.setOverlayedImage(
				"http://files.kurento.org/imgs/mario-wings.png", -0.2F, -1.2F,
				1.6F, 1.6F);

		playerEP.connect(filter);
		filter.connect(httpEP);
		filter.connect(recorderEP);

		// Test execution #1. Play and record
		launchBrowser(browserType, httpEP, playerEP, recorderEP);

		// Media Pipeline #2
		PlayerEndpoint playerEP2 = mp.newPlayerEndpoint(
				FILE_SCHEMA + getDefaultFileForRecording()).build();
		HttpGetEndpoint httpEP2 = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP2.connect(httpEP2);

		// Test execution #2. Play the recorded video
		launchBrowser(browserType, httpEP2, playerEP2, null);
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

			// Color in the video at second 10, in the position 420x15 must be
			// red (R=200;G=0;B=0)
			Assert.assertTrue(
					"Color above the head must be red (FaceOverlayFilter)",
					browser.color(new Color(200, 0, 0), 10, 420, 45));

			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Play time must be at least " + VIDEO_LENGTH
					+ " seconds and is " + currentTime,
					currentTime >= VIDEO_LENGTH);

			// Assess video/audio codec of the recorded video
			AssertMedia.assertCodecs(getDefaultFileForRecording(),
					EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
		}
	}
}
