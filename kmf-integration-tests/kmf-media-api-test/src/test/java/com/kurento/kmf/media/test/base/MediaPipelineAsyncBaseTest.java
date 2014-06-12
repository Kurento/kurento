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
package com.kurento.kmf.media.test.base;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;

import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.commons.tests.MediaApiTests;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.test.base.MediaApiTest;

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
			Assert.assertTrue("Timeout of 5s releasing object",
					latch.await(5, TimeUnit.SECONDS));
		}
	}

}
