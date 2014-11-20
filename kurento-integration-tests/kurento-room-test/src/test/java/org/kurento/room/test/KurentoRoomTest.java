package org.kurento.room.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.testing.KurentoTreeTests;
import org.kurento.room.client.KurentoRoomManagerClient;
import org.kurento.room.client.Room;
import org.kurento.room.client.Stream;
import org.kurento.room.server.app.KurentoRoomServerApp;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.SdpOfferProcessor;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

@Category(KurentoTreeTests.class)
public class KurentoRoomTest {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoRoomTest.class);

	private static final int NUM_VIEWERS = 4;

	@Test
	public void test() throws Exception {

		int port = KurentoServicesTestHelper.getAppHttpPort();

		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		ConfigurableApplicationContext roomServer = startKurentoRoomServer();

		final KurentoRoomManagerClient kurentoRoomManager = KurentoRoomManagerClient
				.create("ws://localhost:" + port + "/kurento-room");

		String token = kurentoRoomManager.createRoom("Room 1");

		final Room room = kurentoRoomManager.getRoom(token);

		ExecutorService exec = Executors.newFixedThreadPool(NUM_VIEWERS + 1);

		List<Future<BrowserClient>> users = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			users.add(exec.submit(new Callable<BrowserClient>() {
				@Override
				public BrowserClient call() throws Exception {
					return createUser(room);
				}
			}));
		}

		for (Future<BrowserClient> user : users) {
			user.get();
		}

		log.info("Browsers created");
		log.info("Waiting for users");

		// Wait for media in viewers
		for (Future<BrowserClient> viewer : users) {
			viewer.get().waitForEvent("playing");
		}

		log.info("Media received...");
		log.info("Start closing");

		// Close user browsers
		for (Future<BrowserClient> user : users) {
			user.get().close();
		}

		room.close();

		roomServer.close();

		KurentoServicesTestHelper.teardownServices();
	}

	private ConfigurableApplicationContext startKurentoRoomServer() {

		String kmsUri = PropertiesManager.getProperty(
				KurentoServicesTestHelper.KMS_WS_URI_PROP,
				KurentoServicesTestHelper.KMS_WS_URI_DEFAULT);

		System.setProperty(KurentoRoomServerApp.KMSS_URI_PROPERTY, "[\""
				+ kmsUri + "\",\"" + kmsUri + "\"]");

		System.setProperty(KurentoRoomServerApp.WEBSOCKET_PORT_PROPERTY,
				Integer.toString(KurentoServicesTestHelper.getAppHttpPort()));

		return KurentoRoomServerApp.start();
	}

	private BrowserClient createUser(final Room room) {

		BrowserClient browser = new BrowserClient.Builder().client(
				Client.WEBRTC).build();

		browser.subscribeEvents("playing");
		browser.initWebRtcSdpProcessor(new SdpOfferProcessor() {
			@Override
			public String processSdpOffer(String sdpOffer) {
				Stream stream = room.publishStream(sdpOffer);
				return stream.getSdpResponse();
			}
		}, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

		return browser;
	}

}
