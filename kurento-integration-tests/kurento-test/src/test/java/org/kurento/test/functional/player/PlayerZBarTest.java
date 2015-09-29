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
package org.kurento.test.functional.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.CodeFoundEvent;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.ZBarFilter;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.ConsoleLogLevel;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * <strong>Description</strong>: Test of a Player with ZBar Filter.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> ZBarFilter -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time should be the expected</li>
 * <li>CodeFound events received</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class PlayerZBarTest extends FunctionalTest {

	private static final int PLAYTIME = 13; // seconds

	public PlayerZBarTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testPlayerZBar() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/barcodes.webm").build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
		ZBarFilter zBarFilter = new ZBarFilter.Builder(mp).build();
		playerEP.connect(zBarFilter);
		zBarFilter.connect(webRtcEP);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);
		playerEP.play();

		final List<String> codeFoundEvents = new ArrayList<>();
		zBarFilter.addCodeFoundListener(new EventListener<CodeFoundEvent>() {
			@Override
			public void onEvent(CodeFoundEvent event) {
				String codeFound = event.getValue();

				if (!codeFoundEvents.contains(codeFound)) {
					codeFoundEvents.add(codeFound);
					getPage().consoleLog(ConsoleLogLevel.INFO,
							"Code found: " + codeFound);
				}
			}
		});

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));
		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
		double currentTime = getPage().getCurrentTime();
		Assert.assertTrue("Error in play time (expected: " + PLAYTIME
				+ " sec, real: " + currentTime + " sec)",
				getPage().compare(PLAYTIME, currentTime));
		Assert.assertFalse("No code found by ZBar filter",
				codeFoundEvents.isEmpty());

		// Release Media Pipeline
		mp.release();
	}
}
