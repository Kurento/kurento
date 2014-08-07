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
import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * 
 * <strong>Description</strong>: Test of a HTTP Recorder, using the stream
 * source from a PlayerEndpoint through an HttpGetEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> RecorderEndpoint & HttpGetEndpoint</li>
 * <li>PlayerEndpoint -> HttpGetEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Play time should be the expected</li>
 * <li>Color of the video should be the expected</li>
 * <li>Browser ends before default timeout</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderPlayerTest extends BrowserKurentoClientTest {

	private static final int VIDEO_LENGTH = 8; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";

	@Test
	public void testRecorderPlayerChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testRecorderPlayerFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = pipelineFactory.createMediaPipeline();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/10sec/green.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		RecorderEndpoint recorderEP = mp.newRecorderEndpoint(
				FILE_SCHEMA + getDefaultFileForRecording()).build();
		playerEP.connect(httpEP);
		playerEP.connect(recorderEP);

		// Test execution #1. Play the video while it is recorded
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
			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			Assert.assertTrue("Play time must be at least " + VIDEO_LENGTH
					+ " seconds", browser.getCurrentTime() >= VIDEO_LENGTH);
			Assert.assertTrue("The color of the video should be green",
					browser.colorSimilarTo(Color.GREEN));

			// Assess video/audio codec of the recorded video
			AssertMedia.assertCodecs(getDefaultFileForRecording(),
					EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
		}
	}
}
