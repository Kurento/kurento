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

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
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
 * Test of a PlayerEndpoint with different types of media sources (WEBM, OGV,
 * MOV, MP4, MKV, AVI, 3GP ... all with video and audio) connected to a
 * WebRtcEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) PlayerEndpoint reads media source (from HTTP and FILE) and connects
 * to a WebRtcEndpoint <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · The color of the received video should be as expected <br>
 * · EOS event should arrive to player <br>
 * · Play time in remote video should be as expected <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class PlayerWebRtcTest extends FunctionalTest {

	public PlayerWebRtcTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testPlayerWebRtcHttpWebm() throws Exception {
		doTestWithSmallFile("http", "webm");
	}

	@Test
	public void testPlayerWebRtcHttpMp4() throws Exception {
		doTestWithSmallFile("http", "mp4");
	}

	@Test
	public void testPlayerWebRtcHttpMov() throws Exception {
		doTestWithSmallFile("http", "mov");
	}

	@Test
	public void testPlayerWebRtcHttpAvi() throws Exception {
		doTestWithSmallFile("http", "avi");
	}

	@Test
	public void testPlayerWebRtcHttpMkv() throws Exception {
		doTestWithSmallFile("http", "mkv");
	}

	@Test
	public void testPlayerWebRtcHttpOgv() throws Exception {
		doTestWithSmallFile("http", "ogv");
	}

	@Test
	public void testPlayerWebRtcHttp3gp() throws Exception {
		doTestWithSmallFile("http", "3gp");
	}

	@Test
	public void testPlayerWebRtcFileWebm() throws Exception {
		doTestWithSmallFile("file", "webm");
	}

	@Test
	public void testPlayerWebRtcFileMp4() throws Exception {
		doTestWithSmallFile("file", "mp4");
	}

	@Test
	public void testPlayerWebRtcFileMov() throws Exception {
		doTestWithSmallFile("file", "mov");
	}

	@Test
	public void testPlayerWebRtcFileAvi() throws Exception {
		doTestWithSmallFile("file", "avi");
	}

	@Test
	public void testPlayerWebRtcFileMkv() throws Exception {
		doTestWithSmallFile("file", "mkv");
	}

	@Test
	public void testPlayerWebRtcFileOgv() throws Exception {
		doTestWithSmallFile("file", "ogv");
	}

	@Test
	public void testPlayerWebRtcFile3gp() throws Exception {
		doTestWithSmallFile("file", "3gp");
	}

	@Test
	public void testPlayerWebRtcRtsp() throws Exception {
		doTest("rtsp://r6---sn-cg07luez.c.youtube.com/CiILENy73wIaGQm2gbECn1Hi5RMYDSANFEgGUgZ2aWRlb3MM/0/0/0/video.3gp",
				0, 50, 50, Color.WHITE);
	}

	public void doTestWithSmallFile(String protocol, String extension)
			throws InterruptedException {
		// Reduce threshold time per test
		getPage().setThresholdTime(5); // seconds

		String mediaUrl = protocol.equalsIgnoreCase("http")
				? "http://files.kurento.org" : "file://" + getPathTestFiles();
		mediaUrl += "/video/format/small." + extension;
		log.debug("Playing small file located on {}", mediaUrl);
		doTest(mediaUrl, 5, 50, 50, new Color(99, 65, 40));
	}

	public void doTest(String mediaUrl, int playtime, int x, int y,
			Color expectedColor) throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, mediaUrl)
				.build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
		playerEP.connect(webRtcEP);

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

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));
		Assert.assertTrue("The color of the video should be " + expectedColor,
				getPage().similarColorAt(expectedColor, x, y));
		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
		double currentTime = getPage().getCurrentTime();
		if (playtime > 0) {
			Assert.assertTrue(
					"Error in play time (expected: " + playtime + " sec, real: "
							+ currentTime + " sec)",
					getPage().compare(playtime, currentTime));
		}

		// Release Media Pipeline
		mp.release();
	}

}
