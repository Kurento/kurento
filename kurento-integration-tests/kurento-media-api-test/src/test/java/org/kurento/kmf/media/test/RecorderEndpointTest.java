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
package org.kurento.kmf.media.test;

import static org.kurento.kmf.media.test.RtpEndpoint2Test.URL_SMALL;

import org.junit.*;

import org.kurento.kmf.media.RecorderEndpoint;
import org.kurento.kmf.media.test.base.MediaPipelineBaseTest;

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
public class RecorderEndpointTest extends MediaPipelineBaseTest {

	private RecorderEndpoint recorder;

	@Before
	public void setupMediaElements() {
		recorder = pipeline.newRecorderEndpoint(URL_SMALL).build();
	}

	@After
	public void teardownMediaElements() {
		recorder.release();
	}

	/**
	 * start/pause/stop sequence test
	 */
	@Test
	public void testRecorder() {
		recorder.record();
		recorder.pause();
		recorder.stop();
	}

	@Test
	public void testCommandGetUri() {
		Assert.assertEquals(URL_SMALL, recorder.getUri());
	}

}
