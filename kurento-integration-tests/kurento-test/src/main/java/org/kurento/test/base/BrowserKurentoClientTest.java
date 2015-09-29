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

import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.IntegrationTests;
import org.kurento.test.client.KurentoTestClient;
import org.kurento.test.config.TestScenario;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Base for tests using kurento-client and HTTP Server.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
@EnableAutoConfiguration
@Category(IntegrationTests.class)
public class BrowserKurentoClientTest extends KurentoClientTest<KurentoTestClient> {

	public BrowserKurentoClientTest(TestScenario testScenario) {
		super(testScenario);
	}

	public BrowserKurentoClientTest() {
		super();
	}

}
