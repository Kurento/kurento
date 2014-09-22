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
package org.kurento.test.sanity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.client.MediaPipeline;
import org.kurento.commons.testing.SanityTests;
import org.kurento.test.base.KurentoClientTest;

/**
 * Sanity test of a Media Pipeline creation and release.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@Category(SanityTests.class)
public class PipelineTest extends KurentoClientTest {

	@Test
	public void basicPipelineTest() {
		MediaPipeline mediaPipeline = MediaPipeline.with(kurentoClient)
				.create();
		Assert.assertNotNull("Error: MediaPipeline is null", mediaPipeline);
		mediaPipeline.release();
	}

}
