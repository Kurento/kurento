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
package com.kurento.kmf.media;

import static com.kurento.kmf.media.SyncMediaServerTest.URL_SMALL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;

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
public class HttpGetEndpointAsyncTest extends AbstractAsyncBaseTest {

	private HttpGetEndpoint httpEp;

	@Before
	public void setup() throws InterruptedException {
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
						throw new KurentoMediaFrameworkException(cause);
					}
				});
		Assert.assertTrue(sem.tryAcquire(500, MILLISECONDS));
	}

	@After
	public void teardown() throws InterruptedException {
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
				throw new KurentoMediaFrameworkException(cause);
			}
		});

		String url = events.poll(500, MILLISECONDS);
		Assert.assertTrue(!(url == null || url.isEmpty()));
	}

	/**
	 * Test for {@link MediaSessionStartedEvent}
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testEventMediaSessionStarted() throws InterruptedException {

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
						throw new KurentoMediaFrameworkException(cause);
					}
				});

		ListenerRegistration reg = events.poll(500, MILLISECONDS);
		Assert.assertNotNull(reg);

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEp.getUrl()));
		} catch (ClientProtocolException e) {
			throw new KurentoMediaFrameworkException();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException();
		}

		try {
			eosLatch.await(500, MILLISECONDS);
		} catch (InterruptedException e) {
			player.release();
			throw new KurentoMediaFrameworkException(e);
		}

	}

	/**
	 * Test for {@link MediaSessionTerminatedEvent}
	 */
	@Ignore
	@Test
	public void testEventMediaSessionTerminated() {
		// TODO how to test this event?
	}

}
