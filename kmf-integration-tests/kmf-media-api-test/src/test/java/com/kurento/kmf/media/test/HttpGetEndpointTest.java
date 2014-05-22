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
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.concurrent.*;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.*;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.*;
import com.kurento.kmf.media.test.base.MediaPipelineBaseTest;

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
	 */
	@Test
	public void testEventMediaSessionStarted() throws InterruptedException {
		final PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL)
				.build();
		HttpGetEndpoint httpEP = pipeline.newHttpGetEndpoint().build();
		player.connect(httpEP);

		final BlockingQueue<EndOfStreamEvent> eosEvents = new ArrayBlockingQueue<>(
				1);
		player.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosEvents.add(event);
			}
		});

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
		} catch (ClientProtocolException e) {
			throw new KurentoMediaFrameworkException();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException();
		}

		Assert.assertNotNull(eosEvents.poll(60, SECONDS));

		httpEP.release();
		player.release();
	}

	/**
	 * Test for {@link MediaSessionTerminatedEvent}
	 *
	 * @throws InterruptedException
	 */
	@Ignore
	@Test
	public void testEventMediaSessionTerminated() throws InterruptedException {
		HttpGetEndpoint httpEP = pipeline.newHttpGetEndpoint().build();

		final Semaphore sem = new Semaphore(0);

		httpEP.addMediaSessionTerminatedListener(new MediaEventListener<MediaSessionTerminatedEvent>() {

			@Override
			public void onEvent(MediaSessionTerminatedEvent event) {
				sem.release();
			}
		});

		try (CloseableHttpClient httpclient = HttpClientBuilder.create()
				.build()) {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEP.getUrl()));
		} catch (ClientProtocolException e) {
			throw new KurentoMediaFrameworkException();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException();
		}

		Assert.assertTrue(sem.tryAcquire(50, SECONDS));

		httpEP.release();
	}
}
