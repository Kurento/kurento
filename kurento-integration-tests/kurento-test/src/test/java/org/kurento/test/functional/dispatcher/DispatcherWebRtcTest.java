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

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Dispatcher;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.TestScenario;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

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

	public DispatcherWebRtcTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localPresenterAndViewer();
	}

	@Test
	public void testDispatcherWebRtc() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();

		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();

		webRtcEP1.connect(hubPort1);
		hubPort2.connect(webRtcEP2);

		dispatcher.connect(hubPort1, hubPort2);

		// Test execution
		getPresenter().initWebRtc(webRtcEP1, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_ONLY);

		getViewer().subscribeEvents("playing");
		getViewer().initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);

		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getViewer().waitForEvent("playing"));
		Assert.assertTrue("The color of the video should be green", getViewer()
				.similarColor(CHROME_VIDEOTEST_COLOR));

		// Release Media Pipeline
		mp.release();
	}
}
