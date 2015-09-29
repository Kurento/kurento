package org.kurento.test.functional.alphablending;

import java.awt.Color;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.AlphaBlending;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * 
 * <strong>Description</strong>: Three synthetic videos are played by four
 * PlayerEndpoint and mixed by a AlphaBlending. The resulting video is played in
 * an WebRtcEndpoint.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>3xPlayerEndpoint -> AlphaBlending -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected (red, green, blue)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.0.0
 */

public class AlphaBlendingPlayerTest extends FunctionalTest {
	private static final int PLAYTIME = 5; // seconds

	public AlphaBlendingPlayerTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChrome();
	}

	@Test
	public void testAlphaBlendingPlayer() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/red.webm").build();
		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/green.webm").build();
		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/blue.webm").build();

		AlphaBlending alphaBlending = new AlphaBlending.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort2 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort3 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort4 = new HubPort.Builder(alphaBlending).build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		playerRed.connect(hubPort1);
		playerGreen.connect(hubPort2);
		playerBlue.connect(hubPort3);

		hubPort4.connect(webRtcEP);

		alphaBlending.setMaster(hubPort1, 1);

		alphaBlending.setPortProperties(0F, 0F, 8, 0.2F, 0.2F, hubPort2);
		alphaBlending.setPortProperties(0.4F, 0.4F, 7, 0.2F, 0.2F, hubPort3);

		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.RCV_ONLY);

		playerRed.play();
		playerGreen.play();
		playerBlue.play();

		Thread.sleep(2000);
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));

		Thread.sleep(2000);

		// Assertions
		Assert.assertTrue("Upper left part of the video must be blue",
				getPage().similarColorAt(Color.GREEN, 0, 0));
		Assert.assertTrue("Lower right part of the video must be red",
				getPage().similarColorAt(Color.RED, 315, 235));
		Assert.assertTrue("Center of the video must be blue", getPage()
				.similarColorAt(Color.BLUE, 160, 120));

		// alphaBlending.setMaster(hubPort3, 1);
		alphaBlending.setPortProperties(0.8F, 0.8F, 7, 0.2F, 0.2F, hubPort3);

		Assert.assertTrue("Lower right part of the video must be blue",
				getPage().similarColorAt(Color.BLUE, 315, 235));
		Assert.assertTrue("Center of the video must be red", getPage()
				.similarColorAt(Color.RED, 160, 120));

		Thread.sleep(PLAYTIME * 1000);
	}
}
