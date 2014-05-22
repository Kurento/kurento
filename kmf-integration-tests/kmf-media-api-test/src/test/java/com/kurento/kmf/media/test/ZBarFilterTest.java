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

import static com.kurento.kmf.media.test.RtpEndpoint2Test.URL_BARCODES;

import java.util.concurrent.*;

import org.junit.*;

import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.test.base.MediaPipelineBaseTest;

/**
 * {@link HttpEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link HttpEndpoint#getUrl()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link HttpEndpoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link HttpEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class ZBarFilterTest extends MediaPipelineBaseTest {

	private ZBarFilter zbar;

	@Before
	public void setupMediaElements() {
		zbar = pipeline.newZBarFilter().build();
	}

	@After
	public void teardownMediaElements() {
		zbar.release();
	}

	@Test
	public void testCodeFoundEvent() throws InterruptedException {

		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_BARCODES)
				.build();
		player.connect(zbar);

		final BlockingQueue<CodeFoundEvent> events = new ArrayBlockingQueue<CodeFoundEvent>(
				1);

		zbar.addCodeFoundListener(new MediaEventListener<CodeFoundEvent>() {

			@Override
			public void onEvent(CodeFoundEvent event) {
				events.add(event);
			}
		});

		player.play();

		Assert.assertNotNull(events.poll(10, TimeUnit.SECONDS));

		player.stop();
		player.release();
	}

}
