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
package org.kurento.media.test;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.media.HttpGetEndpoint;
import org.kurento.media.PlayerEndpoint;
import org.kurento.media.test.base.MediaPipelineBaseTest;

public class BasicPipelineTest extends MediaPipelineBaseTest {

	@Test
	public void basicPipelineTest() {

		PlayerEndpoint player = pipeline.newPlayerEndpoint(
				"http://files.kurento.org/video/small.webm").build();

		HttpGetEndpoint httpGetEndpoint = pipeline.newHttpGetEndpoint().build();

		player.connect(httpGetEndpoint);

		String url = httpGetEndpoint.getUrl();

		player.release();

		Assert.assertNotSame("The URL shouldn't be empty", "", url);
	}
}
