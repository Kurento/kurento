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

import static com.kurento.kmf.media.test.RtpEndpoint2Test.URL_PLATES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.*;

import org.junit.*;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.kmf.media.test.base.MediaPipelineAsyncBaseTest;

/**
 * {@link PlateDetectorFilter} test suite.
 *
 * Events tested:
 * <ul>
 * <li>{@link PlateDetectorFilter#addPlateDetectedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 2.0.1
 *
 */
public class PlateDetectorFilterAsyncTest extends MediaPipelineAsyncBaseTest {

	private PlateDetectorFilter detector;

	private PlayerEndpoint player;

	@Before
	public void setupMediaElements() throws KurentoMediaFrameworkException,
	InterruptedException {
		final BlockingQueue<PlateDetectorFilter> events = new ArrayBlockingQueue<PlateDetectorFilter>(
				1);

		player = pipeline.newPlayerEndpoint(URL_PLATES).build();

		pipeline.newPlateDetectorFilter().buildAsync(
				new Continuation<PlateDetectorFilter>() {

					@Override
					public void onSuccess(PlateDetectorFilter result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoMediaFrameworkException();
					}
				});

		detector = events.poll(500, MILLISECONDS);
		Assert.assertNotNull(detector);
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		player.release();
		releaseMediaObject(detector);
		pipeline.release();
	}

	@Test
	public void testEventPlateDetected() throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		final BlockingQueue<PlateDetectedEvent> events = new ArrayBlockingQueue<PlateDetectedEvent>(
				1);
		detector.addPlateDetectedListener(
				new MediaEventListener<PlateDetectedEvent>() {

					@Override
					public void onEvent(PlateDetectedEvent event) {
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
		player.connect(detector);
		player.play();

		PlateDetectedEvent event = events.poll(7, SECONDS);
		Assert.assertNotNull(event);
	}

}
