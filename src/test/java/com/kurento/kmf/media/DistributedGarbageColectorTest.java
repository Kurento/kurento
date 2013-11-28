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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.AbstractMediaObject;
import com.kurento.kmf.media.internal.DistributedGarbageCollector;

/**
 * {@link DistributedGarbageCollector} test suite.
 * 
 * Checks the behaviour of the DGC.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class DistributedGarbageColectorTest {

	@Autowired
	private MediaPipelineFactory pipelineFactory;

	@Autowired
	private DistributedGarbageCollector dgc;

	/**
	 * Checks that the pipeline is not collected in KMS while it has children.
	 * 
	 * @throws InterruptedException
	 */
	@Test(expected = KurentoMediaFrameworkException.class)
	public void testPipelineWithChildrenNotCollected()
			throws InterruptedException {
		// Create a media pipeline with 1s. garbage collection time
		MediaPipeline pipeline = pipelineFactory.create(1);
		PlayerEndPoint player = pipeline.newPlayerEndPoint("").build();

		dgc.removeReference(((AbstractMediaObject) pipeline).getObjectRef()
				.getThriftRef());
		Thread.sleep(3000);

		Assert.assertNotNull(player.getMediaPipeline());

		player.release();
		// Give enough time to KMS´ GC to collect the pipeline
		Thread.sleep(3000);

		// Pipeline should now be deleted in KMS, so this should throw an
		// exception
		pipeline.newHttpEndPoint().build();
	}

}
