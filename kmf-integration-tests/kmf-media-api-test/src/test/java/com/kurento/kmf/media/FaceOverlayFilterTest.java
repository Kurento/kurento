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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * {@link FaceOverlayFilter} test suite.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 * 
 */
public class FaceOverlayFilterTest extends MediaApiTest {

	private MediaPipeline pipeline;

	private FaceOverlayFilter overlayFilter;

	@Before
	public void setup() throws KurentoMediaFrameworkException {

		pipeline = pipelineFactory.create();
		overlayFilter = pipeline.newFaceOverlayFilter().build();
	}

	@After
	public void teardown() {
		overlayFilter.release();
		pipeline.release();
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
	public void testFaceOverlayFilter() throws InterruptedException {
		PlayerEndpoint player = pipeline
				.newPlayerEndpoint(URL_POINTER_DETECTOR).build();
		player.connect(overlayFilter);

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

		player.stop();
		player.release();
	}

}
