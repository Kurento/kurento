package org.kurento.tree.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.test.services.KurentoMediaServerManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.tree.server.app.KmsRegistrar;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@Category(SystemFunctionalTests.class)
public class TreeCloudRegisterRealMediaServerTest {

	private static final int NUM_KMSS = 3;

	@EnableAutoConfiguration
	static class TestConfiguration extends KurentoTreeServerApp {

		public KmsRegistrar registrar() {
			return new KmsRegistrar() {
				@Override
				public void register(String wsUri) {
					registeredKmss.add(wsUri);
				}
			};
		}
	}

	private static BlockingQueue<String> registeredKmss = new ArrayBlockingQueue<>(
			10);

	@Test
	public void registrarTest() throws IOException, InterruptedException {

		int port = KurentoServicesTestHelper.getAppHttpPort();

		System.setProperty(KurentoTreeServerApp.KMSS_URIS_PROPERTY, "[]");

		ConfigurableApplicationContext context = SpringApplication.run(
				TestConfiguration.class, "--server.port=" + port);
		context.start();

		List<KurentoMediaServerManager> kmss = new ArrayList<>();

		for (int i = 0; i < NUM_KMSS; i++) {

			int wsPort = 10000 + 2 * i;
			int httpPort = 10000 + 2 * i + 1;

			String createdKmsUri = "ws://localhost:" + wsPort + "/kurento";

			kmss.add(KurentoMediaServerManager.createWithWsTransport(
					createdKmsUri, httpPort));

			String kmsUri = registeredKmss.take();

			assertThat(kmsUri, is(createdKmsUri));
		}

		context.close();
	}
}
