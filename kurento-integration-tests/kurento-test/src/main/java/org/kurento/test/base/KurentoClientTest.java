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

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for tests using kurento-client.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */
public class KurentoClientTest {

	protected static Logger log = LoggerFactory.getLogger(KurentoClientTest.class);

	@Rule
	public TestName testName = new TestName();

	protected KurentoClientManager kurentoClientManager;
	protected KurentoClient kurentoClient;
	protected KurentoClient fakeKurentoClient;

	@Before
	public void setupKurentoClient() throws IOException {
		kurentoClientManager = new KurentoClientManager(testName, this.getClass());
		kurentoClient = kurentoClientManager.getKurentoClient();
		fakeKurentoClient = kurentoClientManager.getFakeKurentoClient();
	}

	@After
	public void teardownKurentoClient() throws Exception {
		kurentoClientManager.teardown();
	}

}
