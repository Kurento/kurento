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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 *
 * Base for recorder tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class Recorder extends FunctionalTest {

	protected static final String EXPECTED_VIDEO_CODEC_WEBM = "VP8";
	protected static final String EXPECTED_VIDEO_CODEC_MP4 = "AVC";
	protected static final String EXPECTED_AUDIO_CODEC_WEBM = "Vorbis";
	protected static final String EXPECTED_AUDIO_CODEC_MP4 = "MPEG Audio";
	protected static final String EXTENSION_WEBM = ".webm";
	protected static final String EXTENSION_MP4 = ".mp4";

	protected boolean success = false;
	protected String gstreamerDot;
	protected String pipelineName;

	public Recorder(TestScenario testScenario) {
		super(testScenario);
	}

	@After
	public void storeGStreamerDot() throws IOException {
		if (!success) {
			String gstreamerDotFile = getDefaultOutputFile(
					"-before-stop-recording-" + pipelineName);
			FileUtils.writeStringToFile(new File(gstreamerDotFile),
					gstreamerDot);
		}
	}

	protected void launchBrowser(MediaPipeline mp, WebRtcEndpoint webRtcEP,
			PlayerEndpoint playerEP, RecorderEndpoint recorderEP,
			String expectedVideoCodec, String expectedAudioCodec,
			String recordingFile, Color expectedColor, int xColor, int yColor,
			int playTime) throws InterruptedException {

		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);
		playerEP.play();
		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		if (recorderEP != null) {
			recorderEP.record();
		}

		// Assertions
		String inRecording = (recorderEP == null) ? " in the recording" : "";

		Assert.assertTrue("Not received media (timeout waiting playing event)"
				+ inRecording, getPage().waitForEvent("playing"));
		Assert.assertTrue(
				"Color at coordinates " + xColor + "," + yColor + " must be "
						+ expectedColor + inRecording,
				getPage().similarColorAt(expectedColor, xColor, yColor));
		Assert.assertTrue("Not received EOS event in player" + inRecording,
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
		if (recorderEP != null) {

			saveGstreamerDot(mp);
			recorderEP.stop();

			// Guard time to stop the recording
			Thread.sleep(2000);

			AssertMedia.assertCodecs(recordingFile, expectedVideoCodec,
					expectedAudioCodec);

		} else {
			double currentTime = getPage().getCurrentTime();
			Assert.assertTrue(
					"Error in play time in the recorded video (expected: "
							+ playTime + " sec, real: " + currentTime + " sec) "
							+ inRecording,
					getPage().compare(playTime, currentTime));
		}
	}

	protected void saveGstreamerDot(MediaPipeline mp) {
		if (mp != null) {
			gstreamerDot = mp.getGstreamerDot();
			pipelineName = mp.getName();
		}
	}

}
