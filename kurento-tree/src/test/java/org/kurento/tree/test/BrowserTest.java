package org.kurento.tree.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.SdpOfferProcessor;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.kurento.tree.server.treemanager.TreeException;

public class BrowserTest {

	private static final int NUM_VIEWERS = 4;

	private class TreeViewer {

		private BrowserClient browser;
		private String sinkId;

		public TreeViewer(BrowserClient browser, String sinkId) {
			super();
			this.browser = browser;
			this.sinkId = sinkId;
		}

		public BrowserClient getBrowser() {
			return browser;
		}

		public void setBrowser(BrowserClient browser) {
			this.browser = browser;
		}

		public String getSinkId() {
			return sinkId;
		}

		public void setSinkId(String sinkId) {
			this.sinkId = sinkId;
		}
	}

	@Test
	public void test() throws Exception {

		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		startKurentoTreeServer();

		final KurentoTreeClient kurentoTree = new KurentoTreeClient(
				"ws://localhost:8890/kurento-tree");

		final String treeId = kurentoTree.createTree();

		ExecutorService exec = Executors.newFixedThreadPool(NUM_VIEWERS + 1);

		// Future<BrowserClient> masterBrowser = exec
		// .submit(new Callable<BrowserClient>() {
		// @Override
		// public BrowserClient call() throws Exception {
		// return createMaster(kurentoTree, treeId);
		// }
		// });

		BrowserClient masterBrowser = createMaster(kurentoTree, treeId);

		List<Future<TreeViewer>> viewers = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			viewers.add(exec.submit(new Callable<TreeViewer>() {
				@Override
				public TreeViewer call() throws Exception {
					return createViewer(kurentoTree, treeId);
				}
			}));
		}

		for (Future<TreeViewer> viewer : viewers) {
			viewer.get();
		}

		System.out.println("Browsers created");

		Thread.sleep(20000);

		System.out.println("Start closing");

		masterBrowser.close();

		for (Future<TreeViewer> viewer : viewers) {
			TreeViewer treeViewer = viewer.get();
			treeViewer.getBrowser().close();
			String sinkId = treeViewer.getSinkId();
			System.out.println("Removing sinkId: " + sinkId);
			kurentoTree.removeTreeSink(treeId, sinkId);
		}

		kurentoTree.releaseTree(treeId);

		KurentoServicesTestHelper.teardownServices();
	}

	private BrowserClient createMaster(final KurentoTreeClient kurentoTree,
			final String treeId) {
		BrowserClient masterBrowser = new BrowserClient.Builder().client(
				Client.WEBRTC).build();

		masterBrowser.initWebRtcSdpProcessor(new SdpOfferProcessor() {
			@Override
			public String processSdpOffer(String sdpOffer) {
				try {
					return kurentoTree.setTreeSource(treeId, sdpOffer);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
		return masterBrowser;
	}

	private void startKurentoTreeServer() {
		System.setProperty("server.port", "8890");

		System.setProperty(KurentoTreeServerApp.KMSS_URIS_PROPERTY,
				"[\"ws://localhost:8888/kurento\","
						+ "\"ws://localhost:8888/kurento\"]");

		KurentoTreeServerApp.start();
	}

	private TreeViewer createViewer(final KurentoTreeClient client,
			final String treeId) {

		BrowserClient browserViewer = new BrowserClient.Builder().client(
				Client.WEBRTC).build();

		final TreeViewer treeClient = new TreeViewer(browserViewer, null);

		browserViewer.subscribeEvents("playing");
		browserViewer.initWebRtcSdpProcessor(new SdpOfferProcessor() {
			@Override
			public String processSdpOffer(String sdpOffer) {
				try {
					TreeEndpoint treeEndpoint = client.addTreeSink(treeId,
							sdpOffer);
					treeClient.setSinkId(treeEndpoint.getId());
					return treeEndpoint.getSdp();
				} catch (TreeException | IOException e) {
					throw new RuntimeException(e);
				}
			}
		}, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		return treeClient;
	}

}
