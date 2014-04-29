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

import static com.kurento.kmf.media.SyncMediaServerTest.URL_BARCODES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;

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
public class ZBarFilterAsyncTest extends AbstractAsyncBaseTest {

	private ZBarFilter zbar;
	private PlayerEndpoint player;

	@Before
	public void setup() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);
		pipeline.newZBarFilter().buildAsync(new Continuation<ZBarFilter>() {

			@Override
			public void onSuccess(ZBarFilter result) {
				zbar = result;
				sem.release();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException();
			}
		});
		Assert.assertTrue(sem.tryAcquire(500, MILLISECONDS));

		player = pipeline.newPlayerEndpoint(URL_BARCODES).build();
	}

	@After
	public void teardown() throws InterruptedException {
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

		CodeFoundEvent event = events.poll(5000, MILLISECONDS);
		Assert.assertNotNull(event);

		player.stop();
	}

}
