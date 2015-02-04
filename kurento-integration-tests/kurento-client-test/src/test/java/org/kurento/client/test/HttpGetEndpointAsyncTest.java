/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.kurento.client.test;

import static org.kurento.client.test.RtpEndpoint2Test.URL_SMALL;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaSessionStartedEvent;
import org.kurento.client.MediaSessionTerminatedEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.AsyncResultManager;

/**
 * {@link HttpGetEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link HttpGetEndpoint#getUrl()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>
 * {@link HttpGetEndpoint#addMediaSessionStartedListener(EventListener)}
 * <li>
 * {@link HttpGetEndpoint#addMediaSessionTerminatedListener(EventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@kurento.org)
 * @version 1.0.0
 *
 */
@Ignore
public class HttpGetEndpointAsyncTest extends MediaPipelineAsyncBaseTest {

	private HttpGetEndpoint httpEp;

	@Before
	public void setupMediaElements() {

		AsyncResultManager<HttpGetEndpoint> async = new AsyncResultManager<>(
				"HttpGetEndpoint creation");

		new HttpGetEndpoint.Builder(pipeline).buildAsync(async
				.getContinuation());

		httpEp = async.waitForResult();
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		releaseMediaObject(httpEp);
	}

	/**
	 * Checks that the getUrl method does not return an empty string
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testMethodGetUrl() throws InterruptedException {

		AsyncResultManager<String> async = new AsyncResultManager<>(
				"getUrl() method invocation");

		httpEp.getUrl(async.getContinuation());

		String url = async.waitForResult();

		Assert.assertTrue((url != null) && !url.isEmpty());
	}

	/**
	 * Test for {@link MediaSessionStartedEvent}
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testEventMediaSessionStarted() throws InterruptedException,
			IOException {

		final PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				URL_SMALL).build();

		player.connect(httpEp);

		AsyncEventManager<EndOfStreamEvent> async = new AsyncEventManager<>(
				"EndOfStream event");

		player.addEndOfStreamListener(async.getMediaEventListener());

		AsyncResultManager<ListenerSubscription> async2 = new AsyncResultManager<>(
				"EventListener subscription");

		httpEp.addMediaSessionStartedListener(
				new EventListener<MediaSessionStartedEvent>() {
					@Override
					public void onEvent(MediaSessionStartedEvent event) {
						player.play();
					}
				}, async2.getContinuation());

		async2.waitForResult();

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEp.getUrl()));
		}

		async.waitForResult();
		player.release();
	}

	/**
	 * Test for {@link MediaSessionTerminatedEvent}
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testEventMediaSessionTerminated() throws InterruptedException,
			IOException {

		final PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				URL_SMALL).build();

		player.connect(httpEp);

		httpEp.addMediaSessionStartedListener(new EventListener<MediaSessionStartedEvent>() {
			@Override
			public void onEvent(MediaSessionStartedEvent event) {
				player.play();
			}
		});

		AsyncResultManager<ListenerSubscription> async = new AsyncResultManager<>(
				"EventListener subscription");

		AsyncEventManager<MediaSessionTerminatedEvent> asyncEvent = new AsyncEventManager<>(
				"MediaSessionTerminated event");

		httpEp.addMediaSessionTerminatedListener(
				asyncEvent.getMediaEventListener(), async.getContinuation());

		async.waitForResult();

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEp.getUrl()));
		}

		asyncEvent.waitForResult();
		player.release();

	}
}
