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

import static com.kurento.kmf.media.test.RtpEndpoint2Test.URL_POINTER_DETECTOR;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.PointerDetectorFilter;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.test.base.MediaPipelineAsyncBaseTest;

/**
 * {@link PointerDetectorFilter} test suite.
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
@Ignore
public class PointerDetectorFilterAsyncTest extends MediaPipelineAsyncBaseTest {

	private PlayerEndpoint player;

	private PointerDetectorFilter filter;

	@Before
	public void setupMediaElements() throws InterruptedException {
		player = pipeline.newPlayerEndpoint(URL_POINTER_DETECTOR).build();

		final BlockingQueue<PointerDetectorFilter> events = new ArrayBlockingQueue<PointerDetectorFilter>(
				1);
		pipeline.newPointerDetectorFilter().buildAsync(
				new Continuation<PointerDetectorFilter>() {

					@Override
					public void onSuccess(PointerDetectorFilter result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoException(cause);
					}
				});
		filter = events.poll(4, SECONDS);
		Assert.assertNotNull(filter);
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		player.release();
		releaseMediaObject(filter);
	}

	/**
	 * Test if a {@link PointerDetectorFilter} can be created in the KMS. The
	 * filter is pipelined with a {@link PlayerEndpoint}, which feeds video to
	 * the filter. This test depends on the correct behaviour of the player and
	 * its events.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testPointerDetectorFilter() throws InterruptedException {
		player.connect(filter);
		final BlockingQueue<EndOfStreamEvent> events = new ArrayBlockingQueue<EndOfStreamEvent>(
				1);
		player.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				events.add(event);
			}
		});

		player.play();

		Assert.assertNotNull(events.poll(20, SECONDS));
	}
}
