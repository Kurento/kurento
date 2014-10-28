package org.kurento.tree.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Test;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.tree.server.app.KmsRegistrar;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import com.google.gson.JsonObject;

public class RegistrarTest {

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
	public void registrarTest() throws IOException {

		System.setProperty(KurentoTreeServerApp.KMSS_URIS_PROPERTY, "[]");
		System.setProperty("server.port", "8890");

		SpringApplication.run(TestConfiguration.class).start();

		JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
				"ws://localhost:8890/registrar");

		String wsUri = "ws://localhost:8888/kurento";

		JsonObject params = new JsonObject();
		params.addProperty("wsUri", wsUri);

		client.sendRequest("register", params);

		assertThat(registeredKmss.poll(), is(wsUri));

		client.close();
	}
}
