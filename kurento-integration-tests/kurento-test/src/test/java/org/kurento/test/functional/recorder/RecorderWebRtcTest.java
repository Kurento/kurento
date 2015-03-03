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

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.Shell;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 *
 * <strong>Description</strong>: Test of a HTTP Recorder, using the stream
 * source from a WebRtcEndpoint in loopback.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>WebRtcEndpoint -> WebRtcEndpoint & RecorderEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected</li>
 * <li>Browser ends before default timeout</li>
 * <li>Media should be received in the video tag (in the recording)</li>
 * <li>Color of the video should be the expected (in the recording)</li>
 * <li>Ended event should arrive to player (in the recording)</li>
 * <li>Play time should be the expected (in the recording)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderWebRtcTest extends FunctionalTest {

	private static final int PLAYTIME = 10; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";
	private static final String PRE_PROCESS_SUFIX = "-preprocess.webm";

	public RecorderWebRtcTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChrome();
	}

	@Test
	public void testRecorderWebRtcChrome() throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		final String recordingPreProcess = Protocol.FILE
				+ getDefaultOutputFile(PRE_PROCESS_SUFIX);
		final String recordingPostProcess = Protocol.FILE
				+ getDefaultFileForRecording();
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				recordingPreProcess).build();
		webRtcEP.connect(webRtcEP);
		webRtcEP.connect(recorderEP);

		// Test execution #1. WewbRTC in loopback while it is recorded
		getBrowser().subscribeEvents("playing");
		getBrowser().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_RCV);
		recorderEP.record();

		// Wait until event playing in the remote stream
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getBrowser().waitForEvent("playing"));

		// Guard time to play the video
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		Assert.assertTrue("The color of the video should be green",
				getBrowser().similarColor(CHROME_VIDEOTEST_COLOR));

		// Assert codecs
		AssertMedia.assertCodecs(getDefaultOutputFile(PRE_PROCESS_SUFIX),
				EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);

		// Release Media Pipeline #1
		recorderEP.stop();
		mp.release();

		// Post-processing
		Shell.runAndWait("ffmpeg", "-i", recordingPreProcess, "-c", "copy",
				recordingPostProcess);

		// Play the recording
		playFileAsLocal(BrowserType.CHROME, recordingPostProcess, PLAYTIME, 0,
				0, CHROME_VIDEOTEST_COLOR);

		// Uncomment this line to play the recording with a new pipeline
		// playFileWithPipeline(browserType, recordingPostProcess, PLAYTIME,
		// color);
	}
}
