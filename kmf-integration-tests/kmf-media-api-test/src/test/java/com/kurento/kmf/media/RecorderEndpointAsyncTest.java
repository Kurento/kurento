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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * {@link RecorderEndpoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RecorderEndpoint#getUri()}
 * <li>{@link RecorderEndpoint#record()}
 * <li>{@link RecorderEndpoint#pause()}
 * <li>{@link RecorderEndpoint#stop()}
 * </ul>
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class RecorderEndpointAsyncTest extends AbstractAsyncBaseTest {

	private RecorderEndpoint recorder;

	@Before
	public void setup() throws InterruptedException {
		final BlockingQueue<RecorderEndpoint> events = new ArrayBlockingQueue<RecorderEndpoint>(
				1);
		pipeline.newRecorderEndpoint(URL_SMALL).buildAsync(
				new Continuation<RecorderEndpoint>() {

					@Override
					public void onSuccess(RecorderEndpoint result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoMediaFrameworkException();
					}
				});
		recorder = events.poll(500, MILLISECONDS);
		Assert.assertNotNull(recorder);
	}

	@After
	public void teardown() throws InterruptedException {
		releaseMediaObject(recorder);
	}

	@Test
	public void testGetUri() throws InterruptedException {
		final BlockingQueue<String> events = new ArrayBlockingQueue<String>(1);

		recorder.getUri(new Continuation<String>() {

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

	@Test
	public void testRecorder() throws InterruptedException {

		final CountDownLatch recordLatch = new CountDownLatch(1);
		recorder.record(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) {
				recordLatch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		Assert.assertTrue(recordLatch.await(500, MILLISECONDS));

		final CountDownLatch pauseLatch = new CountDownLatch(1);
		recorder.pause(new Continuation<Void>() {
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
		recorder.stop(new Continuation<Void>() {
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

}
