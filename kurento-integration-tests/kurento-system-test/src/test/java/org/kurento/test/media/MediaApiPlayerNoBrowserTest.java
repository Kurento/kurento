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
package org.kurento.test.media;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.tests.SystemMediaApiTests;
import org.kurento.media.HttpGetEndpoint;
import org.kurento.media.MediaPipeline;
import org.kurento.media.PlayerEndpoint;
import org.kurento.test.base.MediaApiTest;

/**
 * <strong>Description</strong>: HTTP Player, tested with HttpClient (not
 * Selenium).<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Received content/type is video/webm</li>
 * </ul>
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@Category(SystemMediaApiTests.class)
public class MediaApiPlayerNoBrowserTest extends MediaApiTest {

	@Test
	public void testPlayer() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/small.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP.connect(httpEP);
		playerEP.play();

		// Test execution
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet httpGet = new HttpGet(httpEP.getUrl());
		HttpResponse response = client.execute(httpGet);
		HttpEntity resEntity = response.getEntity();

		// Assertions
		Assert.assertEquals("Response content-type must be video/webm",
				"video/webm", resEntity.getContentType().getValue());
	}

}
