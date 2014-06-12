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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.media.HttpPostEndpoint;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.test.base.MediaPipelineBaseTest;

/**
 * {@link HttpPostEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link HttpPostEndpoint#getUrl()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>
 * {@link HttpPostEndpoint#addMediaSessionStartedListener(MediaEventListener)}
 * <li>
 * {@link HttpPostEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class HttpPostEndpointTest extends MediaPipelineBaseTest {

	/**
	 * Checks that the getUrl method does not return an empty string
	 */
	@Test
	public void testMethodGetUrl() {
		HttpPostEndpoint httpEP = pipeline.newHttpPostEndpoint().build();
		assertTrue(!httpEP.getUrl().isEmpty());
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
		HttpPostEndpoint httpEP = pipeline.newHttpPostEndpoint().build();
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
			throw new KurentoException();
		} catch (IOException e) {
			throw new KurentoException(e);
		}

		assertNotNull(eosEvents.poll(7, SECONDS));

		httpEP.release();
		player.release();
	}

}
