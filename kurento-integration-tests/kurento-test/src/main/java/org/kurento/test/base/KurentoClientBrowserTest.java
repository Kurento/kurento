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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.junit.After;
import org.junit.Before;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebPage;
import org.kurento.test.lifecycle.FailedTest;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.services.FakeKmsService;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;
import org.kurento.test.services.WebServerService;
import org.kurento.test.utils.WebRtcConnector;

/**
 * Base for tests using Kurento client tests with browsers.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class KurentoClientBrowserTest<W extends WebPage>
		extends BrowserTest<W> {

	public static @Service WebServerService webServer = new WebServerService();
	public static @Service KmsService kms = new KmsService();
	public static @Service KmsService fakeKms = new FakeKmsService();

	protected static KurentoClient kurentoClient;
	protected static KurentoClient fakeKurentoClient;

	@Before
	public void setupKurentoClient() {
		kurentoClient = kms.getKurentoClient();
		fakeKurentoClient = fakeKms.getKurentoClient();
	}

	@After
	public void teardownKurentoClient() throws Exception {
		if (kurentoClient != null) {
			kurentoClient.destroy();
		}
		if (fakeKurentoClient != null) {
			fakeKurentoClient.destroy();
		}
	}

	@FailedTest
	public static void retrieveGstreamerDots() {
		if (kurentoClient != null) {
			try {
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

	protected String getDefaultFileForRecording() {
		return getDefaultOutputFile(".webm");
	}

	public void addFakeClients(int numFakeClients, int bandwidht,
			MediaPipeline mainPipeline, WebRtcEndpoint senderWebRtcEndpoint) {
		fakeKms.addFakeClients(numFakeClients, bandwidht, mainPipeline,
				senderWebRtcEndpoint, 0, null, null);
	}

	public void addFakeClients(int numFakeClients, MediaPipeline mainPipeline,
			WebRtcEndpoint senderWebRtcEndpoint, long timeBetweenClientMs,
			SystemMonitorManager monitor, WebRtcConnector connector) {
		fakeKms.addFakeClients(numFakeClients, -1, mainPipeline,
				senderWebRtcEndpoint, timeBetweenClientMs, monitor, connector);
	}

}
