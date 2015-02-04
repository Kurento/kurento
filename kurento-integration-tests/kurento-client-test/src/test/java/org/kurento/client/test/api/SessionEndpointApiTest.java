package org.kurento.client.test.api;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaSessionStartedEvent;
import org.kurento.client.MediaSessionTerminatedEvent;
import org.kurento.client.SessionEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.MediaPipelineBaseTest;

public class SessionEndpointApiTest extends MediaPipelineBaseTest {

	private WebRtcEndpoint webrtcEP;

	@Before
	public void setupMediaElements() {
		webrtcEP = new WebRtcEndpoint.Builder(pipeline).build();
	}

	@After
	public void teardownMediaElements() {
		webrtcEP.release();
	}

	/**
	 * Tests
	 * {@link SessionEndpoint#addMediaSessionTerminatedListener(EventListener)}
	 * and
	 * {@link SessionEndpoint#removeMediaSessionTerminatedListener(ListenerSubscription)}
	 */
	@Test
	public void addRemoveSessionTerminatedListener() {
		ListenerSubscription subscription = webrtcEP
				.addMediaSessionTerminatedListener(new EventListener<MediaSessionTerminatedEvent>() {

					@Override
					public void onEvent(MediaSessionTerminatedEvent event) {
						// Intentionally left blank
					}
				});
		assertThat(subscription, is(notNullValue()));

		webrtcEP.removeMediaSessionTerminatedListener(subscription);
	}

	/**
	 * Tests
	 * {@link SessionEndpoint#addMediaSessionStartedListener(EventListener)} and
	 * {@link SessionEndpoint#removeMediaSessionStartedListener(ListenerSubscription)}
	 */
	@Test
	public void addRemoveSessionStartedListener() {
		ListenerSubscription subscription = webrtcEP
				.addMediaSessionStartedListener(new EventListener<MediaSessionStartedEvent>() {

					@Override
					public void onEvent(MediaSessionStartedEvent event) {
						// Intentionally left blank
					}
				});
		assertThat(subscription, is(notNullValue()));

		webrtcEP.removeMediaSessionStartedListener(subscription);
	}

}
