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
package org.kurento.test.base;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.kurento.client.KurentoClient;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoClientTestFactory;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.openqa.selenium.WebDriverException;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base for tests using kurento-client.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class KurentoClientTest extends KurentoTest {

	protected static KurentoClient kurentoClient;

	private static boolean startHttpServer;

	public KurentoClientTest() {
		super();
	}

	public KurentoClientTest(TestScenario testScenario) {
		super(testScenario);
		// HTTP server
		startHttpServer = !this.getClass().isAnnotationPresent(
				WebAppConfiguration.class);
		if (startHttpServer) {
			KurentoServicesTestHelper
					.startHttpServer(BrowserKurentoClientTest.class);
		}
	}

	@Before
	public void setupKurentoClient() throws IOException {
		// Kurento Services
		KurentoServicesTestHelper.setTestName(testName.getMethodName());
		KurentoServicesTestHelper.setTestCaseName(this.getClass().getName());
		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		log.info("Starting test {}",
				this.getClass().getName() + "." + testName.getMethodName());

		// Kurento client
		kurentoClient = KurentoClientTestFactory.createKurentoForTest();
	}

	@After
	public void teardownKurentoClient() throws Exception {
		// Kurento client
		if (kurentoClient != null) {
			kurentoClient.destroy();
		}

		// Kurento Services
		KurentoServicesTestHelper.teardownServices();
	}

	protected int getServerPort() {
		return KurentoServicesTestHelper.getAppHttpPort();
	}

	public static String getPathTestFiles() {
		return KurentoServicesTestHelper.getTestFilesPath();
	}

	public void addTestName(BrowserClient browserClient, String testName) {
		try {
			browserClient.executeScript("addTestName('" + testName + "');");
		} catch (WebDriverException we) {
			log.warn(we.getMessage());
		}
	}

}
