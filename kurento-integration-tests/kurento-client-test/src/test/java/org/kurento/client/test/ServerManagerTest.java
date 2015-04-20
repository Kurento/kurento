package org.kurento.client.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hamcrest.core.IsSame;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaObject;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ObjectCreatedEvent;
import org.kurento.client.ServerManager;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.KurentoClientTest;
import org.kurento.test.services.KurentoClientTestFactory;

public class ServerManagerTest extends KurentoClientTest {

	@Test
	public void testSameInstance() throws InterruptedException {

		ServerManager server = kurentoClient.getServerManager();
		ServerManager server2 = kurentoClient.getServerManager();

		assertThat(server, IsSame.sameInstance(server2));
	}

	@Test
	public void testObjectCreationEvents() throws InterruptedException {

		ServerManager server = kurentoClient.getServerManager();

		final Exchanger<MediaObject> exchanger = new Exchanger<>();

		server.addObjectCreatedListener(new EventListener<ObjectCreatedEvent>() {
			@Override
			public void onEvent(ObjectCreatedEvent event) {
				try {
					exchanger.exchange(event.getObject());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		try {
			MediaObject eventObject = exchanger.exchange(null, 10,
					TimeUnit.SECONDS);

			System.out.println("pipeline: " + pipeline);
			System.out.println("eventObject: " + eventObject);

			assertThat(pipeline, IsSame.sameInstance(eventObject));

		} catch (TimeoutException e) {
			fail(ObjectCreatedEvent.class.getName() + " should be thrown");
		}
	}

	@Test
	public void readPipelines() {

		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		ServerManager serverManager = kurentoClient.getServerManager();
		List<MediaPipeline> mediaPipelines = serverManager.getPipelines();

		for (MediaPipeline p : mediaPipelines) {
			String gstreamerDot = p.getGstreamerDot();
			System.out.println(p.getId() + ": " + gstreamerDot);
		}

		assertTrue(mediaPipelines.contains(pipeline));
	}

	@Test
	public void readPipelineElements() throws IOException {

		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		new WebRtcEndpoint.Builder(pipeline).build();

		KurentoClient otherKurentoClient = KurentoClientTestFactory
				.createKurentoForTest();

		ServerManager serverManager = otherKurentoClient.getServerManager();

		List<MediaPipeline> mediaPipelines = serverManager.getPipelines();

		for (MediaObject o : mediaPipelines.get(0).getChilds()) {
			if (o.getId().indexOf("WebRtcEndpoint") >= 0) {
				WebRtcEndpoint webRtc = (WebRtcEndpoint) o;

				assertThat(pipeline, is(webRtc.getParent()));
			}
		}
	}

}
