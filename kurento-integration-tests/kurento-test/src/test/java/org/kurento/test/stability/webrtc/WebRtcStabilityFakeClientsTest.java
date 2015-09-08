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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.base.KurentoClientTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.ChartWriter;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.LatencyRegistry;
import org.kurento.test.latency.VideoTagType;

/**
 * <strong>Description</strong>: Stability test for WebRTC in loopback during a
 * long time (configurable).<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Color change should be detected on local/remote video tag of browsers
 * </li>
 * <li>Test fail when 3 consecutive latency errors (latency > 3sec) are detected
 * </li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */

public class WebRtcStabilityFakeClientsTest extends BrowserKurentoClientTest {

	private static final int DEFAULT_PLAYTIME = 1; // minutes
	private static final int BANDWITH = 500;
	private static Map<Long, LatencyRegistry> latencyResult = new HashMap<>();

	private int fakeClients;

	public WebRtcStabilityFakeClientsTest(TestScenario testScenario,
			int fakeClients) {
		super(testScenario);
		this.fakeClients = fakeClients;
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		String videoPath = KurentoClientTest.getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m";
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.BROWSER,
				new BrowserClient.Builder().client(Client.WEBRTC)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).video(videoPath).build());

		return Arrays.asList(new Object[][] { { test, 0 }, { test, 20 },
				{ test, 40 }, { test, 60 }, { test, 80 }, { test, 100 },
				{ test, 120 }, { test, 140 }, { test, 160 }, { test, 180 },
				{ test, 200 }, { test, 220 }, { test, 240 } });
	}

	@Test
	public void testWebRtcStabilityFakeClients() throws Exception {
		final int playTime = Integer
				.parseInt(System.getProperty("test.webrtcstability.playtime",
						String.valueOf(DEFAULT_PLAYTIME)));

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(webRtcEndpoint);

		// Fake clients
		addFakeClients(fakeClients, BANDWITH, mp, webRtcEndpoint);

		// Latency control
		LatencyController cs = new LatencyController("WebRTC in loopback");

		// WebRTC
		getBrowser().subscribeEvents("playing");
		getBrowser().initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_RCV);

		// Latency assessment
		getBrowser().activateLatencyControl(VideoTagType.LOCAL.getId(),
				VideoTagType.REMOTE.getId());
		cs.checkLocalLatency(playTime, TimeUnit.MINUTES, getBrowser());

		// Release Media Pipeline
		mp.release();

		// Draw latency results (PNG chart and CSV file)
		cs.drawChart(getDefaultOutputFile("-" + fakeClients + ".png"), 500,
				270);
		cs.writeCsv(getDefaultOutputFile("-" + fakeClients + ".csv"));
		cs.logLatencyErrorrs();

		// Latency average
		Map<Long, LatencyRegistry> latencyMap = cs.getLatencyMap();
		long avgLatency = 0;
		for (LatencyRegistry lr : latencyMap.values()) {
			avgLatency += lr.getLatency();
		}
		avgLatency /= latencyMap.size();
		latencyResult.put((long) fakeClients, new LatencyRegistry(avgLatency));
	}

	@AfterClass
	public static void teardown() throws IOException {
		// Write csv
		PrintWriter pw = new PrintWriter(
				new FileWriter(getDefaultOutputFile("-latency.csv")));
		for (long time : latencyResult.keySet()) {
			pw.println(time + "," + latencyResult.get(time).getLatency());
		}
		pw.close();

		// Draw chart
		ChartWriter chartWriter = new ChartWriter(latencyResult, "Latency avg",
				"Number of client(s)", "Latency (ms)");
		chartWriter.drawChart(getDefaultOutputFile("-latency.png"), 500, 207);
	}

}
