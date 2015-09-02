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
package org.kurento.client.test;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.HttpPostEndpoint;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.MediaPipelineBaseTest;

public class BasicPipelineTest extends MediaPipelineBaseTest {

	@Test
	public void basicPipelineTest() {

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		HttpPostEndpoint httpEndpoint = new HttpPostEndpoint.Builder(pipeline)
				.build();

		player.connect(httpEndpoint);
		
		for(int i=0; i<100; i++){
		
		WebRtcEndpoint webRtc = new WebRtcEndpoint.Builder(pipeline).build();
		
		player.connect(webRtc);
		
		}

		System.out.println("Dot length: "+pipeline.getGstreamerDot().getBytes().length);
		
		String url = httpEndpoint.getUrl();

		player.release();

		Assert.assertNotSame("The URL shouldn't be empty", "", url);
	}

}
