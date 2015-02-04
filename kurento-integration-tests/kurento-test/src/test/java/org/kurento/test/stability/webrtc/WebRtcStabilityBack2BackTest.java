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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.latency.LatencyController;

/**
 * <strong>Description</strong>: Stability test for switching a WebRTC in one to
 * one communication.<br/>
 * <strong>Pipeline(s)</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (back-to-back)(x2)</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Color change should be detected on local/remote video tag of browsers</li>
 * <li>Test fail when 3 consecutive latency errors (latency > 3sec) are detected
 * </li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */

public class WebRtcStabilityBack2BackTest extends StabilityTest {

	private static final int DEFAULT_PLAYTIME = 30; // minutes

	@Test
	public void testWebRtcSwitchChrome() throws InterruptedException,
			IOException {
		final int playTime = Integer.parseInt(System.getProperty(
				"test.webrtc.stability.back2back.playtime",
				String.valueOf(DEFAULT_PLAYTIME)));
		doTest(Browser.CHROME, getPathTestFiles() + "/video/15sec/rgb.y4m",
				playTime);
	}

	public void doTest(Browser browserType, String videoPath, final int playTime)
			throws InterruptedException, IOException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint1.connect(webRtcEndpoint2);
		webRtcEndpoint2.connect(webRtcEndpoint1);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);
		if (videoPath != null) {
			builder = builder.video(videoPath);
		}

		// Latency control
		final LatencyController cs1 = new LatencyController(
				"WebRTC latency in browser 1");
		LatencyController cs2 = new LatencyController(
				"WebRTC latency in browser 2");

		try (BrowserClient browser1 = builder.build();
				BrowserClient browser2 = builder.build()) {
			browser1.subscribeEvents("playing");
			browser1.initWebRtc(webRtcEndpoint1, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_RCV);
			browser2.subscribeEvents("playing");
			browser2.initWebRtc(webRtcEndpoint2, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_RCV);

			try {
				cs1.activateRemoteLatencyAssessmentIn(browser2, browser1);
				cs1.checkLatencyInBackground(playTime, TimeUnit.MINUTES);

				cs2.activateRemoteLatencyAssessmentIn(browser1, browser2);
				cs2.checkLatency(playTime, TimeUnit.MINUTES);
			} catch (RuntimeException re) {
				Assert.fail(re.getMessage());
			}
		} finally {
			// Release Media Pipeline
			mp.release();
		}

		// Draw latency results (PNG chart and CSV file)
		cs1.drawChart(getDefaultOutputFile("-browser1.png"), 500, 270);
		cs1.writeCsv(getDefaultOutputFile("-browser1.csv"));
		cs1.logLatencyErrorrs();

		cs2.drawChart(getDefaultOutputFile("-browser2.png"), 500, 270);
		cs2.writeCsv(getDefaultOutputFile("-browser2.csv"));
		cs2.logLatencyErrorrs();
	}
}
