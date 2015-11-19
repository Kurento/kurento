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

import static org.kurento.test.config.TestConfiguration.FAKE_KMS_WS_URI_PROP;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.junit.After;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.base.RepositoryFunctionalTest.RepositoryWebServer;
import org.kurento.test.browser.WebPage;
import org.kurento.test.config.TestScenario;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.services.FailedTest;
import org.kurento.test.services.KurentoClientManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.test.services.WebRtcConnector;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base for tests using kurento-client.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class KurentoClientBrowserTest<W extends WebPage>
		extends BrowserTest<W> {

	@EnableAutoConfiguration
	public static class WebServer {
	}

	protected static ConfigurableApplicationContext context;
	protected static KurentoClientManager kurentoClientManager;

	protected KurentoClient kurentoClient;
	protected KurentoClient fakeKurentoClient;

	public KurentoClientBrowserTest(Class<?> webServerClass) {
		this.webServerClass = webServerClass;
	}

	public KurentoClientBrowserTest(TestScenario testScenario) {
		super(testScenario);
	}

	private Class<?> webServerClass = WebServer.class;

	protected void setWebServerClass(
			Class<RepositoryWebServer> webServerClass) {
		this.webServerClass = webServerClass;
	}

	@FailedTest
	public static void retrieveGstreamerDots() {
		if (kurentoClientManager != null) {
			try {
				KurentoClient kurentoClient = kurentoClientManager
						.getKurentoClient();
				List<MediaPipeline> pipelines = kurentoClient.getServerManager()
						.getPipelines();
				log.debug(
						"Retrieving GStreamerDots for all pipelines in KMS ({})",
						pipelines.size());

				for (MediaPipeline pipeline : pipelines) {

					String pipelineName = pipeline.getName();
					log.debug("Saving GstreamerDot for pipeline {}",
							pipelineName);

					String gstreamerDotFile = KurentoClientBrowserTest
							.getDefaultOutputFile("-" + pipelineName);

					try {
						FileUtils.writeStringToFile(new File(gstreamerDotFile),
								pipeline.getGstreamerDot());

					} catch (IOException ioe) {
						log.error("Exception writing GstreamerDot file", ioe);
					}
				}
			} catch (WebSocketException e) {
				log.warn(
						"WebSocket exception while reading existing pipelines. Maybe KMS is closed: {}:{}",
						e.getClass().getName(), e.getMessage());
			}
		}
	}

	@Override
	public void setupKurentoTest() throws InterruptedException {

		startHttpServer();

		try {

			kurentoClientManager = new KurentoClientManager();
			kurentoClient = kurentoClientManager.getKurentoClient();
			fakeKurentoClient = kurentoClientManager.getFakeKurentoClient();

			log.info(
					"--------------- Started KurentoClientWebPageTest ---------------");

			super.setupKurentoTest();

		} catch (IOException e) {
			throw new KurentoException(
					"Exception creating kurentoClientManager", e);
		}
	}

	private void startHttpServer() {
		context = KurentoServicesTestHelper.startHttpServer(webServerClass);
	}

	@After
	public void teardownKurentoClient() throws Exception {
		log.info(
				"--------------- Finished KurentoClientWebPageTest ---------------");
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

			throw new KurentoException(
					"Fake kurentoClient is not defined. You must set the '"
							+ FAKE_KMS_WS_URI_PROP
							+ "' property with KMS uri to fake clients");

		} else {

			log.info("* * * Adding {} fake clients * * *", numFakeClients);

			MediaPipeline fakePipeline = fakeKurentoClient
					.createMediaPipeline();

			for (int i = 0; i < numFakeClients; i++) {

				log.info("* * * Adding fake client {} * * *", i);

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
