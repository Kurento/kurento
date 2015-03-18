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
package org.kurento.test.functional.dispatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Dispatcher;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.TestScenario;

/**
 * 
 * <strong>Description</strong>: A Chrome browser opens a WebRtcEndpoint and
 * this stream is connected through a Dispatcher to an HttpGetEndpoint, played
 * in another browser.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> Dispatcher -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherHttpTest extends FunctionalTest {

	private static final int PLAYTIME = 5; // seconds to play in WebRTC

	public DispatcherHttpTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		// Test: 2 browsers
		TestScenario test = new TestScenario();
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
	public void testDispatcherHttp() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		HttpGetEndpoint httpEP = new HttpGetEndpoint.Builder(mp)
				.terminateOnEOS().build();

		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();

		webRtcEP1.connect(hubPort1);
		hubPort2.connect(httpEP);

		dispatcher.connect(hubPort1, hubPort2);

		// Test execution
		getPresenter().initWebRtc(webRtcEP1, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_ONLY);

		getViewer().subscribeEvents("playing");
		getViewer().start(httpEP.getUrl());

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getViewer().waitForEvent("playing"));
		Assert.assertTrue(
				"The color of the video should be green (RGB #008700)",
				getViewer().similarColor(CHROME_VIDEOTEST_COLOR));

		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Release Media Pipeline
		mp.release();
	}
}
