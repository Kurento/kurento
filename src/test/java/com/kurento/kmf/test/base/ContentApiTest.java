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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.kurento.kmf.spring.KurentoApplicationContextUtils;
import com.kurento.kmf.test.BootApplication;
import com.kurento.kmf.test.PortManager;

/**
 * Base for tests using kmf-content-api and Spring Boot.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
public class ContentApiTest {

	private static boolean springBootEnabled = false;

	public static Logger log = LoggerFactory.getLogger(ContentApiTest.class);

	public final static int TIMEOUT = 100; // seconds

	protected static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {
		context = BootApplication.start();
		springBootEnabled = true;
	}

	@AfterClass
	public static void stop() {
		log.info("*** Closing...");
		if (context != null) {
			KurentoApplicationContextUtils
					.closeAllKurentoApplicationContexts(((WebApplicationContext) context)
							.getServletContext());

			context.close();
		}

		log.info("*** Closed");
	}

	public int getServerPort() {
		return PortManager.getPort();
	}

	public static boolean isSpringBootEnabled() {
		return springBootEnabled;
	}

}
