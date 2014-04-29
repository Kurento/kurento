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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.kurento.kmf.spring.KurentoApplicationContextUtils;
import com.kurento.kmf.test.BootApplication;
import com.kurento.kmf.test.KurentoMediaServer;
import com.kurento.kmf.test.PortManager;
import com.kurento.kmf.test.PropertiesManager;

/**
 * Base for tests using kmf-content-api and Spring Boot.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
public class ContentApiTest {

	private boolean springBootEnabled = false;

	public static Logger log = LoggerFactory.getLogger(ContentApiTest.class);

	protected ConfigurableApplicationContext context;

	private KurentoMediaServer kms;

	@Rule
	public TestName testName = new TestName();

	@Before
	public void start() throws Exception {
		context = BootApplication.start();
		springBootEnabled = true;

		// KMS
		String serverAddress = PropertiesManager.getSystemProperty(
				"kurento.serverAddress", "127.0.0.1");
		int serverPort = PropertiesManager.getSystemProperty(
				"kurento.serverPort", 9090);
		int httpEndpointPort = PropertiesManager.getSystemProperty(
				"httpEPServer.serverPort", 9091);

		kms = new KurentoMediaServer(serverAddress, serverPort,
				httpEndpointPort);
		if (kms.isConfigAvailable()) {
			kms.start(testName.getMethodName());
		}
	}

	@After
	public void stop() {
		log.info("*** Closing...");
		if (context != null) {
			KurentoApplicationContextUtils
					.closeAllKurentoApplicationContexts(((WebApplicationContext) context)
							.getServletContext());
			context.close();
		}
		log.info("*** Closed");

		if (kms.isConfigAvailable()) {
			kms.stop();
		}
	}

	public int getServerPort() {
		return PortManager.getPort();
	}

	public boolean isSpringBootEnabled() {
		return springBootEnabled;
	}

}
