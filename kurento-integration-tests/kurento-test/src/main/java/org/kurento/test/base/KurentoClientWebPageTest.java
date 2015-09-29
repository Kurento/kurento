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
import org.junit.Before;
import org.junit.Rule;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebPage;
import org.kurento.test.config.TestScenario;
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
public class KurentoClientWebPageTest<W extends WebPage> extends WebPageTest<W> {

	protected ConfigurableApplicationContext context;
	protected KurentoClientManager kurentoClientManager;
	protected KurentoClient kurentoClient;
	protected KurentoClient fakeKurentoClient;

	public KurentoClientWebPageTest() {
	}

	public KurentoClientWebPageTest(TestScenario testScenario) {
		super(testScenario);

		Class<?> clazz = this.getClass();
		while(true){
			try {
				clazz.getConstructor();
				break;
			} catch(NoSuchMethodException e){
				clazz = clazz.getSuperclass();
			}
		}
		
		context = KurentoServicesTestHelper.startHttpServer(clazz);
	}

	@Rule
	public KmsLogOnFailure logOnFailure = new KmsLogOnFailure();

	@Before
	public void setupKurentoClient() throws IOException {
		kurentoClientManager = new KurentoClientManager(testName, this.getClass());
		kurentoClient = kurentoClientManager.getKurentoClient();
		fakeKurentoClient = kurentoClientManager.getFakeKurentoClient();
	}

	@After
	public void teardownKurentoClient() throws Exception {
		kurentoClientManager.teardown();
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
		File fileForRecording = new File(
				KurentoServicesTestHelper.getTestDir() + "/" + KurentoServicesTestHelper.getTestCaseName());
		String testName = KurentoServicesTestHelper.getSimpleTestName();
		return fileForRecording.getAbsolutePath() + "/" + testName + preffix;
	}

	public void addFakeClients(int numMockClients, int bandwidht, MediaPipeline mainPipeline,
			WebRtcEndpoint webRtcEndpoint) {

		if (fakeKurentoClient == null) {
			log.warn("Fake kurentoClient is not defined. You must set the value of {} property", FAKE_KMS_WS_URI_PROP);

		} else {

			log.info("* * * Adding {} mock clients * * *", numMockClients);

			MediaPipeline fakePipeline = fakeKurentoClient.createMediaPipeline();

			for (int i = 0; i < numMockClients; i++) {
				final WebRtcEndpoint fakeSender = new WebRtcEndpoint.Builder(mainPipeline).build();
				final WebRtcEndpoint fakeReceiver = new WebRtcEndpoint.Builder(fakePipeline).build();

				fakeSender.setMaxVideoSendBandwidth(bandwidht);
				fakeSender.setMinVideoSendBandwidth(bandwidht);
				fakeReceiver.setMaxVideoRecvBandwidth(bandwidht);

				fakeSender.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
					@Override
					public void onEvent(OnIceCandidateEvent event) {
						fakeReceiver.addIceCandidate(event.getCandidate());
					}
				});

				fakeReceiver.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
					@Override
					public void onEvent(OnIceCandidateEvent event) {
						fakeSender.addIceCandidate(event.getCandidate());
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
