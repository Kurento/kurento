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

import static org.kurento.client.test.RtpEndpoint2Test.URL_PLATES;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.GStreamerFilter;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.events.MediaEventListener;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.client.test.util.MediaPipelineAsyncBaseTest;

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
public class GStreamerFilterAsyncTest extends MediaPipelineAsyncBaseTest {

	private GStreamerFilter filter;

	private PlayerEndpoint player;

	@Before
	public void setupMediaElements() {
		player = pipeline.newPlayerEndpoint(URL_PLATES).build();
	}

	@After
	public void teardownMediaElements() {
		player.release();
	}

	@Test
	public void testInstantiation() throws InterruptedException {

		AsyncResultManager<GStreamerFilter> async = new AsyncResultManager<GStreamerFilter>(
				"GStreamerFilter creation");

		pipeline.newGStreamerFilter("videoflip method=horizontal-flip")
				.buildAsync(async.getContinuation());

		filter = async.waitForResult();

		releaseMediaObject(filter);
	}

}
