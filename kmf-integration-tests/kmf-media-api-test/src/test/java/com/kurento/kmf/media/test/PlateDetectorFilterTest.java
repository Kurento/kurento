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
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.*;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.kmf.media.test.base.MediaPipelineBaseTest;

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
public class PlateDetectorFilterTest extends MediaPipelineBaseTest {

	private PlateDetectorFilter detector;

	private PlayerEndpoint player;

	@Before
	public void setupMediaElements() throws KurentoMediaFrameworkException {
		player = pipeline.newPlayerEndpoint(URL_PLATES).build();
		detector = pipeline.newPlateDetectorFilter().build();
	}

	@After
	public void teardownMediaElements() {
		player.release();
		detector.release();
	}

	@Test
	public void testEventPlateDetected() throws InterruptedException {
		final BlockingQueue<PlateDetectedEvent> events = new ArrayBlockingQueue<PlateDetectedEvent>(
				1);
		detector.addPlateDetectedListener(new MediaEventListener<PlateDetectedEvent>() {

			@Override
			public void onEvent(PlateDetectedEvent event) {
				events.add(event);
			}
		});
		player.connect(detector);
		player.play();

		PlateDetectedEvent event = events.poll(20, SECONDS);
		Assert.assertNotNull(event);
	}

}
