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

import java.io.IOException;
import java.util.concurrent.Semaphore;

import junit.framework.Assert;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;

/**
 * {@link HttpEndPoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link HttpEndPoint#getUrl()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link HttpEndPoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link HttpEndPoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 * 
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class HttpEndpointTest {

	@Autowired
	private MediaPipelineFactory pipelineFactory;

	private MediaPipeline pipeline;

	@Before
	public void setUpBeforeClass() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
	}

	@After
	public void afterClass() {
		pipeline.release();
	}

	/**
	 * Checks that the getUrl method does not return an empty string
	 */
	@Test
	public void testMethodGetUrl() {
		HttpEndPoint httpEP = pipeline.createHttpEndPoint();
		Assert.assertTrue(!httpEP.getUrl().isEmpty());
	}

	/**
	 * Test for {@link MediaSessionStartedEvent}
	 */
	@Test
	public void testEventMediaSessionStarted() {
		final PlayerEndPoint player = pipeline.createPlayerEndPoint(URL_SMALL);
		HttpEndPoint httpEP = pipeline.createHttpEndPoint();
		player.connect(httpEP);

		final Semaphore sem = new Semaphore(0);
		player.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				sem.release();
			}
		});

		httpEP.addMediaSessionStartListener(new MediaEventListener<MediaSessionStartedEvent>() {

			@Override
			public void onEvent(MediaSessionStartedEvent event) {
				player.play();
			}
		});

		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEP.getUrl()));
		} catch (ClientProtocolException e) {
			throw new KurentoMediaFrameworkException();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException();
		}

		try {
			sem.acquire();
		} catch (InterruptedException e) {
			throw new KurentoMediaFrameworkException();
		}

	}

	/**
	 * Test for {@link MediaSessionTerminatedEvent}
	 */
	// TODO how to test this event?
	@Ignore
	@Test
	public void testEventMediaSessionTerminated() {
		HttpEndPoint httpEP = pipeline.createHttpEndPoint(1, 1);

		final Semaphore sem = new Semaphore(0);

		httpEP.addMediaSessionTerminatedListener(new MediaEventListener<MediaSessionTerminatedEvent>() {

			@Override
			public void onEvent(MediaSessionTerminatedEvent event) {
				sem.release();
			}
		});

		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {
			// This should trigger MediaSessionStartedEvent
			httpclient.execute(new HttpGet(httpEP.getUrl()));
		} catch (ClientProtocolException e) {
			throw new KurentoMediaFrameworkException();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException();
		}

		try {
			sem.acquire();
		} catch (InterruptedException e) {
			throw new KurentoMediaFrameworkException();
		}

	}

}
