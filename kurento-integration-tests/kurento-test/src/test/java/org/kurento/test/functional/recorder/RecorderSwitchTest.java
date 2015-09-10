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
package org.kurento.test.functional.recorder;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.Shell;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 *
 * <strong>Description</strong>: Test of a Recorder switching sources from
 * PlayerEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Color of the video should be the expected</li>
 * <li>Media should be received in the video tag (in the recording)</li>
 * <li>Color of the video should be the expected (in the recording)</li>
 * <li>Ended event should arrive to player (in the recording)</li>
 * <li>Play time should be the expected (in the recording)</li>
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderSwitchTest extends FunctionalTest {

	private static final int PLAYTIME = 20; // seconds
	private static final int N_PLAYER = 3;
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";
	private static final String PRE_PROCESS_SUFIX = "-preprocess.webm";
	private static final Color[] EXPECTED_COLORS = { Color.RED, Color.GREEN, Color.BLUE };

	public RecorderSwitchTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testRecorderSwitch() throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/10sec/red.webm")
				.build();
		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/10sec/green.webm")
				.build();
		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/10sec/blue.webm")
				.build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		final String recordingPreProcess = Protocol.FILE + getDefaultOutputFile(PRE_PROCESS_SUFIX);
		final String recordingPostProcess = Protocol.FILE + getDefaultFileForRecording();
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp, recordingPreProcess).build();

		// Test execution
		getBrowser().subscribeEvents("playing");
		getBrowser().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		// red
		playerRed.connect(webRtcEP);
		playerRed.connect(recorderEP);
		playerRed.play();
		recorderEP.record();

		Assert.assertTrue("Not received media (timeout waiting playing event)", getBrowser().waitForEvent("playing"));
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// green
		playerGreen.connect(webRtcEP);
		playerGreen.connect(recorderEP);
		playerGreen.play();
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// blue
		playerBlue.connect(webRtcEP);
		playerBlue.connect(recorderEP);
		playerBlue.play();
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// Release Media Pipeline #1
		recorderEP.stop();
		mp.release();

		// Reloading browser
		getBrowser().reload();

		// Post-processing
		Shell.runAndWait("ffmpeg", "-y", "-i", recordingPreProcess, "-c", "copy", recordingPostProcess);

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2, recordingPostProcess).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP2.connect(webRtcEP2);

		// Playing the recording
		getBrowser().subscribeEvents("playing");
		getBrowser().initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});
		playerEP2.play();

		// Assertions in recording
		makeAssertions(getBrowser().getBrowserClient().getId(), "[played file with media pipeline]",
				getBrowser().getBrowserClient(), PLAYTIME, 0, 0, eosLatch, EXPECTED_COLORS);
		AssertMedia.assertCodecs(getDefaultOutputFile(PRE_PROCESS_SUFIX), EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);

		// Release Media Pipeline #2
		mp2.release();

	}

}
