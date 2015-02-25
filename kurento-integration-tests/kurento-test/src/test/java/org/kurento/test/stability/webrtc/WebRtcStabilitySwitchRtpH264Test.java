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
package org.kurento.test.stability.webrtc;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.KurentoClientTest;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestConfig;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;

/**
 * <strong>Description</strong>: Stability test for switching a WebRTC connected
 * to RTP performing H264 transcoding.<br/>
 * <strong>Pipeline(s)</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> RtpEndpoint1</li>
 * <li>RtpEndpoint1 -> RtpEndpoint2 (RTP session)</li>
 * <li>RtpEndpoint2 -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the remote video tag</li>
 * <li>Color change should be detected on local and remote video tags</li>
 * <li>Test fail when 3 consecutive latency errors (latency > 3sec) are detected
 * </li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class WebRtcStabilitySwitchRtpH264Test extends StabilityTest {

	private static final int DEFAULT_PLAYTIME = 30; // minutes
	private static final String[] REMOVE_CODECS = { "H263-1998", "VP8",
			"MP4V-ES" };

	public WebRtcStabilitySwitchRtpH264Test(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		String videoPath = KurentoClientTest.getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m";
		TestScenario test = new TestScenario();
		test.addBrowser(TestConfig.DEFAULT_BROWSER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.video(videoPath).build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testWebRtcStabilitySwitchRtpH264() throws Exception {
		final int playTime = Integer.parseInt(System.getProperty(
				"test.webrtc.stability.switch.webrtc2rtp.playtime",
				String.valueOf(DEFAULT_PLAYTIME)));

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		RtpEndpoint rtpEndpoint1 = new RtpEndpoint.Builder(mp).build();
		RtpEndpoint rtpEndpoint2 = new RtpEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(rtpEndpoint1);
		rtpEndpoint2.connect(webRtcEndpoint);

		// RTP session (rtpEndpoint1 --> rtpEndpoint2)
		String sdpOffer = rtpEndpoint1.generateOffer();
		log.info("SDP offer in rtpEndpoint1\n{}", sdpOffer);

		// SDP mangling
		sdpOffer = mangleSdp(sdpOffer, REMOVE_CODECS);
		log.info("SDP offer in rtpEndpoint1 after mangling\n{}", sdpOffer);

		String sdpAnswer1 = rtpEndpoint2.processOffer(sdpOffer);
		log.info("SDP answer in rtpEndpoint2\n{}", sdpAnswer1);
		String sdpAnswer2 = rtpEndpoint1.processAnswer(sdpAnswer1);
		log.info("SDP answer in rtpEndpoint1\n{}", sdpAnswer2);

		// Latency controller
		final LatencyController cs = new LatencyController();

		// Test execution
		subscribeEvents(TestConfig.DEFAULT_BROWSER, "playing");
		initWebRtc(TestConfig.DEFAULT_BROWSER, webRtcEndpoint,
				WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);

		// Assertion: wait to playing event in browser
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				waitForEvent(TestConfig.DEFAULT_BROWSER, "playing"));

		// Latency assessment
		try {
			cs.activateLocalLatencyAssessmentIn(testScenario.getBrowserMap()
					.get(TestConfig.DEFAULT_BROWSER));
			cs.checkLatencyInBackground(playTime, TimeUnit.MINUTES);
		} catch (RuntimeException re) {
			Assert.fail(re.getMessage());
		}

		// Connect-disconnect each second
		for (int i = 0; i < DEFAULT_PLAYTIME * 60; i++) {
			Thread.sleep(1000);
			rtpEndpoint2.disconnect(webRtcEndpoint);
			rtpEndpoint2.connect(webRtcEndpoint);
		}

		// Release Media Pipeline
		mp.release();

		// Draw latency results (PNG chart and CSV file)
		cs.drawChart(getDefaultOutputFile(".png"), 500, 270);
		cs.writeCsv(getDefaultOutputFile(".csv"));
		cs.logLatencyErrorrs();
	}

}
