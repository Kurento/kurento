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
package org.kurento.test.client;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.Composite;
import org.kurento.client.GStreamerFilter;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;

/**
 * 
 * <strong>Description</strong>: Four synthetic videos are played by four
 * WebRtcEndpoint and mixed by a Composite. The resulting video is played in an
 * HttpGetEndpoint. At the end, a B&N filter is connected in one of the
 * WebRTC's.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>4xWebRtcEndpoint -> Composite -> HttpGetEndpoint</li>
 * <li>1xWebRtcEndpoint -> GStreamerFilter</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected (red, green, blue, and white)</li>
 * <li>At the end, one the videos should gray (the one with the B&N filter).</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class CompositeWebRtcTest extends BrowserKurentoClientTest {

	@Ignore
	@Test
	public void testCompositeWebRtcChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP3 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP4 = new WebRtcEndpoint.Builder(mp).build();
		HttpGetEndpoint httpEP = new HttpGetEndpoint.Builder(mp)
				.terminateOnEOS().build();

		Composite composite = new Composite.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(composite).build();
		HubPort hubPort2 = new HubPort.Builder(composite).build();
		HubPort hubPort3 = new HubPort.Builder(composite).build();
		HubPort hubPort4 = new HubPort.Builder(composite).build();
		HubPort hubPort5 = new HubPort.Builder(composite).build();

		webRtcEP1.connect(hubPort1);
		webRtcEP2.connect(hubPort2);
		webRtcEP3.connect(hubPort3);
		webRtcEP4.connect(hubPort4);
		hubPort5.connect(httpEP);

		// Test execution
		try (BrowserClient browserPlayer = new BrowserClient.Builder()
				.browser(browserType).client(Client.PLAYER).build();
				BrowserClient browserWebRtc1 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/red.y4m")
						.build();
				BrowserClient browserWebRtc2 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/green.y4m")
						.build();
				BrowserClient browserWebRtc3 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/blue.y4m")
						.build();
				BrowserClient browserWebRtc4 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/white.y4m")
						.build();) {

			// WebRTC browsers
			browserWebRtc1.subscribeEvents("playing");
			browserWebRtc1.connectToWebRtcEndpoint(webRtcEP1,
					WebRtcChannel.AUDIO_AND_VIDEO);
			browserWebRtc2.subscribeEvents("playing");
			browserWebRtc2.connectToWebRtcEndpoint(webRtcEP2,
					WebRtcChannel.AUDIO_AND_VIDEO);
			browserWebRtc3.subscribeEvents("playing");
			browserWebRtc3.connectToWebRtcEndpoint(webRtcEP3,
					WebRtcChannel.AUDIO_AND_VIDEO);
			browserWebRtc4.subscribeEvents("playing");
			browserWebRtc4.connectToWebRtcEndpoint(webRtcEP4,
					WebRtcChannel.AUDIO_AND_VIDEO);

			browserPlayer.setURL(httpEP.getUrl());
			browserPlayer.subscribeEvents("playing");
			browserPlayer.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browserPlayer.waitForEvent("playing"));
			Assert.assertTrue("Upper left part of the video must be red",
					browserPlayer.similarColorAt(Color.RED, 0, 0));
			Assert.assertTrue("Upper right part of the video must be green",
					browserPlayer.similarColorAt(Color.GREEN, 450, 0));
			Assert.assertTrue("Lower left part of the video must be blue",
					browserPlayer.similarColorAt(Color.BLUE, 0, 450));
			Assert.assertTrue("Lower right part of the video must be white",
					browserPlayer.similarColorAt(Color.WHITE, 450, 450));

			// Finally, a B&N filter is connected in one of the WebRTC's
			GStreamerFilter bn = new GStreamerFilter.Builder(mp,
					"videobalance saturation=0.0").build();
			webRtcEP1.connect(bn);
			bn.connect(hubPort1);
			Thread.sleep(5000);
			Assert.assertTrue(
					"When connecting the filter, the upper left part of the video must be gray",
					browserPlayer.similarColorAt(new Color(75, 75, 75), 0, 0));
		}

		// Release Media Pipeline
		mp.release();
	}

}
