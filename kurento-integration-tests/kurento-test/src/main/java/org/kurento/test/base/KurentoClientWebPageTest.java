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

import static org.kurento.test.TestConfiguration.FAKE_KMS_WS_URI_PROP;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.browser.WebPage;
import org.kurento.test.config.TestScenario;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base for tests using kurento-client.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
@EnableAutoConfiguration
public class KurentoClientWebPageTest<W extends WebPage>
		extends WebPageTest<W> {

	protected static ConfigurableApplicationContext context;

	protected KurentoClientManager kurentoClientManager;
	protected KurentoClient kurentoClient;
	protected KurentoClient fakeKurentoClient;

	public KurentoClientWebPageTest() {
	}

	public KurentoClientWebPageTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Rule
	public KmsLogOnFailure logOnFailure = new KmsLogOnFailure();

	@Override
	public void setupKurentoTest() throws InterruptedException {

		startHttpServer();

		try {

			kurentoClientManager = new KurentoClientManager(testName,
					this.getClass());
			kurentoClient = kurentoClientManager.getKurentoClient();
			fakeKurentoClient = kurentoClientManager.getFakeKurentoClient();
			logOnFailure.setKurentoClientManager(kurentoClientManager);

			log.info(
					"--------------- Started KurentoClientWebPageTest ----------------");

			super.setupKurentoTest();

		} catch (IOException e) {
			throw new KurentoException(
					"Exception creating kurentoClientManager", e);
		}
	}

	private void startHttpServer() {
		Class<?> clazz = this.getClass();
		while (true) {
			try {
				clazz.getConstructor();
				break;
			} catch (NoSuchMethodException e) {
				clazz = clazz.getSuperclass();
			}
		}
		context = KurentoServicesTestHelper.startHttpServer(clazz);
	}

	@After
	public void teardownKurentoClient() throws Exception {
		log.info(
				"--------------- Finished KurentoClientWebPageTest ----------------");
		if (kurentoClientManager != null) {
			kurentoClientManager.teardown();
		}
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

	public static String getDefaultOutputFile(String suffix) {
		File fileForRecording = new File(KurentoServicesTestHelper.getTestDir()
				+ "/" + KurentoServicesTestHelper.getTestCaseName());
		String testName = KurentoServicesTestHelper.getSimpleTestName();
		return fileForRecording.getAbsolutePath() + "/" + testName + suffix;
	}

	public void addFakeClients(int numFakeClients, int bandwidht,
			MediaPipeline mainPipeline, WebRtcEndpoint senderWebRtcEndpoint) {
		addFakeClients(numFakeClients, bandwidht, mainPipeline,
				senderWebRtcEndpoint, 0, null, null);
	}

	public void addFakeClients(int numFakeClients, MediaPipeline mainPipeline,
			WebRtcEndpoint senderWebRtcEndpoint, long timeBetweenClientMs,
			SystemMonitorManager monitor, WebRtcConnector connector) {
		addFakeClients(numFakeClients, -1, mainPipeline, senderWebRtcEndpoint,
				timeBetweenClientMs, monitor, connector);
	}

	public void addFakeClients(int numFakeClients, int bandwidht,
			MediaPipeline mainPipeline, WebRtcEndpoint inputWebRtc,
			long timeBetweenClientMs, SystemMonitorManager monitor,
			WebRtcConnector connector) {

		if (fakeKurentoClient == null) {
			log.warn(
					"Fake kurentoClient is not defined. You must set the value of {} property",
					FAKE_KMS_WS_URI_PROP);

		} else {

			log.info("* * * Adding {} fake clients * * *", numFakeClients);

			MediaPipeline fakePipeline = fakeKurentoClient
					.createMediaPipeline();

			for (int i = 0; i < numFakeClients; i++) {
				final WebRtcEndpoint fakeOutputWebRtc = new WebRtcEndpoint.Builder(
						mainPipeline).build();
				final WebRtcEndpoint fakeBrowser = new WebRtcEndpoint.Builder(
						fakePipeline).build();

				if (bandwidht != -1) {
					fakeOutputWebRtc.setMaxVideoSendBandwidth(bandwidht);
					fakeOutputWebRtc.setMinVideoSendBandwidth(bandwidht);
					fakeBrowser.setMaxVideoRecvBandwidth(bandwidht);
				}

				fakeOutputWebRtc.addOnIceCandidateListener(
						new EventListener<OnIceCandidateEvent>() {
							@Override
							public void onEvent(OnIceCandidateEvent event) {
								fakeBrowser
										.addIceCandidate(event.getCandidate());
							}
						});

				fakeBrowser.addOnIceCandidateListener(
						new EventListener<OnIceCandidateEvent>() {
							@Override
							public void onEvent(OnIceCandidateEvent event) {
								fakeOutputWebRtc
										.addIceCandidate(event.getCandidate());
							}
						});

				String sdpOffer = fakeBrowser.generateOffer();
				String sdpAnswer = fakeOutputWebRtc.processOffer(sdpOffer);
				fakeBrowser.processAnswer(sdpAnswer);

				fakeOutputWebRtc.gatherCandidates();
				fakeBrowser.gatherCandidates();

				if (connector == null) {
					inputWebRtc.connect(fakeOutputWebRtc);
				} else {
					connector.connect(inputWebRtc, fakeOutputWebRtc);
				}

				if (monitor != null) {
					monitor.incrementNumClients();
				}

				if (timeBetweenClientMs > 0) {
					try {
						Thread.sleep(timeBetweenClientMs);
					} catch (InterruptedException e) {
						log.warn("Interrupted exception adding fake clients",
								e);
					}
				}
			}
		}
	}

}
