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
package com.kurento.kmf.media.test;

import static com.kurento.kmf.media.test.RtpEndpoint2Test.URL_SMALL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;
import com.kurento.kmf.media.test.base.MediaPipelineAsyncBaseTest;

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
public class HttpGetEndpointAsyncTest extends MediaPipelineAsyncBaseTest {

	private HttpGetEndpoint httpEp;

	@Before
	public void setupMediaElements() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);
		pipeline.newHttpGetEndpoint().buildAsync(
				new Continuation<HttpGetEndpoint>() {

					@Override
					public void onSuccess(HttpGetEndpoint result) {
						httpEp = result;
						sem.release();
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoException(cause);
					}
				});
		Assert.assertTrue("HttpGetEndpoint no created in 500ms",
				sem.tryAcquire(500, MILLISECONDS));
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
		final BlockingQueue<String> events = new ArrayBlockingQueue<>(1);
		httpEp.getUrl(new Continuation<String>() {

			@Override
			public void onSuccess(String result) {
				events.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoException(cause);
			}
		});

		String url = events.poll(500, MILLISECONDS);
		Assert.assertTrue(!(url == null || url.isEmpty()));
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

		final PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL)
				.build();
		player.connect(httpEp);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		player.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		final BlockingQueue<ListenerRegistration> events = new ArrayBlockingQueue<>(
				1);
		httpEp.addMediaSessionStartedListener(
				new MediaEventListener<MediaSessionStartedEvent>() {

					@Override
					public void onEvent(MediaSessionStartedEvent event) {
						player.play();
					}
				}, new Continuation<ListenerRegistration>() {

					@Override
					public void onSuccess(ListenerRegistration result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoException(cause);
					}
				});

		Assert.assertNotNull("MediaSessionStartedEvent not send in 500ms",
				events.poll(500, MILLISECONDS));

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEp.getUrl()));
		}

		try {
			eosLatch.await(500, MILLISECONDS);
		} finally {
			player.release();
		}

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

		final PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL)
				.build();
		player.connect(httpEp);

		httpEp.addMediaSessionStartedListener(new MediaEventListener<MediaSessionStartedEvent>() {

			@Override
			public void onEvent(MediaSessionStartedEvent event) {
				player.play();
			}
		});
		final CountDownLatch latch = new CountDownLatch(1);
		final BlockingQueue<ListenerRegistration> events = new ArrayBlockingQueue<>(
				1);
		httpEp.addMediaSessionTerminatedListener(
				new MediaEventListener<MediaSessionTerminatedEvent>() {

					@Override
					public void onEvent(MediaSessionTerminatedEvent event) {
						latch.countDown();
					}
				}, new Continuation<ListenerRegistration>() {

					@Override
					public void onSuccess(ListenerRegistration result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoException(cause);
					}
				});

		Assert.assertNotNull("Listener not registered in 500ms",
				events.poll(500, MILLISECONDS));

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEp.getUrl()));
		}

		try {
			latch.await(500, MILLISECONDS);
		} finally {
			player.release();
		}

	}

}
