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

	public WebRtcStabilityBack2BackTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localPresenterAndViewer();
	}

	@Test
	public void testWebRtcSwitchChrome() throws InterruptedException,
			IOException {
		final int playTime = Integer.parseInt(System.getProperty(
				"test.webrtc.stability.back2back.playtime",
				String.valueOf(DEFAULT_PLAYTIME)));

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint1.connect(webRtcEndpoint2);
		webRtcEndpoint2.connect(webRtcEndpoint1);

		// Latency control
		final LatencyController cs1 = new LatencyController(
				"WebRTC latency in browser 1");
		LatencyController cs2 = new LatencyController(
				"WebRTC latency in browser 2");

		getPresenter().subscribeLocalEvents("playing");
		getPresenter().initWebRtc(webRtcEndpoint1, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_ONLY);
		getViewer().subscribeEvents("playing");
		getViewer().initWebRtc(webRtcEndpoint2, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.RCV_ONLY);

		try {
			cs1.checkRemoteLatencyInBackground(playTime, TimeUnit.MINUTES,
					getViewer().getBrowserClient().getJs(), getPresenter()
							.getBrowserClient().getJs());

			cs2.checkRemoteLatencyInBackground(playTime, TimeUnit.MINUTES,
					getPresenter().getBrowserClient().getJs(), getViewer()
							.getBrowserClient().getJs());

		} catch (RuntimeException re) {
			Assert.fail(re.getMessage());
		}

		// Release Media Pipeline
		mp.release();

		// Draw latency results (PNG chart and CSV file)
		cs1.drawChart(getDefaultOutputFile("-browser1.png"), 500, 270);
		cs1.writeCsv(getDefaultOutputFile("-browser1.csv"));
		cs1.logLatencyErrorrs();

		cs2.drawChart(getDefaultOutputFile("-browser2.png"), 500, 270);
		cs2.writeCsv(getDefaultOutputFile("-browser2.csv"));
		cs2.logLatencyErrorrs();
	}
}
