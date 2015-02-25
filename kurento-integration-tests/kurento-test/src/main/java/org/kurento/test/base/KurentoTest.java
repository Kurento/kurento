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
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.config.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for Kurento tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */

@RunWith(Parameterized.class)
public class KurentoTest {

	public static final Logger log = LoggerFactory.getLogger(KurentoTest.class);

	@Rule
	public TestName testName = new TestName();

	public TestScenario testScenario;

	public KurentoTest() {
	}

	public KurentoTest(TestScenario testScenario) {
		this.testScenario = testScenario;
	}

	@Before
	public void setupKurentoTest() {
		for (BrowserClient browserClient : testScenario.getBrowserMap()
				.values()) {
			browserClient.init();
		}
	}

	@After
	public void teardownKurentoTest() {
		for (BrowserClient browserClient : testScenario.getBrowserMap()
				.values()) {
			browserClient.close();
		}
	}

}
