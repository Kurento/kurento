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

import static com.kurento.kmf.media.SyncMediaServerTest.URL_FIWARECUT;

import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * {@link PlayerEndPoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link PlayerEndPoint#getUri()}
 * <li>{@link PlayerEndPoint#play()}
 * <li>{@link PlayerEndPoint#pause()}
 * <li>{@link PlayerEndPoint#stop()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link PlayerEndPoint#addEndOfStreamListener(MediaEventListener)}
 * </ul>
 * 
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class PlayerEndPointTest {

	@Autowired
	private MediaPipelineFactory pipelineFactory;

	private MediaPipeline pipeline;

	private PlayerEndPoint player;

	@Before
	public void setUpBeforeClass() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
		player = pipeline.createPlayerEndPoint(URL_FIWARECUT);
	}

	@After
	public void afterClass() {
		pipeline.release();
		player.release();
	}

	/**
	 * start/pause/stop sequence test
	 */
	@Test
	public void testPlayer() {
		player.play();
		player.pause();
		player.stop();
	}

	@Test
	public void testEventEndOfStream() {

		final Semaphore sem = new Semaphore(0);

		player.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				sem.release();
			}
		});

		player.play();
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			throw new KurentoMediaFrameworkException(e);
		}
	}

	@Test
	public void testCommandGetUri() {
		Assert.assertTrue(URL_FIWARECUT.equals(player.getUri()));
	}

}
