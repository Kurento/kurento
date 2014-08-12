package org.kurento.control.server.test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.factory.KurentoClient;
import org.kurento.commons.testing.KurentoControlServerTests;
import org.kurento.control.server.KurentoControlServerApp;
import org.springframework.context.ConfigurableApplicationContext;

@Category(KurentoControlServerTests.class)
public class KurentoControlServerClientTest {

	@Test
	public void kurentoClientTest() throws IOException, InterruptedException {

		ConfigurableApplicationContext context = KurentoControlServerApp
				.start();

		KurentoClient kurentoClient = KurentoClient
				.create("ws://127.0.0.1:8888/kurento");

		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build();

		player.connect(httpGetEndpoint);

		String url = httpGetEndpoint.getUrl();

		assertThat(url, not(isEmptyString()));

		player.release();
		pipeline.release();

		context.close();
	}
}
