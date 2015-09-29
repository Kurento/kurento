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
package org.kurento.test.performance.webrtc;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserRunner;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.grid.ParallelBrowsers;
import org.kurento.test.latency.LatencyController;

/**
 * <strong>Description</strong>: WebRTC (one to many) test with Selenium Grid.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> N x WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No assertion, just data gathering.</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcPerformanceOneToManyTest extends PerformanceTest {

	private static final String NUM_VIEWERS_PROPERTY = "perf.one2many.numviewers";
	private static final int NUM_VIEWERS_DEFAULT = 1;

	private static final String BROWSER_PER_VIEWER_PROPERTY = "perf.one2many.browserperviewer";
	private static final int BROWSER_PER_VIEWER_DEFAULT = 2;

	private static int numViewers;
	private static int browserPerViewer;

	public WebRtcPerformanceOneToManyTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		numViewers = getProperty(NUM_VIEWERS_PROPERTY, NUM_VIEWERS_DEFAULT);
		browserPerViewer = getProperty(BROWSER_PER_VIEWER_PROPERTY,
				BROWSER_PER_VIEWER_DEFAULT);

		TestScenario test = new TestScenario();
		String video = getPathTestFiles() + "/video/15sec/rgbHD.y4m";
		test.addBrowser(
				BrowserConfig.PRESENTER,
				new Browser.Builder().webPageType(WebPageType.WEBRTC)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).video(video).build());

		test.addBrowser(
				BrowserConfig.VIEWER,
				new Browser.Builder().webPageType(WebPageType.WEBRTC)
						.numInstances(numViewers)
						.browserPerInstance(browserPerViewer)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).build());

		// Uncomment this for remote scenario
		// test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
		// .browserType(BrowserType.CHROME).scope(BrowserScope.REMOTE)
		// .video(video).build());
		//
		// ... or saucelabs, for example:
		//
		// test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
		// .browserType(BrowserType.CHROME).scope(BrowserScope.SAUCELABS)
		// .platform(Platform.WIN8_1).browserVersion("39").build());
		//
		// test.addBrowser(TestConfig.VIEWER, new BrowserClient.Builder()
		// .numInstances(numViewers).browserPerInstance(browserPerViewer)
		// .browserType(BrowserType.CHROME).scope(BrowserScope.REMOTE)
		// .build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void test() throws InterruptedException {
		// Media Pipeline
		final MediaPipeline mp = kurentoClient.createMediaPipeline();
		final WebRtcEndpoint masterWebRtcEP = new WebRtcEndpoint.Builder(mp)
				.build();

		// Master
		getPresenter().subscribeLocalEvents("playing");
		getPresenter().initWebRtc(masterWebRtcEP, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_ONLY);

		Map<String, Browser> browsers = new TreeMap<>(getTestScenario()
				.getBrowserMap());
		browsers.remove(BrowserConfig.PRESENTER);
		final int playTime = ParallelBrowsers.getRampPlaytime(browsers.size());

		ParallelBrowsers.ramp(browsers, monitor, new BrowserRunner() {
			public void run(Browser browser) throws Exception {
				String name = browser.getId();

				try {
					// Viewer
					WebRtcEndpoint viewerWebRtcEP = new WebRtcEndpoint.Builder(
							mp).build();
					masterWebRtcEP.connect(viewerWebRtcEP);

					// Latency control
					LatencyController cs = new LatencyController(
							"Latency control on " + name, monitor);

					// WebRTC
					log.debug(">>> start {}", name);
					getPage(name).subscribeEvents("playing");
					getPage(name).initWebRtc(viewerWebRtcEP,
							WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);

					// Latency assessment
					cs.checkRemoteLatency(playTime, TimeUnit.MILLISECONDS,
							getPresenter(), getPage(name));

				} catch (Throwable e) {
					log.error("[[[ {} ]]]", e.getCause().getMessage());
					throw e;
				}
			}
		});

		log.debug("<<< Releasing pipeline");

		// Release Media Pipeline
		if (mp != null) {
			mp.release();
		}
	}
}
