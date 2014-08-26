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

import org.junit.After;
import org.junit.Before;
import org.kurento.client.factory.KurentoClient;
import org.kurento.client.factory.KurentoClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for tests using kurento-client.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class KurentoClientTest extends KurentoTest {

	public static Logger log = LoggerFactory.getLogger(KurentoClientTest.class);

	protected KurentoClient kurentoClient;

	@Before
	public void setupMediaPipelineFactory() throws Exception {

		kurentoClient = KurentoClientFactory.createKurentoForTest();
	}

	@After
	public void teardownMediaPipelineFactory() throws Exception {

		if (kurentoClient != null) {
			kurentoClient.destroy();
		}
	}
}
