package org.kurento.tree.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.SdpOfferProcessor;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestConfig;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

@Category(SystemFunctionalTests.class)
public class TreeDistributionConnectSourceToSinksTest extends
		BrowserKurentoClientTest {

	private static final Logger log = LoggerFactory
			.getLogger(TreeDistributionConnectSourceToSinksTest.class);

	private static final int NUM_VIEWERS = 4;

	public TreeDistributionConnectSourceToSinksTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		// Test: 1+NUM_VIEWERS local Chrome's
		TestScenario test = new TestScenario();
		test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());

		for (int i = 0; i < NUM_VIEWERS; i++) {
			test.addBrowser(TestConfig.VIEWER + i, new BrowserClient.Builder()
					.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
					.build());
		}
		return Arrays.asList(new Object[][] { { test } });
	}

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

		public String getSinkId() {
			return sinkId;
		}

		public void setSinkId(String sinkId) {
			this.sinkId = sinkId;
		}
	}

	private ConfigurableApplicationContext treeServer;

	@Before
	public void startServices() throws IOException {

		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		String kmsUri = PropertiesManager.getProperty(
				KurentoServicesTestHelper.KMS_WS_URI_PROP,
				KurentoServicesTestHelper.KMS_WS_URI_DEFAULT);

		System.setProperty(KurentoTreeServerApp.KMSS_URIS_PROPERTY, "[\""
				+ kmsUri + "\",\"" + kmsUri + "\"]");

		System.setProperty(KurentoTreeServerApp.WEBSOCKET_PORT_PROPERTY,
				Integer.toString(KurentoServicesTestHelper.getAppHttpPort()));

		treeServer = KurentoTreeServerApp.start();
	}

	@After
	public void teardownServices() throws IOException {

		treeServer.close();

		KurentoServicesTestHelper.teardownServices();
	}

	@Ignore
	@Test
	public void test() throws Exception {

		int port = KurentoServicesTestHelper.getAppHttpPort();

		final KurentoTreeClient kurentoTree = new KurentoTreeClient(
				"ws://localhost:" + port + "/kurento-tree");

		final String treeId = kurentoTree.createTree();

		ExecutorService exec = Executors.newFixedThreadPool(NUM_VIEWERS + 1);

		// Create browsers
		Future<BrowserClient> masterBrowserFuture = exec
				.submit(new Callable<BrowserClient>() {
					@Override
					public BrowserClient call() throws Exception {
						return createMaster(kurentoTree, treeId);
					}
				});

		// Wait for browsers created and SDP negotiated
		BrowserClient masterBrowser = masterBrowserFuture.get();

		// TODO: If master is connected after viewers, the media never arrive to
		// viewer browsers.

		List<Future<TreeViewer>> viewers = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			final int key = i;
			viewers.add(exec.submit(new Callable<TreeViewer>() {
				@Override
				public TreeViewer call() throws Exception {
					return createViewer(key, kurentoTree, treeId);
				}
			}));
		}

		for (Future<TreeViewer> viewer : viewers) {
			viewer.get();
		}

		log.info("Browsers created");
		log.info("Waiting for media in viewers");

		// Wait for media in viewers
		for (Future<TreeViewer> viewer : viewers) {
			waitForEvent(viewer.get().getBrowser(), "playing");
		}

		log.info("Media received...");
		log.info("Start closing");

		// Close master browser
		masterBrowser.close();

		// Close viewer browsers
		for (Future<TreeViewer> viewer : viewers) {
			TreeViewer treeViewer = viewer.get();
			treeViewer.getBrowser().close();
			String sinkId = treeViewer.getSinkId();
			System.out.println("Removing sinkId: " + sinkId);
			kurentoTree.removeTreeSink(treeId, sinkId);
		}

		kurentoTree.releaseTree(treeId);

	}

	private BrowserClient createMaster(final KurentoTreeClient kurentoTree,
			final String treeId) {

		subscribeEvents(TestConfig.PRESENTER, "playing");
		initWebRtcSdpProcessor(TestConfig.PRESENTER, new SdpOfferProcessor() {
			@Override
			public String processSdpOffer(String sdpOffer) {
				try {
					return kurentoTree.setTreeSource(treeId, sdpOffer);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		return testScenario.getBrowserMap().get(TestConfig.PRESENTER);
	}

	private TreeViewer createViewer(int key, final KurentoTreeClient client,
			final String treeId) {
		BrowserClient browserViewer = testScenario.getBrowserMap().get(
				TestConfig.VIEWER + key);
		final TreeViewer treeClient = new TreeViewer(browserViewer, null);

		subscribeEvents(TestConfig.VIEWER + key, "playing");
		initWebRtcSdpProcessor(TestConfig.VIEWER + key,
				new SdpOfferProcessor() {
					@Override
					public String processSdpOffer(String sdpOffer) {
						try {
							TreeEndpoint treeEndpoint = client.addTreeSink(
									treeId, sdpOffer);
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
