/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.FAKE_KMS_WS_URI_PROP;

import java.io.IOException;

import org.junit.rules.TestName;
import org.kurento.client.KurentoClient;
import org.kurento.test.services.KurentoClientTestFactory;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for kurento client.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */
public class KurentoClientManager {

	protected static Logger log = LoggerFactory.getLogger(KurentoClientManager.class);

	protected KurentoClient kurentoClient;
	protected KurentoClient fakeKurentoClient;

	public KurentoClientManager(TestName testName, Class<?> clazz) throws IOException {
		// Kurento services
		KurentoServicesTestHelper.setTestName(testName.getMethodName());
		KurentoServicesTestHelper.setTestCaseName(clazz.getName());
		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		log.info("Starting test {}", clazz.getName() + "." + testName.getMethodName());

		// Kurento client
		kurentoClient = KurentoClientTestFactory.createKurentoForTest();

		// Fake Kurento client
		String fakeWsUri = getProperty(FAKE_KMS_WS_URI_PROP);
		if (fakeWsUri != null) {
			fakeKurentoClient = KurentoClient.create(fakeWsUri);
		}
	}

	public void teardown() throws Exception {
		// Kurento client
		if (kurentoClient != null) {
			kurentoClient.destroy();
		}

		// Fake Kurento client
		if (fakeKurentoClient != null) {
			fakeKurentoClient.destroy();
		}

		// Kurento services
		KurentoServicesTestHelper.teardownServices();
	}

	public KurentoClient getKurentoClient() {
		return kurentoClient;
	}

	public KurentoClient getFakeKurentoClient() {
		return fakeKurentoClient;
	}

}
