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
package org.kurento.client.test.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.client.Continuation;
import org.kurento.client.MediaObject;
import org.kurento.client.MediaPipeline;
import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.testing.MediaApiTests;
import org.kurento.test.base.MediaApiTest;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 *
 */
@Category(MediaApiTests.class)
public abstract class MediaPipelineAsyncBaseTest extends MediaApiTest {

	protected MediaPipeline pipeline;

	@Before
	public void setupPipeline() throws InterruptedException {
		final BlockingQueue<MediaPipeline> events = new ArrayBlockingQueue<MediaPipeline>(
				1);
		pipelineFactory.create(new Continuation<MediaPipeline>() {
			@Override
			public void onSuccess(MediaPipeline result) {
				events.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoException(cause);
			}
		});
		pipeline = events.poll(3, TimeUnit.SECONDS);

		if (pipeline == null) {
			Assert.fail();
		}
	}

	@After
	public void teardownPipeline() throws InterruptedException {
		if (pipeline != null) {
			releaseMediaObject(pipeline);
		}
	}

	protected static void releaseMediaObject(final MediaObject mo)
			throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);

		if (mo != null) {
			mo.release(new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) {
					latch.countDown();
				}

				@Override
				public void onError(Throwable cause) {
					throw new KurentoException(cause);
				}
			});
			Assert.assertTrue("Timeout of 25s releasing object",
					latch.await(25, TimeUnit.SECONDS));
		}
	}

}
