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
package org.kurento.test.grid;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.GridBrowserMediaApiTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.services.AudioChannel;
import org.kurento.test.services.Node;
import org.kurento.test.services.Recorder;

/**
 * <strong>Description</strong>: WebRTC (in loopback) test with Selenium Grid.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser should start before default timeout</li>
 * <li>Play time should be as expected</li>
 * <li>Color received by client should be as expected</li>
 * <li>Perceived audio quality should be fair (PESQMOS)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class GridWebRtcTest extends GridBrowserMediaApiTest {

	private static int PLAYTIME = 10; // seconds to play in WebRTC
	private static int AUDIO_SAMPLE_RATE = 16000; // samples per second
	private static float MIN_PESQ_MOS = 3; // Audio quality (PESQ MOS [1..5])

	public GridWebRtcTest() {
		nodes = new ArrayList<Node>();

		nodes.addAll(getRandomNodes(5, Browser.CHROME));

		// Uncomment these lines to use custom video and audio files:
		// nodes.addAll(getRandomNodes(3, Browser.CHROME, getPathTestFiles()
		// + "/video/10sec/red.y4m",
		// "http://files.kurento.org/audio/10sec/fiware_mono_16khz.wav"));

		// ... or specifying a given node:
		// nodes.add(new Node("epsilon01.aulas.gsyc.es", Browser.CHROME,
		// getPathTestFiles() + "/video/10sec/red.y4m",
		// "http://files.kurento.org/audio/10sec/fiware_mono_16khz.wav"));

		log.info("Node list {} ", nodes);
	}

	@Ignore
	@Test
	public void tesGridWebRtc() throws InterruptedException, ExecutionException {
		ExecutorService exec = Executors.newFixedThreadPool(nodes.size());
		List<Future<?>> results = new ArrayList<>();
		for (final Node node : nodes) {
			results.add(exec.submit(new Runnable() {
				public void run() {
					doTest(node, new Color(0, 135, 0));

					// Uncomment this line to assess custom color video
					// doTest(node, Color.RED);
				}
			}));
		}
		for (Future<?> r : results) {
			r.get();
		}
	}

	public void doTest(Node node, Color color) {
		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder()
				.browser(node.getBrowser()).client(Client.WEBRTC)
				.remoteNode(node);
		if (node.getVideo() != null) {
			builder = builder.video(node.getVideo());
		}
		if (node.getAudio() != null) {
			builder = builder.audio(node.getAudio(), PLAYTIME,
					AUDIO_SAMPLE_RATE, AudioChannel.MONO);
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

		// Assert audio quality
		if (node.getAudio() != null) {
			float realPesqMos = Recorder.getRemotePesqMos(node,
					AUDIO_SAMPLE_RATE);
			Assert.assertTrue(
					"Bad perceived audio quality: PESQ MOS too low (expected="
							+ MIN_PESQ_MOS + ", real=" + realPesqMos + ")",
					realPesqMos >= MIN_PESQ_MOS);
		}
	}

}
