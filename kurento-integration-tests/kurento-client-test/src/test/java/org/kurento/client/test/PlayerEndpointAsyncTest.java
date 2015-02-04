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
 */
package org.kurento.client.test;

import static org.kurento.client.test.RtpEndpoint2Test.URL_SMALL;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.AsyncResultManager;

/**
 * {@link PlayerEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link PlayerEndpoint#getUri()}
 * <li>{@link PlayerEndpoint#play()}
 * <li>{@link PlayerEndpoint#pause()}
 * <li>{@link PlayerEndpoint#stop()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link PlayerEndpoint#addEndOfStreamListener(EventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@kurento.org)
 * @version 1.0.0
 *
 */
public class PlayerEndpointAsyncTest extends MediaPipelineAsyncBaseTest {

	private PlayerEndpoint player;

	@Before
	public void setupMediaElements() {

		AsyncResultManager<PlayerEndpoint> async = new AsyncResultManager<>(
				"PlayerEndpoint creation");

		new PlayerEndpoint.Builder(pipeline, URL_SMALL).buildAsync(async
				.getContinuation());

		player = async.waitForResult();
		Assert.assertNotNull(player);
	}

	@After
	public void teardownMediaElements() throws InterruptedException {
		releaseMediaObject(player);
	}

	@Test
	public void testGetUri() {

		AsyncResultManager<String> async = new AsyncResultManager<>(
				"player.getUri() invocation");

		player.getUri(async.getContinuation());

		String uri = async.waitForResult();

		Assert.assertEquals(URL_SMALL, uri);
	}

	/**
	 * start/pause/stop sequence test
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testPlayer() throws InterruptedException {

		AsyncResultManager<Void> async = new AsyncResultManager<>(
				"player.play() invocation");
		player.play(async.getContinuation());
		async.waitForResult();

		AsyncResultManager<Void> async2 = new AsyncResultManager<>(
				"player.pause() invocation");
		player.pause(async2.getContinuation());
		async2.waitForResult();

		AsyncResultManager<Void> async3 = new AsyncResultManager<>(
				"player.stop() invocation");
		player.stop(async3.getContinuation());
		async3.waitForResult();
	}

	@Test
	public void testEventEndOfStream() {

		AsyncResultManager<ListenerSubscription> asyncListener = new AsyncResultManager<>(
				"EndOfStream Listener registration");

		AsyncEventManager<EndOfStreamEvent> asyncEvent = new AsyncEventManager<>(
				"EndOfStream event");

		player.addEndOfStreamListener(asyncEvent.getMediaEventListener(),
				asyncListener.getContinuation());

		asyncListener.waitForResult();

		player.play();

		asyncEvent.waitForResult();
	}

}
