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

import static com.kurento.kmf.media.SyncMediaServerTest.URL_PLATES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;

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
public class PlateDetectorFilterTest extends MediaApiTest {

	private MediaPipeline pipeline;

	private PlateDetectorFilter detector;

	private PlayerEndpoint player;

	@Before
	public void setup() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
		player = pipeline.newPlayerEndpoint(URL_PLATES).build();
		detector = pipeline.newPlateDetectorFilter().build();
	}

	@After
	public void teardown() {
		player.release();
		detector.release();
		pipeline.release();
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

		PlateDetectedEvent event = events.poll(7, SECONDS);
		Assert.assertNotNull(event);
	}

}
