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

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.FAKE_KMS_WS_URI_PROP;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoClientTestFactory;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base for tests using kurento-client.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class KurentoClientTest extends KurentoTest {

	protected static KurentoClient kurentoClient;
	protected static KurentoClient fakeKurentoClient;
	protected static boolean startHttpServer;
	protected ConfigurableApplicationContext context;

	public KurentoClientTest() {
		super();
	}

	public KurentoClientTest(TestScenario testScenario) {
		super(testScenario);
		// HTTP server
		startHttpServer = !this.getClass()
				.isAnnotationPresent(WebAppConfiguration.class);
		if (startHttpServer) {
			context = KurentoServicesTestHelper
					.startHttpServer(BrowserKurentoClientTest.class);
		}
	}

	@Rule
	public KmsLogOnFailure logOnFailure = new KmsLogOnFailure();

	@Before
	public void setupKurentoClient() throws IOException {
		// Kurento services
		KurentoServicesTestHelper.setTestName(testName.getMethodName());
		KurentoServicesTestHelper.setTestCaseName(this.getClass().getName());
		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		log.info("Starting test {}",
				this.getClass().getName() + "." + testName.getMethodName());

		// Kurento client
		kurentoClient = KurentoClientTestFactory.createKurentoForTest();

		// Fake Kurento client
		String fakeWsUri = getProperty(FAKE_KMS_WS_URI_PROP);
		if (fakeWsUri != null) {
			fakeKurentoClient = KurentoClient.create(fakeWsUri);
		}
	}

	@After
	public void teardownKurentoClient() throws Exception {
		// Kurento client
		if (kurentoClient != null) {
			kurentoClient.destroy();
		}

		// Kurento services
		KurentoServicesTestHelper.teardownServices();
	}

	protected int getServerPort() {
		return KurentoServicesTestHelper.getAppHttpPort();
	}

	public static String getPathTestFiles() {
		return KurentoServicesTestHelper.getTestFilesPath();
	}

	public String getDefaultFileForRecording() {
		return getDefaultOutputFile(".webm");
	}

	public static String getDefaultOutputFile(String preffix) {
		File fileForRecording = new File(KurentoServicesTestHelper.getTestDir()
				+ "/" + KurentoServicesTestHelper.getTestCaseName());
		String testName = KurentoServicesTestHelper.getSimpleTestName();
		return fileForRecording.getAbsolutePath() + "/" + testName + preffix;
	}

	public void addFakeClients(int numMockClients, int bandwidht,
			MediaPipeline mainPipeline, WebRtcEndpoint webRtcEndpoint) {

		if (fakeKurentoClient == null) {
			log.warn(
					"Fake kurentoClient is not defined. You must set the value of {} property",
					FAKE_KMS_WS_URI_PROP);

		} else {

			log.info("* * * Adding {} mock clients * * *", numMockClients);

			MediaPipeline fakePipeline = fakeKurentoClient
					.createMediaPipeline();

			for (int i = 0; i < numMockClients; i++) {
				final WebRtcEndpoint fakeSender = new WebRtcEndpoint.Builder(
						mainPipeline).build();
				final WebRtcEndpoint fakeReceiver = new WebRtcEndpoint.Builder(
						fakePipeline).build();

				fakeSender.setMaxVideoSendBandwidth(bandwidht);
				fakeSender.setMinVideoSendBandwidth(bandwidht);
				fakeReceiver.setMaxVideoRecvBandwidth(bandwidht);

				fakeSender.addOnIceCandidateListener(
						new EventListener<OnIceCandidateEvent>() {
							@Override
							public void onEvent(OnIceCandidateEvent event) {
								fakeReceiver
										.addIceCandidate(event.getCandidate());
							}
						});

				fakeReceiver.addOnIceCandidateListener(
						new EventListener<OnIceCandidateEvent>() {
							@Override
							public void onEvent(OnIceCandidateEvent event) {
								fakeSender
										.addIceCandidate(event.getCandidate());
							}
						});

				String sdpOffer = fakeReceiver.generateOffer();
				String sdpAnswer = fakeSender.processOffer(sdpOffer);
				fakeReceiver.processAnswer(sdpAnswer);

				fakeSender.gatherCandidates();
				fakeReceiver.gatherCandidates();

				webRtcEndpoint.connect(fakeSender);
			}
		}
	}

}
