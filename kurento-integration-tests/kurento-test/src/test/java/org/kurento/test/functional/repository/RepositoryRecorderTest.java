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
package org.kurento.test.functional.repository;

import java.awt.Color;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.test.base.RepositoryKurentoClientTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 *
 * <strong>Description</strong>: Test of a Recorder in the repository, using the
 * stream source from a PlayerEndpoint through an WebRtcEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time should be the expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.4
 */
public class RepositoryRecorderTest extends RepositoryKurentoClientTest {

	private static final int PLAYTIME = 10; // seconds
	private static final int TIMEOUT_EOS = 60; // seconds

	@Test
	public void testRecorderRepositoryChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testRecorderRepositoryFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/ball.webm").build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();

		RepositoryItem repositoryItem = repository.createRepositoryItem();
		RepositoryHttpRecorder recorder = repositoryItem
				.createRepositoryHttpRecorder();

		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				recorder.getURL()).build();
		playerEP.connect(webRtcEP1);
		playerEP.connect(recorderEP);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution #1. Play the video while it is recorded
		launchBrowser(browserType, webRtcEP1, playerEP, recorderEP);

		// Wait for EOS
		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));

		// Release Media Pipeline #1
		recorderEP.stop();
		mp.release();
	}

	private void launchBrowser(Browser browserType, WebRtcEndpoint webRtcEP,
			PlayerEndpoint playerEP, RecorderEndpoint recorderEP)
			throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);
			playerEP.play();
			final CountDownLatch eosLatch = new CountDownLatch(1);
			playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
				@Override
				public void onEvent(EndOfStreamEvent event) {
					eosLatch.countDown();
				}
			});

			if (recorderEP != null) {
				recorderEP.record();
			}

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			Assert.assertTrue("The color of the video should be black",
					browser.similarColor(Color.BLACK));
			Assert.assertTrue("Not received EOS event in player",
					eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));
		}
	}
}
