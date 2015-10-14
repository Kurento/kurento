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
import org.kurento.test.config.TestScenario;

/**
 * <strong>Description</strong>: Performance test for KMS.<br/>
 * <strong>Possible pipelines</strong>:
 * <ul>
 * <li>one2one</li>
 * <li>one2many</li>
 * <li>many2many</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No assertion, just data gathering.</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
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
	private static final int NUM_CLIENTS_DEFAULT = 1;
	private static final String TIME_BEETWEEN_CLIENTS_PROPERTY = "timeBetweenClientCreation";
	private static final int TIME_BEETWEEN_CLIENTS_DEFAULT = 0;

	private static final String PERFORMANCE_TEST_TIME_PROPERTY = "performanceTestTime";
	private static final int PERFORMANCE_TEST_TIME_DEFAULT = 10; // seconds

	private static String mediaProcessing = getProperty(
			MEDIA_PROCESSING_PROPERTY, MEDIA_PROCESSING_DEFAULT);
	private static int numClients = getProperty(NUM_CLIENTS_PROPERTY,
			NUM_CLIENTS_DEFAULT);
	private static int timeBetweenClientCreation = getProperty(
			TIME_BEETWEEN_CLIENTS_PROPERTY, TIME_BEETWEEN_CLIENTS_DEFAULT);
	private static int testTime = getProperty(PERFORMANCE_TEST_TIME_PROPERTY,
			PERFORMANCE_TEST_TIME_DEFAULT);

	public KmsPerformanceTest(TestScenario testScenario) {
		super(testScenario);

		setMonitorResultPath("./kms-results.csv");
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChrome();
	}

	@Test
	public void testKmsPerformance() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();

		// Media processing
		MediaProcessingType mediaProcessingType;
		Filter filter = null;
		try {
			mediaProcessingType = MediaProcessingType
					.valueOf(mediaProcessing.toUpperCase());
		} catch (IllegalArgumentException e) {
			mediaProcessingType = MediaProcessingType.NONE;
		}
		switch (mediaProcessingType) {
		case ENCODER:
			filter = new GStreamerFilter.Builder(mp,
					"capsfilter caps=video/x-raw")
							.withFilterType(FilterType.VIDEO).build();
			webRtcEndpoint.connect(filter);
			filter.connect(webRtcEndpoint);

			log.debug(
					"Pipeline: WebRtcEndpoint -> GStreamerFilter -> WebRtcEndpoint");
			break;

		case FILTER:
			filter = new FaceOverlayFilter.Builder(mp).build();
			webRtcEndpoint.connect(filter);
			filter.connect(webRtcEndpoint);

			log.debug(
					"Pipeline: WebRtcEndpoint -> FaceOverlayFilter -> WebRtcEndpoint");
			break;

		case NONE:
		default:
			webRtcEndpoint.connect(webRtcEndpoint);

			log.debug("Pipeline: WebRtcEndpoint -> WebRtcEndpoint");
			break;
		}

		// Browser
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_RCV);
		monitor.incrementNumClients();
		getPage().activateClientRtcStats(monitor, "webRtcPeer.peerConnection");
		monitor.setWebRtcEndpoint(webRtcEndpoint);

		// Fake clients
		if (numClients > 0) {
			log.debug("Adding {} fake clients", numClients);
			addFakeClients(numClients, mp, webRtcEndpoint,
					timeBetweenClientCreation, monitor, filter.getClass());
		}

		// Test time
		Thread.sleep(TimeUnit.SECONDS.toMillis(testTime));

		// Release Media Pipeline
		if (mp != null) {
			mp.release();
		}
	}

}
