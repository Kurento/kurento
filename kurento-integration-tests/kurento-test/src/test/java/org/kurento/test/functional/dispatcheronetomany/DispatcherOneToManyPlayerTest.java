package org.kurento.test.functional.dispatcheronetomany;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.DispatcherOneToMany;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;

/**
 * 
 * <strong>Description</strong>: A PlayerEndpoint is connected to a
 * WebRtcEndpoint through a Dispatcher.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time should be the expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.0.0
 */

public class DispatcherOneToManyPlayerTest extends FunctionalTest {
	private static final int PLAYTIME = 10; // seconds
	private static final int TIMEOUT_EOS = 60; // seconds
	private static final String BROWSER1 = "browser1";
	private static final String BROWSER2 = "browser2";
	private static final String BROWSER3 = "browser3";

	public DispatcherOneToManyPlayerTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		TestScenario test = new TestScenario();

		test.addBrowser(BROWSER1,
				new BrowserClient.Builder().browserType(BrowserType.CHROME)
						.client(Client.WEBRTC).scope(BrowserScope.LOCAL)
						.build());
		test.addBrowser(BROWSER2,
				new BrowserClient.Builder().browserType(BrowserType.CHROME)
						.client(Client.WEBRTC).scope(BrowserScope.LOCAL)
						.build());
		test.addBrowser(BROWSER3,
				new BrowserClient.Builder().browserType(BrowserType.CHROME)
						.client(Client.WEBRTC).scope(BrowserScope.LOCAL)
						.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testDispatcherOneToManyPlayer() throws Exception {
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/red.webm").build();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/blue.webm").build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP3 = new WebRtcEndpoint.Builder(mp).build();

		DispatcherOneToMany dispatcherOneToMany = new DispatcherOneToMany.Builder(
				mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort3 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort4 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort5 = new HubPort.Builder(dispatcherOneToMany).build();

		playerEP.connect(hubPort1);
		playerEP2.connect(hubPort2);
		hubPort3.connect(webRtcEP1);
		hubPort4.connect(webRtcEP2);
		hubPort5.connect(webRtcEP3);
		dispatcherOneToMany.setSource(hubPort1);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution
		getBrowser(BROWSER1).subscribeEvents("playing");
		getBrowser(BROWSER1).initWebRtc(webRtcEP2,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		getBrowser(BROWSER2).subscribeEvents("playing");
		getBrowser(BROWSER2).initWebRtc(webRtcEP1,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		getBrowser(BROWSER3).subscribeEvents("playing");
		getBrowser(BROWSER3).initWebRtc(webRtcEP3,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		playerEP.play();

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getBrowser(BROWSER1).waitForEvent("playing"));
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getBrowser(BROWSER2).waitForEvent("playing"));
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getBrowser(BROWSER3).waitForEvent("playing"));

		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER1).similarColor(Color.RED));
		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER2).similarColor(Color.RED));
		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER3).similarColor(Color.RED));

		Thread.sleep(3000);
		playerEP2.play();
		dispatcherOneToMany.setSource(hubPort2);

		Assert.assertTrue("The color of the video should be blue",
				getBrowser(BROWSER1).similarColor(Color.BLUE));
		Assert.assertTrue("The color of the video should be blue",
				getBrowser(BROWSER2).similarColor(Color.BLUE));
		Assert.assertTrue("The color of the video should be blue",
				getBrowser(BROWSER3).similarColor(Color.BLUE));

		Thread.sleep(3000);
		dispatcherOneToMany.setSource(hubPort1);

		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER1).similarColor(Color.RED));
		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER2).similarColor(Color.RED));
		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER3).similarColor(Color.RED));

		Thread.sleep(3000);

		dispatcherOneToMany.setSource(hubPort2);

		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER1).similarColor(Color.BLUE));
		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER2).similarColor(Color.BLUE));
		Assert.assertTrue("The color of the video should be red",
				getBrowser(BROWSER3).similarColor(Color.BLUE));

		Thread.sleep(3000);

		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));
	}
}
