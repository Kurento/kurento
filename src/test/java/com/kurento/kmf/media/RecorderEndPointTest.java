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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * {@link RecorderEndPoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RecorderEndPoint#getUri()}
 * <li>{@link RecorderEndPoint#record()}
 * <li>{@link RecorderEndPoint#pause()}
 * <li>{@link RecorderEndPoint#stop()}
 * </ul>
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class RecorderEndPointTest {

	@Autowired
	private MediaPipelineFactory pipelineFactory;

	private MediaPipeline pipeline;

	private RecorderEndPoint recorder;

	@Before
	public void setUpBeforeClass() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
		recorder = pipeline.createRecorderEndPoint(URL_FIWARECUT);
	}

	@After
	public void afterClass() {
		pipeline.release();
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
		Assert.assertTrue(URL_FIWARECUT.equals(recorder.getUri()));
	}
}
