package org.kurento.test.functional.alphablending;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.AlphaBlending;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;

/**
 * 
 * <strong>Description</strong>: Three synthetic videos are played by three
 * WebRtcEndpoint and mixed by an AlphaBlending. The resulting video is played
 * in an WebRtcEndpoint.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>3xWebRtcEndpoint -> AlphaBlending -> WebRtcEndpoint</li>
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

public class AlphaBlendingWebRtcTest extends FunctionalTest {
	private static int PLAYTIME = 5;
	private static final String BROWSER1 = "browser1";
	private static final String BROWSER2 = "browser2";
	private static final String BROWSER3 = "browser3";
	private static final String BROWSER4 = "browser4";

	public AlphaBlendingWebRtcTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		TestScenario test = new TestScenario();

		test.addBrowser(
				BROWSER1,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).webPageType(WebPageType.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/red.y4m")
						.build());
		test.addBrowser(
				BROWSER2,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).webPageType(WebPageType.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/green.y4m")
						.build());
		test.addBrowser(
				BROWSER3,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).webPageType(WebPageType.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/blue.y4m")
						.build());
		test.addBrowser(BROWSER4,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).webPageType(WebPageType.WEBRTC)
						.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testAlphaBlendingWebRtc() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEPRed = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPGreen = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPBlue = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPAlphabaBlending = new WebRtcEndpoint.Builder(mp)
				.build();

		AlphaBlending alphaBlending = new AlphaBlending.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort2 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort3 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort4 = new HubPort.Builder(alphaBlending).build();

		webRtcEPRed.connect(hubPort1);
		webRtcEPGreen.connect(hubPort2);
		webRtcEPBlue.connect(hubPort3);
		hubPort4.connect(webRtcEPAlphabaBlending);

		alphaBlending.setMaster(hubPort1, 1);

		alphaBlending.setPortProperties(0F, 0F, 8, 0.2F, 0.2F, hubPort2);
		alphaBlending.setPortProperties(0.4F, 0.4F, 7, 0.2F, 0.2F, hubPort3);

		getPage(BROWSER1).subscribeLocalEvents("playing");
		getPage(BROWSER1).initWebRtc(webRtcEPRed,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		getPage(BROWSER2).subscribeLocalEvents("playing");
		getPage(BROWSER2).initWebRtc(webRtcEPGreen,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		getPage(BROWSER3).subscribeLocalEvents("playing");
		getPage(BROWSER3).initWebRtc(webRtcEPBlue,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		getPage(BROWSER4).subscribeEvents("playing");
		getPage(BROWSER4).initWebRtc(webRtcEPAlphabaBlending,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		// Assertions
		Assert.assertTrue("Upper left part of the video must be blue",
				getPage(BROWSER4).similarColorAt(Color.GREEN, 0, 0));
		Assert.assertTrue("Lower right part of the video must be red",
				getPage(BROWSER4).similarColorAt(Color.RED, 315, 235));
		Assert.assertTrue("Center of the video must be blue",
				getPage(BROWSER4).similarColorAt(Color.BLUE, 160, 120));

		// alphaBlending.setMaster(hubPort3, 1);
		alphaBlending.setPortProperties(0.8F, 0.8F, 7, 0.2F, 0.2F, hubPort3);

		Assert.assertTrue("Lower right part of the video must be blue",
				getPage(BROWSER4).similarColorAt(Color.BLUE, 315, 235));
		Assert.assertTrue("Center of the video must be red",
				getPage(BROWSER4).similarColorAt(Color.RED, 160, 120));
		Thread.sleep(PLAYTIME * 1000);
	}
}
