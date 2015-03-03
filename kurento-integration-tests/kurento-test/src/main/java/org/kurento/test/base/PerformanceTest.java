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
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SystemPerformanceTests;
import org.kurento.test.config.TestScenario;
import org.kurento.test.monitor.SystemMonitorManager;

/**
 * Base for tests using kurento-client, Jetty Http Server and Selenium Grid.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@Category(SystemPerformanceTests.class)
public class PerformanceTest extends BrowserKurentoClientTest {

	protected SystemMonitorManager monitor;

	public PerformanceTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Before
	public void setupMonitor() {
		monitor = new SystemMonitorManager();
		monitor.start();
	}

	@After
	public void teardownMonitor() {
		monitor.stop();
		monitor.writeResults(getDefaultOutputFile("-monitor.csv"));
		monitor.destroy();
	}

}
