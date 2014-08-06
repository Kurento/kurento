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

package org.kurento.repository.test.util;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.kurento.commons.tests.RepositoryApiTests;
import org.kurento.repository.main.BootApplication;

@Category(RepositoryApiTests.class)
public class ContextByTestSpringBootTest {

	private static Logger log = LoggerFactory
			.getLogger(ContextByTestSpringBootTest.class);

	protected static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {
		context = BootApplication.start();
	}

	@AfterClass
	public static void stop() {

		log.info("Closing...");

		context.close();

		log.info("Closed");
	}

	protected RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response)
					throws IOException {
			}
		});
		return restTemplate;
	}

}