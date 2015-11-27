/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.performance.kms;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.Filter;
import org.kurento.client.FilterType;
import org.kurento.client.GStreamerFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.TestScenario;
import org.kurento.test.utils.WebRtcConnector;

/**
 * <strong>Description</strong>: Performance test for KMS.<br/>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No assertion, just data gathering.</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */
public class KmsPerformanceTest extends PerformanceTest {

	private enum MediaProcessingType {
		NONE, ENCODER, FILTER
	}

	private static final String MEDIA_PROCESSING_PROPERTY = "mediaProcessing";
	private static final String MEDIA_PROCESSING_DEFAULT = MediaProcessingType.NONE
			.name().toLowerCase();

	private static final String NUM_CLIENTS_PROPERTY = "numClients";
	private static final int NUM_CLIENTS_DEFAULT = 2;

	private static final String TIME_BEETWEEN_CLIENTS_PROPERTY = "timeBetweenClientCreation";
	private static final int TIME_BEETWEEN_CLIENTS_DEFAULT = 0;

	private static final String PERFORMANCE_TEST_TIME_PROPERTY = "performanceTestTime";
	private static final int PERFORMANCE_TEST_TIME_DEFAULT = 10; // seconds

	private static String MEDIA_PROCESSING = getProperty(
			MEDIA_PROCESSING_PROPERTY, MEDIA_PROCESSING_DEFAULT);
	private static int NUM_CLIENTS = getProperty(NUM_CLIENTS_PROPERTY,
			NUM_CLIENTS_DEFAULT);
	private static int TIME_BETEEN_CLIENT_CREATION = getProperty(
			TIME_BEETWEEN_CLIENTS_PROPERTY, TIME_BEETWEEN_CLIENTS_DEFAULT);
	private static int TEST_TIME = getProperty(PERFORMANCE_TEST_TIME_PROPERTY,
			PERFORMANCE_TEST_TIME_DEFAULT);

	private MediaPipeline mp;

	private MediaProcessingType mediaProcessingType;

	public KmsPerformanceTest() {

		setMonitorResultPath("./kms-results.csv");

		try {
			mediaProcessingType = MediaProcessingType
					.valueOf(MEDIA_PROCESSING.toUpperCase());
		} catch (IllegalArgumentException e) {
			mediaProcessingType = MediaProcessingType.NONE;
		}
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		int numBrowsers = NUM_CLIENTS < 2 ? 2 : 3;
		return TestScenario.localChromes(numBrowsers);
	}

	@Test
	public void testKmsPerformance() throws Exception {

		mp = kurentoClient.createMediaPipeline();

		try {

			WebRtcEndpoint inputEndpoint = createInputBrowserClient(getPage(0));

			createOutputBrowserClient("client1", getPage(1), inputEndpoint);

			if (NUM_CLIENTS > 1) {

				configureFakeClients(inputEndpoint);

				createOutputBrowserClient("clientN", getPage(2), inputEndpoint);

			}

			// Test time
			Thread.sleep(TimeUnit.SECONDS.toMillis(TEST_TIME));

		} finally {
			if (mp != null) {
				mp.release();
			}
		}
	}

	private void connectWithMediaProcessing(WebRtcEndpoint inputEndpoint,
			WebRtcEndpoint outputEndpoint) {

		switch (mediaProcessingType) {
		case ENCODER:
			Filter filter = new GStreamerFilter.Builder(mp,
					"capsfilter caps=video/x-raw")
							.withFilterType(FilterType.VIDEO).build();
			inputEndpoint.connect(filter);
			filter.connect(outputEndpoint);
			log.debug(
					"Pipeline: WebRtcEndpoint -> GStreamerFilter -> WebRtcEndpoint");
			break;

		case FILTER:
			filter = new FaceOverlayFilter.Builder(mp).build();
			inputEndpoint.connect(filter);
			filter.connect(outputEndpoint);
			log.debug(
					"Pipeline: WebRtcEndpoint -> FaceOverlayFilter -> WebRtcEndpoint");
			break;

		case NONE:
		default:
			inputEndpoint.connect(outputEndpoint);
			log.debug("Pipeline: WebRtcEndpoint -> WebRtcEndpoint");
			break;
		}
	}

	private void configureFakeClients(WebRtcEndpoint inputWebRtc) {

		int numFakeClients = NUM_CLIENTS - 2;

		if (numFakeClients > 0) {
			log.debug("Adding {} fake clients", numFakeClients);
			addFakeClients(numFakeClients, mp, inputWebRtc,
					TIME_BETEEN_CLIENT_CREATION, monitor,
					new WebRtcConnector() {
						public void connect(WebRtcEndpoint inputEndpoint,
								WebRtcEndpoint outputEndpoint) {
							connectWithMediaProcessing(inputEndpoint,
									outputEndpoint);
						}
					});
		}
	}

	private WebRtcEndpoint createInputBrowserClient(WebRtcTestPage page)
			throws InterruptedException {

		WebRtcEndpoint inputEndpoint = new WebRtcEndpoint.Builder(mp).build();

		page.initWebRtc(inputEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_ONLY);

		monitor.addWebRtcClientAndActivateOutboundStats("sender", inputEndpoint,
				page, "webRtcPeer.peerConnection");

		return inputEndpoint;
	}

	private WebRtcEndpoint createOutputBrowserClient(String id,
			WebRtcTestPage page, WebRtcEndpoint inputWebRtc)
					throws InterruptedException {

		WebRtcEndpoint outputEndpoint = new WebRtcEndpoint.Builder(mp).build();

		connectWithMediaProcessing(inputWebRtc, outputEndpoint);

		page.initWebRtc(outputEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);

		monitor.incrementNumClients();

		monitor.addWebRtcClientAndActivateInboundStats(id, outputEndpoint, page,
				"webRtcPeer.peerConnection");

		return outputEndpoint;
	}
}
