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
import org.kurento.commons.testing.SystemStabilityTests;
import org.kurento.test.config.TestScenario;

/**
 * Stability tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
@Category(SystemStabilityTests.class)
public class StabilityTest extends BrowserKurentoClientTest {

	private static final int STABILITY_START_WAIT_MS = 10000; // ms

	public StabilityTest(TestScenario testScenario) {
		super(testScenario);

		try {
			log.info("Waiting {} ms before stability test startup",
					STABILITY_START_WAIT_MS);
			Thread.sleep(STABILITY_START_WAIT_MS);
		} catch (InterruptedException e) {
			log.warn("{} during wait of stability test startup", e.getClass());
		}
	}

}
