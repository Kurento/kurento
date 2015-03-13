package org.kurento.test.functional.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.Tag;
import org.kurento.test.base.FunctionalTest;

public class EventTagTest extends FunctionalTest {

	public EventTagTest() {
		super(null);
	}

	private static int TAG_SIZE = 3;
	private static long TIMEOUT = 15; // seconds

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { {} });
	}

	@Test
	public void testEventTag() throws Exception {
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		final CountDownLatch eventReceived = new CountDownLatch(TAG_SIZE);

		PlayerEndpoint player = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/red.webm").build();

		player.addTag("test_1", "value_1");
		player.addTag("test_2", "value_2");
		player.addTag("test_3", "value_3");

		player.setSendTagsInEvents(true);

		player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				List<Tag> tags = event.getTags();

				for (Tag tag : tags) {
					if (tag.getKey().compareTo("test_1") == 0) {
						if (tag.getValue().compareTo("value_1") == 0) {
							eventReceived.countDown();
						}
					} else if (tag.getKey().compareTo("test_2") == 0) {
						if (tag.getValue().compareTo("value_2") == 0) {
							eventReceived.countDown();
						}
					} else if (tag.getKey().compareTo("test_3") == 0) {
						if (tag.getValue().compareTo("value_3") == 0) {
							eventReceived.countDown();
						}
					}
				}
			}
		});

		player.play();
		// Guard time to reproduce the whole video
		if (!eventReceived.await(TIMEOUT, TimeUnit.SECONDS)) {
			Assert.fail("Event not received");
		}

	}

	@Test
	public void testEventWithoutTag() throws Exception {
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		final CountDownLatch eventReceived = new CountDownLatch(1);

		PlayerEndpoint player = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/red.webm").build();

		player.addTag("test_1", "value_1");
		player.addTag("test_2", "value_2");
		player.addTag("test_3", "value_3");

		player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				List<Tag> tags = event.getTags();

				if (tags.size() == 0) {
					eventReceived.countDown();
				}
			}
		});

		player.play();
		// Guard time to reproduce the whole video
		if (!eventReceived.await(TIMEOUT, TimeUnit.SECONDS)) {
			Assert.fail("Event not received");
		}
	}
}
