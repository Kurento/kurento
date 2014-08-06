/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.base;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.client.factory.KmfMediaApi;
import org.kurento.client.factory.MediaPipelineFactory;

/**
 * Base for tests using kmf-media-api.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiTest extends KurentoTest {

	public static Logger log = LoggerFactory.getLogger(MediaApiTest.class);

	protected MediaPipelineFactory pipelineFactory;

	@Before
	public void setupMediaPipelineFactory() throws Exception {

		pipelineFactory = KmfMediaApi
				.createMediaPipelineFactoryFromSystemProps();
	}

	@After
	public void teardownMediaPipelineFactory() throws Exception {

		if (pipelineFactory != null) {
			pipelineFactory.destroy();
		}
	}
}
