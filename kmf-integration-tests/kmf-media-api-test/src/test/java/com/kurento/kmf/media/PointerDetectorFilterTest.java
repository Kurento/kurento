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

import static com.kurento.kmf.media.SyncMediaServerTest.URL_POINTER_DETECTOR;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.events.WindowOutEvent;

/**
 * {@link PointerDetectorFilter} test suite.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class PointerDetectorFilterTest extends MediaApiTest {

	private MediaPipeline pipeline;

	private PointerDetectorFilter filter;

	private PlayerEndpoint player;

	@Before
	public void setup() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
		filter = pipeline.newPointerDetectorFilter().build();
		player = pipeline.newPlayerEndpoint(URL_POINTER_DETECTOR).build();
		player.connect(filter);
	}

	@After
	public void teardown() {
		filter.release();
		player.release();
		pipeline.release();
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

		filter.addWindow(new PointerDetectorWindowMediaParam("goal", 50, 50,
				150, 150));
		final BlockingQueue<WindowInEvent> events = new ArrayBlockingQueue<WindowInEvent>(
				1);
		filter.addWindowInListener(new MediaEventListener<WindowInEvent>() {
			@Override
			public void onEvent(WindowInEvent event) {
				events.add(event);
			}
		});

		player.play();

		Assert.assertNotNull(events.poll(20, SECONDS));

		player.stop();
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void testWindowEvents() throws InterruptedException {

		PointerDetectorWindowMediaParam window0 = new PointerDetectorWindowMediaParam(
				"window0", 50, 50, 200, 50);

		PointerDetectorWindowMediaParam window1 = new PointerDetectorWindowMediaParam(
				"window1", 50, 50, 200, 150);

		filter.addWindow(window0);
		filter.addWindow(window1);

		final BlockingQueue<WindowInEvent> eventsIn = new ArrayBlockingQueue<>(
				1);

		final BlockingQueue<WindowOutEvent> eventsOut = new ArrayBlockingQueue<>(
				1);

		filter.addWindowInListener(new MediaEventListener<WindowInEvent>() {

			@Override
			public void onEvent(WindowInEvent event) {
				eventsIn.add(event);
			}
		});

		filter.addWindowOutListener(new MediaEventListener<WindowOutEvent>() {

			@Override
			public void onEvent(WindowOutEvent event) {
				eventsOut.add(event);
			}
		});

		player.play();
		Assert.assertTrue("window0".equals(eventsIn.poll(20, SECONDS)
				.getWindowId()));
		Assert.assertTrue("window0".equals(eventsOut.poll(5, SECONDS)
				.getWindowId()));

		player.stop();
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void testWindowOverlay() throws InterruptedException {

		PointerDetectorWindowMediaParam window0 = new PointerDetectorWindowMediaParam(
				"window0", 50, 50, 200, 50);
		filter.addWindow(window0);

		final BlockingQueue<WindowInEvent> eventsIn = new ArrayBlockingQueue<WindowInEvent>(
				1);

		final BlockingQueue<WindowOutEvent> eventsOut = new ArrayBlockingQueue<WindowOutEvent>(
				1);

		filter.addWindowInListener(new MediaEventListener<WindowInEvent>() {

			@Override
			public void onEvent(WindowInEvent event) {
				eventsIn.add(event);
			}
		});

		filter.addWindowOutListener(new MediaEventListener<WindowOutEvent>() {

			@Override
			public void onEvent(WindowOutEvent event) {
				eventsOut.add(event);
			}
		});

		player.play();
		Assert.assertNotNull(eventsIn.poll(10, TimeUnit.SECONDS));
		Assert.assertNotNull(eventsOut.poll(5, TimeUnit.SECONDS));
		player.stop();
	}

}
