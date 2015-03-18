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
package org.kurento.test.functional.webrtc;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.ConsoleLogLevel;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.TestScenario;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * <strong>Description</strong>: WebRTC to HTTP switch. Test KMS is able to
 * dynamically switch many WebRTC flows to a single HTTP endpoint Setup. Two
 * clients WebRTC send-only with audio/video: A,B. One HTTP-EP: H. Switch
 * between following scenarios: A to H, B to H. At least two round.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>2xWebRtcEndpoint -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browsers starts before default timeout</li>
 * <li>Color received by HttpPlayer should be green (RGB #008700, video test of
 * Chrome)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class WebRtc2HttpSwitchTest extends FunctionalTest {

	private static final int PLAYTIME = 10; // seconds

	public WebRtc2HttpSwitchTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {

		// Test: 3 browsers
		TestScenario test = new TestScenario();
		test.addBrowser(
				BrowserConfig.BROWSER,
				new BrowserClient.Builder().client(Client.WEBRTC)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).build());
		test.addBrowser(
				BrowserConfig.PRESENTER,
				new BrowserClient.Builder().client(Client.WEBRTC)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).build());
		test.addBrowser(
				BrowserConfig.VIEWER,
				new BrowserClient.Builder().client(Client.WEBRTC)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).client(Client.PLAYER)
						.build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Ignore
	@Test
	public void testWebRtc2HttpSwitchChrome() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(mp)
				.build();

		// WebRTC
		getBrowser().subscribeEvents("playing");
		getBrowser().initWebRtc(webRtcEndpoint1, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_ONLY);
		getPresenter().subscribeEvents("playing");
		getPresenter().initWebRtc(webRtcEndpoint2,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		// Round #1: Connecting WebRTC #1 to HttpEnpoint
		webRtcEndpoint1.connect(httpGetEndpoint);
		getViewer().consoleLog(ConsoleLogLevel.info,
				"Connecting to WebRTC #1 source");

		getViewer().subscribeEvents("playing");
		getViewer().start(httpGetEndpoint.getUrl());
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getViewer().waitForEvent("playing"));
		Assert.assertTrue(
				"The color of the video should be green (RGB #008700)",
				getViewer().similarColor(CHROME_VIDEOTEST_COLOR));

		// Guard time to see stream from WebRTC #1
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Round #2: Connecting WebRTC #2 to HttpEnpoint
		webRtcEndpoint2.connect(httpGetEndpoint);
		getViewer().consoleLog(ConsoleLogLevel.info,
				"Switching to WebRTC #2 source");

		// Guard time to see stream from WebRTC #2
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		Assert.assertTrue(
				"The color of the video should be green (RGB #008700)",
				getViewer().similarColor(CHROME_VIDEOTEST_COLOR));

		// Release Media Pipeline
		mp.release();
	}
}
