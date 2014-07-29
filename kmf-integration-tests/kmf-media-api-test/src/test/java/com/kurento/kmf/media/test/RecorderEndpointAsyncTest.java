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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.media.test.base.AsyncResultManager;
import com.kurento.kmf.media.test.base.MediaPipelineAsyncBaseTest;

/**
 * {@link RecorderEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RecorderEndpoint#getUri()}
 * <li>{@link RecorderEndpoint#record()}
 * <li>{@link RecorderEndpoint#pause()}
 * <li>{@link RecorderEndpoint#stop()}
 * </ul>
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class RecorderEndpointAsyncTest extends MediaPipelineAsyncBaseTest {

	private RecorderEndpoint recorder;

	@Before
	public void setupMediaElements() throws InterruptedException {

		AsyncResultManager<RecorderEndpoint> async = new AsyncResultManager<>(
				"RecorderEndpoint creation");

		pipeline.newRecorderEndpoint(URL_SMALL).buildAsync(
				async.getContinuation());

		recorder = async.waitForResult();

		Assert.assertNotNull(recorder);
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		releaseMediaObject(recorder);
	}

	@Test
	public void testGetUri() throws InterruptedException {

		AsyncResultManager<String> async = new AsyncResultManager<>(
				"recorder.getUri() invocation");

		recorder.getUri(async.getContinuation());

		String uri = async.waitForResult();

		Assert.assertEquals(URL_SMALL, uri);
	}

	@Test
	public void testRecorder() throws InterruptedException {

		AsyncResultManager<Void> asyncRecord = new AsyncResultManager<>(
				"recorder.record() invocation");
		recorder.record(asyncRecord.getContinuation());
		asyncRecord.waitForResult();

		AsyncResultManager<Void> asyncPause = new AsyncResultManager<>(
				"recorder.pause() invocation");
		recorder.pause(asyncPause.getContinuation());
		asyncPause.waitForResult();

		AsyncResultManager<Void> asyncStop = new AsyncResultManager<>(
				"recorder.stop() invocation");
		recorder.pause(asyncStop.getContinuation());
		asyncStop.waitForResult();

	}
}
