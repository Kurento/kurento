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

import static com.kurento.kmf.media.SyncMediaServerTest.URL_SMALL;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
 * {@link JackVaderFilter} test suite.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class JackVaderFilterTest {

	@Autowired
	private MediaPipelineFactory pipelineFactory;

	private MediaPipeline pipeline;

	@Before
	public void setUpBeforeClass() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
	}

	@After
	public void afterClass() {
		pipeline.release();
	}

	@Test
	public void testJackVaderFilter() throws InterruptedException {
		String uriStr = URL_SMALL;
		PlayerEndPoint player = pipeline.createPlayerEndPoint(uriStr);

		Assert.assertTrue(uriStr.equals(player.getUri()));

		JackVaderFilter jackVader = pipeline.createJackVaderFilter();

		player.connect(jackVader);

		final Semaphore sem = new Semaphore(0);

		player.play();

		Assert.assertTrue(sem.tryAcquire(10, TimeUnit.SECONDS));

		player.stop();
		jackVader.release();
		player.release();
	}

}
