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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.MP4_AUDIO_ONLY;
import static org.kurento.client.MediaProfileSpecType.MP4_VIDEO_ONLY;
import static org.kurento.client.MediaProfileSpecType.WEBM;
import static org.kurento.client.MediaProfileSpecType.WEBM_AUDIO_ONLY;
import static org.kurento.client.MediaProfileSpecType.WEBM_VIDEO_ONLY;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_AND_VIDEO;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_ONLY;
import static org.kurento.test.browser.WebRtcChannel.VIDEO_ONLY;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

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
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

import com.google.common.base.Strings;

/**
 *
 * <strong>Description</strong>: Test of a HTTP Recorder, using the stream
 * source from a WebRtcEndpoint in loopback. Tests recording with audio and
 * video, only audio or only video<br/>
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
 * <li>Ended event should arrive to player (in the recording)</li>
 * <li>Play time should be the expected (in the recording)</li>
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 4.2.3
 */
public class RecorderWebRtcTest extends BaseRecorder {

	private static final int PLAYTIME = 20; // seconds

	public RecorderWebRtcTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		// return TestScenario.localChrome();
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testRecorderWebRtcChromeWebm() throws Exception {
		doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
				EXTENSION_WEBM);
	}

	@Test
	public void testRecorderWebRtcChromeMp4() throws Exception {
		doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
				EXTENSION_MP4);
	}

	@Test
	public void testRecorderWebRtcChromeVideoOnlyWebm() throws Exception {
		doTest(WEBM_VIDEO_ONLY, EXPECTED_VIDEO_CODEC_WEBM, null,
				EXTENSION_WEBM);
	}

	@Ignore
	@Test
	public void testRecorderWebRtcChromeVideoOnlyMp4() throws Exception {
		doTest(MP4_VIDEO_ONLY, EXPECTED_VIDEO_CODEC_MP4, null, EXTENSION_MP4);
	}

	@Ignore
	@Test
	public void testRecorderWebRtcChromeAudioOnlyWebm() throws Exception {
		doTest(WEBM_AUDIO_ONLY, null, EXPECTED_AUDIO_CODEC_WEBM,
				EXTENSION_WEBM);
	}

	@Ignore
	@Test
	public void testRecorderWebRtcChromeAudioOnlyMp4() throws Exception {
		doTest(MP4_AUDIO_ONLY, null, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
	}

	public void doTest(MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			String extension) throws Exception {

		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		String recordingFile = getDefaultOutputFile(extension);
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				Protocol.FILE + recordingFile)
						.withMediaProfile(mediaProfileSpecType).build();
		webRtcEP.connect(webRtcEP);
		webRtcEP.connect(recorderEP);

		WebRtcChannel webRtcChannel = AUDIO_AND_VIDEO;
		if (Strings.isNullOrEmpty(expectedAudioCodec)) {
			webRtcChannel = VIDEO_ONLY;
		} else if (Strings.isNullOrEmpty(expectedVideoCodec)) {
			webRtcChannel = AUDIO_ONLY;
		}

		log.info("Using webRtcChannel {}", webRtcChannel);

		// Test execution #1. WewbRTC in loopback while it is recorded
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, webRtcChannel, WebRtcMode.SEND_RCV);
		recorderEP.record();
		saveGstreamerDot(mp);

		// Wait until event playing in the remote stream
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));

		// Guard time to play the video
		Thread.sleep(SECONDS.toMillis(PLAYTIME));

		// Release Media Pipeline #1
		saveGstreamerDot(mp);
		recorderEP.stop();
		mp.release();

		// Reloading browser
		getPage().reload();

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2,
				Protocol.FILE + recordingFile).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP2.connect(webRtcEP2);

		// Test execution #2. Playback
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP2, webRtcChannel, WebRtcMode.RCV_ONLY);
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
				getPage().waitForEvent("playing"));
		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(getPage().getTimeout(), SECONDS));

		double currentTime = getPage().getCurrentTime();
		Assert.assertTrue(
				"Error in play time in the recorded video (expected: "
						+ playtime + " sec, real: " + currentTime + " sec) "
						+ messageAppend,
				getPage().compare(playtime, currentTime));

		AssertMedia.assertCodecs(recordingFile, expectedVideoCodec,
				expectedAudioCodec);

		AssertMedia.assertGeneralDuration(recordingFile,
				SECONDS.toMillis(playtime),
				SECONDS.toMillis(getPage().getThresholdTime()));

		if (webRtcChannel == AUDIO_AND_VIDEO || webRtcChannel == AUDIO_ONLY) {
			AssertMedia.assertAudioDuration(recordingFile,
					SECONDS.toMillis(playtime),
					SECONDS.toMillis(getPage().getThresholdTime()));
		}

		// Release Media Pipeline #2
		mp2.release();

		success = true;
	}

}
