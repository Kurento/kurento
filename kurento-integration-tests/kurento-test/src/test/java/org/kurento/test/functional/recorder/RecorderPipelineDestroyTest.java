/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.WEBM;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of a Recorder, using the stream source from a PlayerEndpoint through an
 * WebRtcEndpoint. The pipeline will be destroyed after half the duration of the
 * original video. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) Two media pipelines. First PlayerEndpoint to RecorderEndpoint
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
public class RecorderPipelineDestroyTest extends BaseRecorder {

	private static final int PLAYTIME = 10; // seconds
	private static final Color EXPECTED_COLOR = Color.GREEN;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testRecorderPipelineDestroyWebm() throws Exception {
		doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
				EXTENSION_WEBM);
	}

	@Test
	public void testRecorderPipelineDestroyMp4() throws Exception {
		doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
				EXTENSION_MP4);
	}

	public void doTest(MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			String extension) throws Exception {

		// Media Pipeline #1
		final MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/green.webm").build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();

		String recordingFile = getDefaultOutputFile(extension);

		final RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				Protocol.FILE + "://" + recordingFile)
						.withMediaProfile(mediaProfileSpecType).build();
		playerEP.connect(webRtcEP1);

		playerEP.connect(recorderEP);

		// Test execution #1. Play the video while it is recorded
		launchBrowser(mp, webRtcEP1, playerEP, recorderEP, expectedVideoCodec,
				expectedAudioCodec, recordingFile, EXPECTED_COLOR, 0, 0,
				PLAYTIME);

		final CountDownLatch latch = new CountDownLatch(1);
		ScheduledExecutorService executor = Executors
				.newSingleThreadScheduledExecutor();
		executor.schedule(new Runnable() {

			@Override
			public void run() {
				// Release Media Pipeline #1
				mp.release();
				latch.countDown();
			}
		}, PLAYTIME / 2, TimeUnit.SECONDS);

		latch.await(getPage().getTimeout(), TimeUnit.SECONDS);

		// Reloading browser
		getPage().reload();

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2,
				Protocol.FILE + "://" + recordingFile).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP2.connect(webRtcEP2);

		// Playing the recording
		launchBrowser(null, webRtcEP2, playerEP2, null, expectedVideoCodec,
				expectedAudioCodec, recordingFile, EXPECTED_COLOR, 0, 0,
				PLAYTIME / 2);

		// Release Media Pipeline #2
		mp2.release();

		executor.shutdown();

		success = true;
	}

}
