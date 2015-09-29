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

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Dispatcher;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;

/**
 * 
 * <strong>Description</strong>: A WebRtcEndpoint is connected to another
 * WebRtcEndpoint through a Dispatcher.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherWebRtcTest extends FunctionalTest {

	private static final int PLAYTIME = 10; // seconds
	private static final String BROWSER1 = "browser1";
	private static final String BROWSER2 = "browser2";
	private static final String BROWSER3 = "browser3";

	public DispatcherWebRtcTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		TestScenario test = new TestScenario();

		test.addBrowser(BROWSER1,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL)
						.build());
		test.addBrowser(
				BROWSER2,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL)
						.video(getPathTestFiles() + "/video/10sec/green.y4m")
						.build());
		test.addBrowser(
				BROWSER3,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL)
						.video(getPathTestFiles() + "/video/10sec/blue.y4m")
						.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testDispatcherWebRtc() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP3 = new WebRtcEndpoint.Builder(mp).build();

		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort3 = new HubPort.Builder(dispatcher).build();

		webRtcEP1.connect(hubPort1);
		webRtcEP3.connect(hubPort3);
		hubPort2.connect(webRtcEP2);

		dispatcher.connect(hubPort1, hubPort2);

		// Test execution
		getPage(BROWSER2).initWebRtc(webRtcEP1, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_ONLY);
		getPage(BROWSER3).initWebRtc(webRtcEP3, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_ONLY);

		getPage(BROWSER1).subscribeEvents("playing");
		getPage(BROWSER1).initWebRtc(webRtcEP2, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.RCV_ONLY);

		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage(BROWSER1).waitForEvent("playing"));

		Assert.assertTrue("The color of the video should be green",
				getPage(BROWSER1).similarColor(Color.GREEN));

		Thread.sleep(5000);
		dispatcher.connect(hubPort3, hubPort2);

		Assert.assertTrue("The color of the video should be blue (BLUE)",
				getPage(BROWSER1).similarColor(Color.BLUE));

		Thread.sleep(2000);

		// Release Media Pipeline
		mp.release();
	}
}
