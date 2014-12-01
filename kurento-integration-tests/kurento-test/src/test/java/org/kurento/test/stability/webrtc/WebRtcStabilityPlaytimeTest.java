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
import org.kurento.test.latency.VideoTag;

/**
 * <strong>Description</strong>: Stability test for WebRTC in loopback during a
 * long time (configurable).<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No latency problems detected during test time</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */

public class WebRtcStabilityPlaytimeTest extends StabilityTest {

	private static final int DEFAULT_PLAYTIME = 30; // minutes

	@Test
	public void testWebRtcStabilityChrome() throws InterruptedException,
			IOException {
		final int playTime = Integer.parseInt(System.getProperty(
				"test.webrtcstability.playtime",
				String.valueOf(DEFAULT_PLAYTIME)));
		doTest(Browser.CHROME, getPathTestFiles() + "/video/15sec/rgb.y4m",
				playTime);
	}

	public void doTest(Browser browserType, String videoPath, int playTime)
			throws InterruptedException, IOException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);
		if (videoPath != null) {
			builder = builder.video(videoPath);
		}

		LatencyController cs = new LatencyController("WebRTC in loopback");

		try (BrowserClient browser = builder.build()) {
			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_RCV);

			// Latency control
			try {
				browser.addChangeColorEventListener(VideoTag.LOCAL, cs);
				browser.addChangeColorEventListener(VideoTag.REMOTE, cs);
				cs.checkLatency(playTime, TimeUnit.MINUTES);
			} catch (RuntimeException re) {
				browser.takeScreeshot(getDefaultOutputFile("-error-screenshot.png"));
				Assert.fail(re.getMessage());
			}
		}

		// Draw latency results (PNG chart and CSV file)
		cs.drawChart(getDefaultOutputFile(".png"), 500, 270);
		cs.writeCsv(getDefaultOutputFile(".csv"));
		cs.logLatencyErrorrs();

		// Release Media Pipeline
		mp.release();
	}
}
