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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestConfig;
import org.kurento.test.config.TestScenario;

/**
 * <strong>Description</strong>: WebRTC connected to N HttpEndpoint.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> NxHttpEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browsers starts before default timeout</li>
 * <li>HttpPlayer play time does not differ in a 10% of the transmitting time by
 * WebRTC</li>
 * <li>Color received by HttpPlayer should be green (RGB #008700, video test of
 * Chrome)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class WebRtc2HttpTest extends FunctionalTest {

	private static int PLAYTIME = 10; // seconds to play in player
	private static int NPLAYERS = 2; // number of HttpEndpoint connected to

	public WebRtc2HttpTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {

		// Test: 1+nViewers local Chrome's
		TestScenario test = new TestScenario();
		test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());

		for (int i = 0; i < NPLAYERS; i++) {
			test.addBrowser(TestConfig.VIEWER + i, new BrowserClient.Builder()
					.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
					.client(Client.PLAYER).build());
		}
		return Arrays.asList(new Object[][] { { test } });
	}

	@Ignore
	@Test
	public void testWebRtc2Http() throws Exception {
		// Media Pipeline
		final MediaPipeline mp = kurentoClient.createMediaPipeline();
		final WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp)
				.build();

		// Test execution
		getPresenter().initWebRtc(webRtcEndpoint,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		// Players
		ExecutorService exec = Executors.newFixedThreadPool(NPLAYERS);
		List<Future<?>> results = new ArrayList<>();
		for (int i = 0; i < NPLAYERS; i++) {
			final int j = i;
			results.add(exec.submit(new Runnable() {
				@Override
				public void run() {
					HttpGetEndpoint httpEP = new HttpGetEndpoint.Builder(mp)
							.build();
					webRtcEndpoint.connect(httpEP);
					try {
						createPlayer(TestConfig.VIEWER + j, httpEP.getUrl());
					} catch (InterruptedException e) {
						Assert.fail("Exception creating players: "
								+ e.getClass().getName());
					}
				}
			}));
		}
		for (Future<?> r : results) {
			r.get();
		}

		// Release Media Pipeline
		mp.release();
	}

	private void createPlayer(String browserId, String url)
			throws InterruptedException {
		getBrowser(browserId).subscribeEvents("playing");
		getBrowser(browserId).start(url);

		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getBrowser(browserId).waitForEvent("playing"));

		// Guard time to see the video
		Thread.sleep(PLAYTIME * 1000);

		// Assertions
		double currentTime = getBrowser(browserId).getCurrentTime();
		Assert.assertTrue("Error in play time (expected: " + PLAYTIME
				+ " sec, real: " + currentTime + " sec)", getBrowser(browserId)
				.compare(PLAYTIME, currentTime));
		Assert.assertTrue(
				"The color of the video should be green (RGB #008700)",
				getBrowser(browserId).similarColor(CHROME_VIDEOTEST_COLOR));

		getBrowser(browserId).stop();
	}
}
