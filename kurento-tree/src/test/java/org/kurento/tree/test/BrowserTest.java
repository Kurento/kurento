package org.kurento.tree.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.SdpOfferProcessor;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.TreeException;
import org.kurento.tree.server.treemanager.TreeManager;

public class BrowserTest {

	@Test
	public void test() throws Exception {

		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		System.setProperty("server.port", "8890");

		System.setProperty(KurentoTreeServerApp.KMSS_URIS_PROPERTY,
				"[\"ws://localhost:8888/kurento\","
						+ "\"ws://localhost:8888/kurento\"]");

		KurentoTreeServerApp.start();

		TreeManager treeManager = KurentoTreeServerApp.getTreeManager();
		KmsManager kmsManager = treeManager.getKmsManager();

		final KurentoTreeClient client = new KurentoTreeClient(
				"ws://localhost:8890/kurento-tree");

		final String treeId = client.createTree();

		BrowserClient masterBrowser = new BrowserClient.Builder().client(
				Client.WEBRTC).build();

		masterBrowser.initWebRtcSdpProcessor(new SdpOfferProcessor() {
			@Override
			public String processSdpOffer(String sdpOffer) {
				try {
					return client.setTreeSource(treeId, sdpOffer);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		List<BrowserClient> browsers = new ArrayList<>();
		browsers.add(masterBrowser);
		for (int i = 0; i < 3; i++) {
			browsers.add(createViewer(client, treeId));
		}

		System.out.println("Browsers created");

		// KmsTopologyGrapher.showTopologyGraphic(kmsManager);

		Thread.sleep(5000);

		for (BrowserClient browser : browsers) {
			browser.close();
		}

		KurentoServicesTestHelper.teardownServices();
	}

	private BrowserClient createViewer(final KurentoTreeClient client,
			final String treeId) {

		BrowserClient browserViewer = new BrowserClient.Builder().client(
				Client.WEBRTC).build();

		browserViewer.subscribeEvents("playing");
		browserViewer.initWebRtcSdpProcessor(new SdpOfferProcessor() {
			@Override
			public String processSdpOffer(String sdpOffer) {
				try {
					return client.addTreeSink(treeId, sdpOffer).getSdp();
				} catch (TreeException | IOException e) {
					throw new RuntimeException(e);
				}
			}
		}, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		return browserViewer;
	}

}
