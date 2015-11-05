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
package org.kurento.test.functional.player;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Test of a the stop/release features for a PlayerEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) During the playback of a stream from a PlayerEndpoint to a
 * WebRtcEndpoint, the PlayerEndpoint is stopped/released <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · EndOfStream event cannot be received since the stop is done before the end
 * of the video <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class PlayerEndTest extends FunctionalTest {

	public PlayerEndTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	private enum PlayerOperation {
		STOP, RELEASE;
	}

	@Test
	public void testPlayerStop() throws Exception {
		doTest(PlayerOperation.STOP);
	}

	@Test
	public void testPlayerRelease() throws Exception {
		doTest(PlayerOperation.RELEASE);
	}

	public void doTest(PlayerOperation playerOperation) throws Exception {
		// Test data
		final String mediaUrl = "http://files.kurento.org/video/format/small.webm";
		final int guardTimeSeconds = 10;

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, mediaUrl)
				.build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
		playerEP.connect(webRtcEP);

		// Subscription to EOS event
		final boolean[] eos = new boolean[1];
		eos[0] = false;
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				log.error("EOS event received: {} {}", event.getType(),
						event.getTimestamp());
				eos[0] = true;
			}
		});

		// WebRTC in receive-only mode
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);
		playerEP.play();
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));

		// Stop/release stream and wait x seconds
		switch (playerOperation) {
		case STOP:
			playerEP.stop();
			break;
		case RELEASE:
			playerEP.release();
			break;
		}
		Thread.sleep(TimeUnit.SECONDS.toMillis(guardTimeSeconds));

		// Verify that EOS event has not being received
		Assert.assertFalse(
				"EOS event has been received. "
						+ "This should not be happenning because the stream has been stopped",
				eos[0]);

		// Release Media Pipeline
		mp.release();
	}
}
