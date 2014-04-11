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
package com.kurento.test.recorder;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.demo.internal.VideoURLs;
import com.kurento.test.base.BaseArquillianTst;

/**
 * Integration test (JUnit/Arquillian) for HTTP Player. It uses
 * <code>HttpClient</code> to interact with Kurento Application Server (KAS).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.1.1
 * @see <a href="http://hc.apache.org//">Apache HTTP Component</a>
 */
@RunWith(Arquillian.class)
public class RecorderIT extends BaseArquillianTst {

	private static final Logger log = LoggerFactory.getLogger(RecorderIT.class);

	@Test
	public void testRecorderTunnel() throws IOException {
		testRecord("recorderTunnel", 200);
	}

	@Test
	public void testRecorderRedirect() throws IOException {
		testRecord("recorderRedirect", 307);
	}

	@Test
	public void testRecorderTunnelRepository() throws IOException {
		testRecord("recorderTunnelRepository", 200);
	}

	private void testRecord(String handler, int statusCode) throws IOException {
		// To follow redirect: .setRedirectStrategy(new LaxRedirectStrategy())
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("http://localhost:" + getServerPort()
				+ "/kmf-content-api-test/" + handler);
		MultipartEntityBuilder multipartEntity = MultipartEntityBuilder
				.create();
		multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		File file = new File("small");
		URL small = new URL(VideoURLs.map.get("small-webm"));
		FileUtils.copyURLToFile(small, file);
		FileBody fb = new FileBody(file);
		multipartEntity.addPart("file", fb);

		HttpEntity httpEntity = multipartEntity.build();
		post.setEntity(httpEntity);

		EntityUtils.consume(httpEntity);
		HttpResponse response = client.execute(post);
		final int responseStatusCode = response.getStatusLine().getStatusCode();

		log.info("Response Status Code: {}", responseStatusCode);
		log.info("Deleting tmp file: {}", file.delete());

		Assert.assertEquals("HTTP response status code must be " + statusCode,
				statusCode, responseStatusCode);
	}
}
