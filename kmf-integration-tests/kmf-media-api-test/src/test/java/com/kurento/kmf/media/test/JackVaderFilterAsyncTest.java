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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.*;

import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.test.base.MediaPipelineAsyncBaseTest;

/**
 * {@link JackVaderFilter} test suite.
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class JackVaderFilterAsyncTest extends MediaPipelineAsyncBaseTest {

	private PlayerEndpoint player;

	private JackVaderFilter jackVader;

	@Before
	public void setupMediaElements() throws InterruptedException {
		player = pipeline.newPlayerEndpoint(URL_SMALL).build();

		final BlockingQueue<JackVaderFilter> events = new ArrayBlockingQueue<JackVaderFilter>(
				1);
		pipeline.newJackVaderFilter().buildAsync(
				new Continuation<JackVaderFilter>() {

					@Override
					public void onSuccess(JackVaderFilter result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoException(cause);
					}
				});
		jackVader = events.poll(4, SECONDS);
		Assert.assertNotNull(jackVader);
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		player.release();
		releaseMediaObject(jackVader);
	}

	/**
	 * Test if a {@link JackVaderFilter} can be created in the KMS. The filter
	 * is pipelined with a {@link PlayerEndpoint}, which feeds video to the
	 * filter. This test depends on the correct behaviour of the player and its
	 * events.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testJackVaderFilter() throws InterruptedException {
		player.connect(jackVader);
		final BlockingQueue<EndOfStreamEvent> events = new ArrayBlockingQueue<EndOfStreamEvent>(
				1);
		player.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				events.add(event);
			}
		});

		player.play();

		Assert.assertNotNull(events.poll(10, SECONDS));
	}
}
