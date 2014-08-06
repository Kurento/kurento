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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.HttpEndpoint;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.ZBarFilter;
import org.kurento.client.events.CodeFoundEvent;
import org.kurento.client.events.MediaEventListener;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.MediaPipelineBaseTest;

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

		AsyncEventManager<CodeFoundEvent> async = new AsyncEventManager<>(
				"CodeFound event");

		zbar.addCodeFoundListener(async.getMediaEventListener());

		player.play();

		async.waitForResult();

		player.stop();
		player.release();
	}

}
