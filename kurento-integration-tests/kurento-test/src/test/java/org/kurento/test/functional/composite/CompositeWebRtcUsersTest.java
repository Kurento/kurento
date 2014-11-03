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
package org.kurento.test.functional.composite;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.Composite;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * 
 * <strong>Description</strong>: Four synthetic videos are played by four
 * WebRtcEndpoint and mixed by a Composite. The resulting video is played in an
 * WebRtcEndpoint. At the end, a B&N filter is connected in one of the WebRTC's.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>4xWebRtcEndpoint -> Composite -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>First composite mixes a red and a green videos.</li>
 * <li>In the second stage, composite only shows a red video.</li>
 * <li>In the third stage, composite mixes a red and a white videos.</li>
 * <li>Finally color of the video should be the expected (red, , blue, green,
 * and white)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 4.2.3
 */
public class CompositeWebRtcUsersTest extends BrowserKurentoClientTest {

	private static int PLAYTIME = 5;

	@Test
	public void testCompositeWebRtcChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = new MediaPipeline.Builder(kurentoClient).build();
		WebRtcEndpoint webRtcEPRed = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPGreen = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPBlue = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPWhite = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPComposite = new WebRtcEndpoint.Builder(mp)
				.build();

		Composite composite = new Composite.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(composite).build();
		HubPort hubPort2 = new HubPort.Builder(composite).build();
		HubPort hubPort3 = new HubPort.Builder(composite).build();
		HubPort hubPort4 = new HubPort.Builder(composite).build();
		HubPort hubPort5 = new HubPort.Builder(composite).build();

		webRtcEPRed.connect(hubPort1);
		webRtcEPGreen.connect(hubPort2);
		hubPort5.connect(webRtcEPComposite);

		// Test execution
		try (BrowserClient browserComposite = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build();

				BrowserClient browserRed = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/red.y4m")
						.build();
				BrowserClient browserGreen = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/green.y4m")
						.build();
				BrowserClient browserBlue = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/blue.y4m")
						.build();
				BrowserClient browserWhite = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/white.y4m")
						.build();) {

			// WebRTC browsers
			browserRed.initWebRtc(webRtcEPRed, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_ONLY);
			browserGreen.initWebRtc(webRtcEPGreen, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_ONLY);
			browserBlue.initWebRtc(webRtcEPBlue, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_ONLY);
			browserWhite.initWebRtc(webRtcEPWhite, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_ONLY);

			browserComposite.subscribeEvents("playing");
			browserComposite.initWebRtc(webRtcEPComposite,
					WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browserComposite.waitForEvent("playing"));
			Assert.assertTrue("Left part of the video must be red",
					browserComposite.similarColorAt(Color.RED, 0, 200));
			Assert.assertTrue("Upper right part of the video must be green",
					browserComposite.similarColorAt(Color.GREEN, 450, 300));

			hubPort2.release();
			Thread.sleep(3000);

			Assert.assertTrue("All the video must be red",
					browserComposite.similarColorAt(Color.RED, 300, 200));

			webRtcEPWhite.connect(hubPort4);
			Thread.sleep(PLAYTIME * 1000);

			Assert.assertTrue("Left part of the video must be red",
					browserComposite.similarColorAt(Color.RED, 0, 300));
			Assert.assertTrue("Left part of the video must be white",
					browserComposite.similarColorAt(Color.WHITE, 450, 300));

			hubPort4.release();
			hubPort2 = new HubPort.Builder(composite).build();
			hubPort4 = new HubPort.Builder(composite).build();

			webRtcEPGreen.connect(hubPort2);
			webRtcEPBlue.connect(hubPort3);
			webRtcEPWhite.connect(hubPort4);
			Thread.sleep(PLAYTIME * 1000);

			Assert.assertTrue("Upper left part of the video must be red",
					browserComposite.similarColorAt(Color.RED, 0, 0));
			Assert.assertTrue("Upper right part of the video must be green",
					browserComposite.similarColorAt(Color.BLUE, 450, 0));
			Assert.assertTrue("Lower left part of the video must be blue",
					browserComposite.similarColorAt(Color.GREEN, 0, 450));
			Assert.assertTrue("Lower right part of the video must be white",
					browserComposite.similarColorAt(Color.WHITE, 450, 450));
		}

		// Release Media Pipeline
		mp.release();
	}

}