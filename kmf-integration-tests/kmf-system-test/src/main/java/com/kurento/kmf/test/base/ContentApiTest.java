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
package com.kurento.kmf.test.base;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.kurento.kmf.spring.KurentoApplicationContextUtils;
import com.kurento.kmf.test.ContentApiBootApp;

/**
 * Base for tests using kmf-content-api and Spring Boot.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
public class ContentApiTest extends KurentoTest {

	protected ConfigurableApplicationContext context;

	protected static CountDownLatch terminateLatch;

	@Before
	public void setupContentApiWebApp() throws Exception {
		context = ContentApiBootApp.start();
	}

	@After
	public void teardownContentApiWebApp() throws Exception {

		if (context != null) {
			KurentoApplicationContextUtils
			.closeAllKurentoApplicationContexts(((WebApplicationContext) context)
					.getServletContext());
			context.close();
		}
	}
}
