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
 *
 */
package org.kurento.client.test;

import static org.kurento.client.test.RtpEndpoint2Test.URL_SMALL;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.events.EndOfStreamEvent;
import org.kurento.client.events.MediaEventListener;
import org.kurento.client.events.MediaSessionStartedEvent;
import org.kurento.client.events.MediaSessionTerminatedEvent;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.MediaPipelineBaseTest;

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
 * {@link HttpGetEndpoint#addMediaSessionStartedListener(MediaEventListener)}
 * <li>
 * {@link HttpGetEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class HttpGetEndpointTest extends MediaPipelineBaseTest {

	/**
	 * Checks that the getUrl method does not return an empty string
	 */
	@Test
	public void testMethodGetUrl() {
		HttpGetEndpoint httpEP = pipeline.newHttpGetEndpoint().build();
		Assert.assertTrue(!httpEP.getUrl().isEmpty());
	}

	/**
	 * Test for {@link MediaSessionStartedEvent}
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@Test
	public void testEventMediaSessionStarted() throws InterruptedException,
			ClientProtocolException, IOException {

		final PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL)
				.build();

		HttpGetEndpoint httpEP = pipeline.newHttpGetEndpoint().build();
		player.connect(httpEP);

		AsyncEventManager<EndOfStreamEvent> async = new AsyncEventManager<>(
				"EndOfStream event");

		player.addEndOfStreamListener(async.getMediaEventListener());

		httpEP.addMediaSessionStartedListener(new MediaEventListener<MediaSessionStartedEvent>() {
			@Override
			public void onEvent(MediaSessionStartedEvent event) {
				player.play();
			}
		});

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEP.getUrl()));
		}

		async.waitForResult();

		httpEP.release();
		player.release();
	}

	/**
	 * Test for {@link MediaSessionTerminatedEvent}
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@Test
	public void testEventMediaSessionTerminated() throws InterruptedException,
			ClientProtocolException, IOException {
		final PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL)
				.build();
		HttpGetEndpoint httpEP = pipeline.newHttpGetEndpoint().terminateOnEOS()
				.build();
		player.connect(httpEP);

		httpEP.addMediaSessionStartedListener(new MediaEventListener<MediaSessionStartedEvent>() {

			@Override
			public void onEvent(MediaSessionStartedEvent event) {
				player.play();
			}
		});

		final BlockingQueue<MediaSessionTerminatedEvent> events = new ArrayBlockingQueue<>(
				1);
		httpEP.addMediaSessionTerminatedListener(new MediaEventListener<MediaSessionTerminatedEvent>() {

			@Override
			public void onEvent(MediaSessionTerminatedEvent event) {
				events.add(event);
			}
		});

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEP.getUrl()));
		}

		Assert.assertNotNull("MediaSessionTerminatedEvent not sent in 20s",
				events.poll(20, SECONDS));

		httpEP.release();
		player.release();
	}
}
