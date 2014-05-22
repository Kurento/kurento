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
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.*;

import org.junit.*;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.test.base.MediaPipelineAsyncBaseTest;

/**
 * {@link PlayerEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link PlayerEndpoint#getUri()}
 * <li>{@link PlayerEndpoint#play()}
 * <li>{@link PlayerEndpoint#pause()}
 * <li>{@link PlayerEndpoint#stop()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link PlayerEndpoint#addEndOfStreamListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class PlayerEndpointAsyncTest extends MediaPipelineAsyncBaseTest {

	private PlayerEndpoint player;

	@Before
	public void setupMediaElements() throws InterruptedException {

		final BlockingQueue<PlayerEndpoint> events = new ArrayBlockingQueue<PlayerEndpoint>(
				1);

		pipeline.newPlayerEndpoint(URL_SMALL).buildAsync(
				new Continuation<PlayerEndpoint>() {

					@Override
					public void onSuccess(PlayerEndpoint result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoMediaFrameworkException();
					}
				});
		player = events.poll(500, MILLISECONDS);
		Assert.assertNotNull(player);
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		releaseMediaObject(player);
	}

	@Test
	public void testGetUri() throws InterruptedException {
		final BlockingQueue<String> events = new ArrayBlockingQueue<String>(1);

		player.getUri(new Continuation<String>() {

			@Override
			public void onSuccess(String result) {
				events.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException();
			}
		});

		String uri = events.poll(500, MILLISECONDS);
		Assert.assertEquals(URL_SMALL, uri);
	}

	/**
	 * start/pause/stop sequence test
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testPlayer() throws InterruptedException {

		final CountDownLatch playLatch = new CountDownLatch(1);
		player.play(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) {
				playLatch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		Assert.assertTrue(playLatch.await(500, MILLISECONDS));

		final CountDownLatch pauseLatch = new CountDownLatch(1);
		player.pause(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) {
				pauseLatch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		Assert.assertTrue(pauseLatch.await(500, MILLISECONDS));

		final CountDownLatch stopLatch = new CountDownLatch(1);
		player.stop(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) {
				stopLatch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("stop player onError");
			}
		});
		Assert.assertTrue(stopLatch.await(500, MILLISECONDS));
	}

	@Test
	public void testEventEndOfStream() throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		final BlockingQueue<EndOfStreamEvent> events = new ArrayBlockingQueue<EndOfStreamEvent>(
				1);
		player.addEndOfStreamListener(
				new MediaEventListener<EndOfStreamEvent>() {

					@Override
					public void onEvent(EndOfStreamEvent event) {
						events.add(event);
					}
				}, new Continuation<ListenerRegistration>() {

					@Override
					public void onSuccess(ListenerRegistration result) {
						latch.countDown();
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoMediaFrameworkException(cause);
					}
				});
		latch.await(500, MILLISECONDS);

		player.play();

		EndOfStreamEvent event = events.poll(7, SECONDS);
		if (event == null) {
			Assert.fail();
		}
	}

	@Test
	public void testCommandGetUri() throws InterruptedException {

		final BlockingQueue<String> events = new ArrayBlockingQueue<String>(1);
		player.getUri(new Continuation<String>() {

			@Override
			public void onSuccess(String result) {
				events.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});

		String uri = events.poll(500, MILLISECONDS);
		if (uri == null || uri.isEmpty()) {
			Assert.fail();
		}
	}
}
