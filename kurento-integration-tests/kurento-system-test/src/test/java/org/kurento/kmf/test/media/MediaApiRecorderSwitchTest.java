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
package org.kurento.kmf.test.media;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

import org.kurento.kmf.media.HttpGetEndpoint;
import org.kurento.kmf.media.MediaPipeline;
import org.kurento.kmf.media.PlayerEndpoint;
import org.kurento.kmf.media.RecorderEndpoint;
import org.kurento.kmf.test.base.BrowserMediaApiTest;
import org.kurento.kmf.test.client.Browser;
import org.kurento.kmf.test.client.BrowserClient;
import org.kurento.kmf.test.client.Client;
import org.kurento.kmf.test.mediainfo.AssertMedia;

/**
 * 
 * <strong>Description</strong>: Test of a HTTP Recorder switching sources from
 * PlayerEndpoint.<br/>
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
public class MediaApiRecorderSwitchTest extends BrowserMediaApiTest {

	private static final int PLAYTIME = 14; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";

	@Test
	public void testRecorderSwitchChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testRecorderSwitchFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerRed = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/10sec/red.webm").build();
		PlayerEndpoint playerGreen = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/10sec/green.webm").build();
		PlayerEndpoint playerBlue = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/10sec/blue.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		RecorderEndpoint recorderEP = mp.newRecorderEndpoint(
				FILE_SCHEMA + getDefaultFileForRecording()).build();

		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());

			// red
			playerRed.connect(httpEP);
			playerRed.connect(recorderEP);
			playerRed.play();
			recorderEP.record();
			browser.subscribeEvents("playing", "ended");
			browser.start();
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Thread.sleep(2000);

			// green
			playerGreen.connect(httpEP);
			playerGreen.connect(recorderEP);
			playerGreen.play();
			Thread.sleep(6000);

			// blue
			playerBlue.connect(httpEP);
			playerBlue.connect(recorderEP);
			playerBlue.play();
			Thread.sleep(6000);

			// Assertions
			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			Assert.assertTrue("Play time must be at least " + PLAYTIME
					+ " seconds", browser.getCurrentTime() >= PLAYTIME);

		}

		// Stop and release media elements
		recorderEP.stop();
		playerRed.stop();
		playerGreen.stop();
		playerBlue.stop();
		recorderEP.release();
		playerRed.release();
		playerGreen.release();
		playerBlue.release();

		// Media Pipeline #2
		PlayerEndpoint playerEP2 = mp.newPlayerEndpoint(
				FILE_SCHEMA + getDefaultFileForRecording()).build();
		HttpGetEndpoint httpEP2 = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP2.connect(httpEP2);

		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.PLAYER).build()) {
			browser.setURL(httpEP2.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP2.play();

			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			Assert.assertTrue("Recorded video first must be red",
					browser.color(Color.RED, 0, 0, 0));
			Assert.assertTrue("Recorded video second must be green",
					browser.color(Color.GREEN, 5, 0, 0));
			Assert.assertTrue("Recorded video third must be blue",
					browser.color(Color.BLUE, 11, 0, 0));

			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			Assert.assertTrue("Play time must be at least " + PLAYTIME
					+ " seconds", browser.getCurrentTime() >= PLAYTIME);

			// Assess video/audio codec of the recorded video
			AssertMedia.assertCodecs(getDefaultFileForRecording(),
					EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
		}
	}
}
