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
package org.kurento.test.base;

import java.awt.Color;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.testing.IntegrationTests;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.KurentoTestClient;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.VideoTagType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Base for tests using kurento-client and HTTP Server.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
@EnableAutoConfiguration
@Category(IntegrationTests.class)
public class BrowserKurentoClientTest extends KurentoClientTest {

	public BrowserKurentoClientTest(TestScenario testScenario) {
		super(testScenario);
		this.setClient(new KurentoTestClient());
	}

	public BrowserKurentoClientTest() {
		super();
	}

	@Override
	public KurentoTestClient getBrowser() {
		return (KurentoTestClient) super.getBrowser();
	}

	@Override
	public KurentoTestClient getBrowser(String browserKey) {
		return (KurentoTestClient) super.getBrowser(browserKey);
	}

	@Override
	public KurentoTestClient getBrowser(int index) {
		return (KurentoTestClient) super.getBrowser(index);
	}

	@Override
	public KurentoTestClient getPresenter() {
		return (KurentoTestClient) super.getPresenter();
	}

	@Override
	public KurentoTestClient getViewer() {
		return (KurentoTestClient) super.getViewer();
	}

	@Override
	public KurentoTestClient getPresenter(int index) {
		return (KurentoTestClient) super.getPresenter(index);
	}

	@Override
	public KurentoTestClient getViewer(int index) {
		return (KurentoTestClient) super.getViewer(index);
	}

	protected void playFileAsLocal(BrowserType browserType,
			String recordingFile, int playtime, int x, int y,
			Color... expectedColors) throws InterruptedException {
		BrowserClient browserClient = new BrowserClient.Builder()
				.browserType(browserType).client(Client.WEBRTC)
				.protocol(Protocol.FILE).build();
		browserClient.init();
		String browserkey = "playBrowser";
		addBrowserClient(browserkey, browserClient);

		getBrowser(browserkey).subscribeEvents("playing");
		browserClient.executeScript("document.getElementById('"
				+ VideoTagType.REMOTE.getId() + "').setAttribute('src', '"
				+ recordingFile + "');");
		browserClient.executeScript("document.getElementById('"
				+ VideoTagType.REMOTE.getId() + "').load();");

		// Assertions
		makeAssertions(browserkey, "[played as local file]", browserClient,
				playtime, x, y, null, expectedColors);
	}

	public void playUrlInVideoTag(BrowserClient browserClient, String url,
			VideoTagType videoTagType) {

	}

	protected void playFileWithPipeline(BrowserType browserType,
			String recordingFile, int playtime, int x, int y,
			Color... expectedColors) throws InterruptedException {

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, recordingFile)
				.build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
		playerEP.connect(webRtcEP);

		// Browser
		BrowserClient browserClient = new BrowserClient.Builder()
				.browserType(browserType).client(Client.WEBRTC).build();
		browserClient.init();
		String browserkey = "playBrowser";
		addBrowserClient(browserkey, browserClient);

		// Play latch
		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution
		getBrowser(browserkey).subscribeEvents("playing");
		getBrowser(browserkey).initWebRtc(webRtcEP,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
		playerEP.play();

		// Assertions
		makeAssertions(browserkey, "[played file with media pipeline]",
				browserClient, playtime, x, y, eosLatch, expectedColors);

		// Release Media Pipeline
		if (mp != null) {
			mp.release();
		}
	}

	private void makeAssertions(String browserKey, String messageAppend,
			BrowserClient browser, int playtime, int x, int y,
			CountDownLatch eosLatch, Color... expectedColors)
			throws InterruptedException {
		Assert.assertTrue(
				"Not received media in the recording (timeout waiting playing event) "
						+ messageAppend,
				getBrowser(browserKey).waitForEvent("playing"));
		for (Color color : expectedColors) {
			Assert.assertTrue("The color of the recorded video should be "
					+ color + " " + messageAppend, getBrowser(browserKey)
					.similarColorAt(color, x, y));
		}

		if (eosLatch != null) {
			Assert.assertTrue("Not received EOS event in player",
					eosLatch.await(getTimeout(), TimeUnit.SECONDS));
		} else {
			Thread.sleep(playtime * 1000);
		}

		double currentTime = getBrowser(browserKey).getCurrentTime();
		Assert.assertTrue(
				"Error in play time in the recorded video (expected: "
						+ playtime + " sec, real: " + currentTime + " sec) "
						+ messageAppend,
				getBrowser(browserKey).compare(playtime, currentTime));
	}

}
