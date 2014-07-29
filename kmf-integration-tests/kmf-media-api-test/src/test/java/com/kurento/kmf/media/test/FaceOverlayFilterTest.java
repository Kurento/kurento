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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.test.base.AsyncEventManager;
import com.kurento.kmf.media.test.base.MediaPipelineBaseTest;

/**
 * {@link FaceOverlayFilter} test suite.
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 *
 */
public class FaceOverlayFilterTest extends MediaPipelineBaseTest {

	private FaceOverlayFilter overlayFilter;

	@Before
	public void setupMediaElements() {

		overlayFilter = pipeline.newFaceOverlayFilter().build();
	}

	@After
	public void teardownMediaElements() {

		overlayFilter.release();
	}

	/**
	 * Test if a {@link FaceOverlayFilter} can be created in the KMS. The filter
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

		AsyncEventManager<EndOfStreamEvent> async = new AsyncEventManager<>(
				"EndOfStream event");

		player.addEndOfStreamListener(async.getMediaEventListener());

		player.play();

		async.waitForResult();

		player.stop();
		player.release();
	}

}
