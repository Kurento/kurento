/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the
 * terms of the GNU Lesser General Public License (LGPL) version 2.1 which
 * accompanies this
 * distribution, and is available at http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.functional.recorder;

import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.WEBM;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Test of a Recorder switching sources from WebRtcEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · 2 x Chrome <br>
 *
 * Test logic: <br>
 * 1. (KMS) Two media pipelines. First WebRtcEndpoint to RecorderEndpoint
 * (recording) and then PlayerEndpoint -> WebRtcEndpoint (play of the
 * recording). <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag (in the recording)
 * <br>
 * · The color of the received video should be as expected (in the recording)
 * <br>
 * · EOS event should arrive to player (in the recording) <br>
 * · Play time in remote video should be as expected (in the recording) <br>
 * · Codecs should be as expected (in the recording) <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag (in the playing) <br>
 * · The color of the received video should be as expected (in the playing) <br>
 * · EOS event should arrive to player (in the playing) <br>
 * 
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.1.1
 */
public class RecorderSwitchPlayerWebRTCTest extends BaseRecorder {

	private static final int PLAYTIME = 10; // seconds
	private static final int NUM_SWAPS = 6;
	private static final Color[] EXPECTED_COLORS = { Color.RED, Color.GREEN };

	private static final String BROWSER1 = "browser1";
	private static final String BROWSER2 = "browser2";

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		TestScenario test = new TestScenario();
		test.addBrowser(BROWSER1, new Browser.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.webPageType(WebPageType.WEBRTC)
				.video(getTestFilesPath() + "/video/10sec/red.y4m").build());
		test.addBrowser(BROWSER2,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL)
						.webPageType(WebPageType.WEBRTC).build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testRecorderSwitchPlayerWebRTCWebm() throws Exception {
		doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
				EXTENSION_WEBM);
	}

	@Ignore
	@Test
	public void testRecorderSwitchPlayerWebRTCMp4() throws Exception {
		doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
				EXTENSION_MP4);
	}

	public void doTest(MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			String extension) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/green.webm").build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		String recordingFile = getDefaultOutputFile(extension);
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				Protocol.FILE + recordingFile)
						.withMediaProfile(mediaProfileSpecType).build();

		// Test execution
		getPage(BROWSER1).subscribeLocalEvents("playing");
		getPage(BROWSER1).initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_ONLY);

		// red
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage(BROWSER1).waitForEvent("playing"));
		playerGreen.play();
		recorderEP.record();
		for (int i = 0; i < NUM_SWAPS; i++) {
			if ((i % 2) == 0) {
				webRtcEP.connect(recorderEP);
			} else {
				playerGreen.connect(recorderEP);
			}

			Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / NUM_SWAPS);
		}

		// Release Media Pipeline #1
		saveGstreamerDot(mp);
		recorderEP.stop();
		mp.release();

		// Reloading browser
		getPage(BROWSER1).close();

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2,
				Protocol.FILE + recordingFile).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP2.connect(webRtcEP2);

		// Playing the recording
		getPage(BROWSER2).subscribeEvents("playing");
		getPage(BROWSER2).initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);
		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});
		playerEP2.play();

		// Assertions in recording
		final String messageAppend = "[played file with media pipeline]";
		final int playtime = PLAYTIME;

		Assert.assertTrue(
				"Not received media in the recording (timeout waiting playing event) "
						+ messageAppend,
				getPage(BROWSER2).waitForEvent("playing"));
		for (Color color : EXPECTED_COLORS) {
			Assert.assertTrue(
					"The color of the recorded video should be " + color + " "
							+ messageAppend,
					getPage(BROWSER2).similarColor(color));
		}
		Assert.assertTrue("Not received EOS event in player", eosLatch
				.await(getPage(BROWSER2).getTimeout(), TimeUnit.SECONDS));

		double currentTime = getPage(BROWSER2).getCurrentTime();
		Assert.assertTrue(
				"Error in play time in the recorded video (expected: "
						+ playtime + " sec, real: " + currentTime + " sec) "
						+ messageAppend,
				getPage(BROWSER2).compare(playtime, currentTime));

		AssertMedia.assertCodecs(recordingFile, expectedVideoCodec,
				expectedAudioCodec);
		AssertMedia.assertDuration(recordingFile,
				TimeUnit.SECONDS.toMillis(playtime), TimeUnit.SECONDS
						.toMillis(getPage(BROWSER2).getThresholdTime()));

		// Release Media Pipeline #2
		mp2.release();

		success = true;
	}

}
