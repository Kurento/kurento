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
package org.kurento.client.test;

import static org.kurento.client.test.RtpEndpoint2Test.URL_BARCODES;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.HttpEndpoint;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.ZBarFilter;
import org.kurento.client.events.CodeFoundEvent;
import org.kurento.client.events.MediaEventListener;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.client.test.util.MediaPipelineAsyncBaseTest;

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
public class ZBarFilterAsyncTest extends MediaPipelineAsyncBaseTest {

	private ZBarFilter zbar;
	private PlayerEndpoint player;

	@Before
	public void setupMediaElements() throws InterruptedException {

		AsyncResultManager<ZBarFilter> async = new AsyncResultManager<>(
				"ZBarFilter creation");

		pipeline.newZBarFilter().buildAsync(async.getContinuation());

		zbar = async.waitForResult();

		player = pipeline.newPlayerEndpoint(URL_BARCODES).build();
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		releaseMediaObject(zbar);
		player.release();
	}

	@Test
	public void testCodeFoundEvent() throws InterruptedException {
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

		CodeFoundEvent event = events.poll(10, TimeUnit.SECONDS);
		Assert.assertNotNull(event);

		player.stop();
	}

}
