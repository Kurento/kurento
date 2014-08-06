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
package org.kurento.media.test.base;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.commons.tests.MediaApiTests;
import org.kurento.media.MediaPipeline;
import org.kurento.test.base.MediaApiTest;

@Category(MediaApiTests.class)
public abstract class MediaPipelineBaseTest extends MediaApiTest {

	protected MediaPipeline pipeline;

	@Before
	public void setupPipeline() {
		pipeline = pipelineFactory.create();
	}

	@After
	public void teardownPipeline() {
		if (pipeline != null) {
			pipeline.release();
		}
	}
}
