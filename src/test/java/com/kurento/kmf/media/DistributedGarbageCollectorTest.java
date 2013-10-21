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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.media.internal.DistributedGarbageCollector;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class DistributedGarbageCollectorTest {

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private DistributedGarbageCollector dgc;

	@Test
	public void testGarbageCollection() {
		MediaPipelineRef ref = Utils.createMediaPipelineRef();

		MediaPipeline pipeline = (MediaPipeline) ctx
				.getBean("mediaObject", ref);

		pipeline = null;

		// Instantiate lost of arrays to force a garbage collection
		for (int i = 0; i < 1000; i++) {
			String[] strArr = new String[100000];
		}

		// TODO complete the test checking wether the obj has been removed or
		// not
	}

}
