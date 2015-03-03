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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;

/**
 * <strong>Description</strong>: Stability test for switching 2 WebRTC (looback
 * to back-2-back) a configurable number of times (each switch holds 1 second).<br/>
 * <strong>Pipeline(s)</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * <li>... to:</li>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (back to back)</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Color change should be detected on local/remote video tag of browsers</li>
 * <li>Test fail when 3 consecutive latency errors (latency > 3sec) are detected
 * </li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */

public class WebRtcStabilitySwitchTest extends StabilityTest {

	/**
	 * test time = PLAYTIME_PER_SWITCH * 2 * DEFAULT_NUM_SWITCH
	 * 
	 * DEFAULT_NUM_SWITCH = 2 --> test time = 1 minute <br/>
	 * DEFAULT_NUM_SWITCH = 120 --> test time = 1 hour
	 */
	private static final int DEFAULT_NUM_SWITCH = 60;
	private static final int PLAYTIME_PER_SWITCH = 15; // seconds

	public WebRtcStabilitySwitchTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localPresenterAndViewerRGB();
	}

	@Test
	public void testWebRtcStabilitySwitch() throws Exception {
		final int numSwitch = Integer.parseInt(System.getProperty(
				"test.webrtcstability.switch",
				String.valueOf(DEFAULT_NUM_SWITCH)));

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint1.connect(webRtcEndpoint1);
		webRtcEndpoint2.connect(webRtcEndpoint2);

		// WebRTC
		getPresenter().subscribeEvents("playing");
		getPresenter().initWebRtc(webRtcEndpoint1, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_RCV);
		getViewer().subscribeEvents("playing");
		getViewer().initWebRtc(webRtcEndpoint2, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_RCV);

		// Latency controller
		LatencyController cs1 = new LatencyController("Latency in Browser 1");
		LatencyController cs2 = new LatencyController("Latency in Browser 2");

		try {
			for (int i = 0; i < numSwitch; i++) {

				if (i % 2 == 0) {
					log.debug("Switch #" + i + ": loopback");
					webRtcEndpoint1.connect(webRtcEndpoint1);
					webRtcEndpoint2.connect(webRtcEndpoint2);

					// Latency control (loopback)
					log.debug("[{}.1] Latency control of browser1 to browser1",
							i);
					cs1.checkLocalLatency(PLAYTIME_PER_SWITCH,
							TimeUnit.SECONDS, getPresenter());

					log.debug("[{}.2] Latency control of browser2 to browser2",
							i);
					cs2.checkLocalLatency(PLAYTIME_PER_SWITCH,
							TimeUnit.SECONDS, getViewer());

				} else {
					log.debug("Switch #" + i + ": B2B");
					webRtcEndpoint1.connect(webRtcEndpoint2);
					webRtcEndpoint2.connect(webRtcEndpoint1);

					// Latency control (B2B)
					log.debug("[{}.3] Latency control of browser1 to browser2",
							i);
					cs1.checkRemoteLatency(PLAYTIME_PER_SWITCH,
							TimeUnit.SECONDS, getPresenter(), getViewer());

					log.debug("[{}.4] Latency control of browser2 to browser1",
							i);
					cs2.checkRemoteLatency(PLAYTIME_PER_SWITCH,
							TimeUnit.SECONDS, getViewer(), getPresenter());
				}
			}
		} catch (RuntimeException re) {
			getPresenter().takeScreeshot(
					getDefaultOutputFile("-browser1-error-screenshot.png"));
			getViewer().takeScreeshot(
					getDefaultOutputFile("-browser2-error-screenshot.png"));
			Assert.fail(re.getMessage());
		}

		// Draw latency results (PNG chart and CSV file)
		cs1.drawChart(getDefaultOutputFile("-browser1.png"), 500, 270);
		cs1.writeCsv(getDefaultOutputFile("-browser1.csv"));
		cs1.logLatencyErrorrs();

		cs2.drawChart(getDefaultOutputFile("-browser2.png"), 500, 270);
		cs2.writeCsv(getDefaultOutputFile("-browser2.csv"));
		cs2.logLatencyErrorrs();

		// Release Media Pipeline
		mp.release();
	}
}
