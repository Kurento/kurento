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
package com.kurento.kmf.test.grid;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.GridBrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.WebRtcChannel;
import com.kurento.kmf.test.services.Node;

/**
 * WebRTC test with Selenium Grid.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@RunWith(Parameterized.class)
public class GridWebRtcTest extends GridBrowserMediaApiTest {

	private static final int PLAYTIME = 5; // seconds to play in HTTP player

	@Parameters
	public static Collection<Object[]> data() {
		List<Node> nodeList = new ArrayList<Node>();
		nodeList.addAll(addNodes(5, Browser.CHROME));
		log.info("Node list {} ", nodeList);

		Object[][] data = new Object[][] { { nodeList } };
		return Arrays.asList(data);
	}

	public GridWebRtcTest(List<Node> nodes) {
		this.nodes = nodes;
	}

	@Test
	public void tesGridWebRtc() throws InterruptedException, ExecutionException {
		ExecutorService exec = Executors.newFixedThreadPool(nodes.size());
		List<Future<?>> results = new ArrayList<>();
		for (final Node n : nodes) {
			results.add(exec.submit(new Runnable() {
				@Override
				public void run() {
					doTest(n.getBrowser(), null, new Color(0, 135, 0));
				}
			}));
		}

		for (Future<?> r : results) {
			r.get();
		}

	}

	public void doTest(Browser browserType, String video, Color color) {
		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).remoteTest();
		if (video != null) {
			builder = builder.video(video);
		}

		try (BrowserClient browser = builder.build()) {
			browser.subscribeEvents("playing");
			browser.connectToWebRtcEndpoint(webRtcEndpoint,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Wait until event playing in the remote stream
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// Guard time to play the video
			Thread.sleep(PLAYTIME * 1000);

			// Assert play time
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time of HTTP player (expected: "
					+ PLAYTIME + " sec, real: " + currentTime + " sec)",
					currentTime >= PLAYTIME);

			// Assert color
			if (color != null) {
				Assert.assertTrue("The color of the video should be " + color,
						browser.colorSimilarTo(color));
			}
		} catch (InterruptedException e) {
			Assert.fail("InterruptedException " + e.getMessage());
		}

	}

}
