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
package org.kurento.test.quality.webrtc;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.KurentoClientWebPageTest;
import org.kurento.test.base.QualityTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.AudioChannel;
import org.kurento.test.services.Recorder;

/**
 * <strong>Description</strong>: WebRTC in loopback using custom video and audio
 * files.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser should start before default timeout</li>
 * <li>Play time should be as expected</li>
 * <li>Color received by client should be as expected</li>
 * <li>Perceived audio quality should be fair (PESQMOS)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */

public class WebRtcQualityLoopbackTest extends QualityTest {

	private static int PLAYTIME = 10; // seconds to play in WebRTC
	private static int AUDIO_SAMPLE_RATE = 16000; // samples per second
	private static float MIN_PESQ_MOS = 3; // Audio quality (PESQ MOS [1..5])

	public WebRtcQualityLoopbackTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		String videoPath = KurentoClientWebPageTest.getPathTestFiles()
				+ "/video/10sec/red.y4m";
		String audioUrl = "http://files.kurento.org/audio/10sec/fiware_mono_16khz.wav";
		TestScenario test = new TestScenario();
		test.addBrowser(
				BrowserConfig.BROWSER,
				new Browser.Builder()
						.webPageType(WebPageType.WEBRTC)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL)
						.video(videoPath)
						.audio(audioUrl, PLAYTIME, AUDIO_SAMPLE_RATE,
								AudioChannel.MONO).build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Ignore
	@Test
	public void testWebRtcQualityChrome() throws InterruptedException {
		doTest(BrowserType.CHROME, getPathTestFiles() + "/video/10sec/red.y4m",
				"http://files.kurento.org/audio/10sec/fiware_mono_16khz.wav",
				Color.RED);
	}

	public void doTest(BrowserType browserType, String videoPath,
			String audioUrl, Color color) throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(webRtcEndpoint);

		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_RCV);

		// Wait until event playing in the remote stream
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));

		// Guard time to play the video
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Assert play time
		double currentTime = getPage().getCurrentTime();
		Assert.assertTrue("Error in play time of player (expected: " + PLAYTIME
				+ " sec, real: " + currentTime + " sec)",
				getPage().compare(PLAYTIME, currentTime));

		// Assert color
		if (color != null) {
			Assert.assertTrue("The color of the video should be " + color,
					getPage().similarColor(color));
		}

		// Assert audio quality
		if (audioUrl != null) {
			float realPesqMos = Recorder
					.getPesqMos(audioUrl, AUDIO_SAMPLE_RATE);
			Assert.assertTrue(
					"Bad perceived audio quality: PESQ MOS too low (expected="
							+ MIN_PESQ_MOS + ", real=" + realPesqMos + ")",
					realPesqMos >= MIN_PESQ_MOS);
		}

		// Release Media Pipeline
		mp.release();
	}
}
