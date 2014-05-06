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
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
public class ContentApiTest {

	public static final Logger log = LoggerFactory
			.getLogger(ContentApiTest.class);
	private boolean springBootEnabled = false;
	protected ConfigurableApplicationContext context;
	private KurentoMediaServer kms;
	protected static CountDownLatch terminateLatch;
	private boolean autostart;

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
		autostart = PropertiesManager.getSystemProperty("kurento.autostart", true);

		log.info("Configuring KMS in {}:{}, with httpEP por {}", serverAddress,
				serverPort, httpEndpointPort);

		kms = new KurentoMediaServer(serverAddress, serverPort,
				httpEndpointPort);
		if (autostart && kms.isConfigAvailable()) {
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

		if (autostart && kms.isConfigAvailable()) {
			kms.stop();
		}
	}

	public int getServerPort() {
		return PortManager.getPort();
	}

	public boolean isSpringBootEnabled() {
		return springBootEnabled;
	}

	public KurentoMediaServer getKms() {
		return kms;
	}

}
