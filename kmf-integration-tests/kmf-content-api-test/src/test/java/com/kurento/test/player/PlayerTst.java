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
package com.kurento.test.player;

import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.demo.internal.EventListener;
import com.kurento.kmf.commons.tests.ContentApiTests;

/**
 * Generic test for HTTP Player using <code>HttpClient</code> to connect to
 * Kurento Application Server (KAS).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see <a href="http://hc.apache.org/">Apache HTTP Components</a>
 */
@Category(ContentApiTests.class)
public class PlayerTst implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(PlayerTst.class);

	private final String url;
	private final int statusCode;
	private final String contentType;
	private final boolean interrupt;
	private final String[] expectedHandlerFlow;

	public PlayerTst(String handler, String port, int statusCode,
			String contentType, boolean interrupt, String[] expectedHandlerFlow) {
		this.url = "http://localhost:" + port + "/kmf-content-api-test/"
				+ handler;
		this.statusCode = statusCode;
		this.contentType = contentType;
		this.interrupt = interrupt;
		this.expectedHandlerFlow = expectedHandlerFlow;
	}

	@Override
	public void run() {
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet httpGet = new HttpGet(url);
			HttpResponse response = client.execute(httpGet);
			HttpEntity resEntity = response.getEntity();

			if (interrupt) {
				// Interrupt test
				resEntity.getContent().close();
			} else if (contentType != null) {
				// If not reject test
				final long initTime = new Date().getTime();
				EntityUtils.consume(resEntity);
				final long seconds = (new Date().getTime() - initTime) / 1000 % 60;
				log.info("Play time: " + seconds + " seconds");
			}

			final int responseStatusCode = response.getStatusLine()
					.getStatusCode();
			log.info("ReasonPhrase "
					+ response.getStatusLine().getReasonPhrase());
			log.info("statusCode " + responseStatusCode);
			Assert.assertEquals("HTTP response status code must be "
					+ statusCode, statusCode, responseStatusCode);

			if (expectedHandlerFlow != null) {
				final List<String> realEventList = EventListener.getEventList();
				log.info("Real Event List: " + realEventList);
				Assert.assertArrayEquals(expectedHandlerFlow,
						realEventList.toArray());
			}

			if (contentType != null && resEntity.getContentType() != null) {
				Header responseContentType = resEntity.getContentType();
				log.info("contentType " + responseContentType.getValue());
				Assert.assertEquals("Content-Type in response header must be "
						+ contentType, contentType,
						responseContentType.getValue());
			}

		} catch (Exception e) {
			log.error("Exception in Player Test", e);
		}
	}
}
