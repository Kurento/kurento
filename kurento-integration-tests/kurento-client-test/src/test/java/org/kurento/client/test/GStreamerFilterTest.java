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

import static org.kurento.client.test.RtpEndpoint2Test.URL_SMALL;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.GStreamerFilter;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.MediaPipelineBaseTest;

/**
 * {@link GStreamerFilter} test suite.
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 3.0.1
 *
 */
public class GStreamerFilterTest extends MediaPipelineBaseTest {

	private GStreamerFilter filter;
	private PlayerEndpoint player;

	@Before
	public void setupMediaElements() {
		player = new PlayerEndpoint.Builder(pipeline, URL_SMALL).build();
	}

	@After
	public void teardownMediaElements() {
		player.release();
	}

	@Test
	public void testInstantiation() throws InterruptedException {

		filter = new GStreamerFilter.Builder(pipeline,
				"videoflip method=horizontal-flip").build();

		Assert.assertNotNull(filter);

		player.connect(filter);

		AsyncEventManager<EndOfStreamEvent> async = new AsyncEventManager<EndOfStreamEvent>(
				"EndOfStream event");

		 

		player.addEndOfStreamListener(async.getMediaEventListener());

		player.play();

		async.waitForResult();

		filter.release();
	}
}
