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
package org.kurento.test;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.TEST_CONFIG_JSON_DEFAULT;
import static org.kurento.test.TestConfiguration.TEST_NUMRETRIES_PROPERTY;
import static org.kurento.test.TestConfiguration.TEST_NUM_NUMRETRIES_DEFAULT;

import java.util.Date;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.kurento.commons.ConfigFileManager;
import org.kurento.test.config.Retry;

/**
 * Base for Kurento tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */
public class KurentoTest {

	static {
		ConfigFileManager.loadConfigFile(TEST_CONFIG_JSON_DEFAULT);
	}

	@Rule
	public TestName testName = new TestName();

	@Rule
	public Retry retry = new Retry(numRetries);

	public String testIdentifier = this.getClass().getSimpleName() + " ["
			+ new Date() + "]";

	protected static int numRetries = getProperty(TEST_NUMRETRIES_PROPERTY,
			TEST_NUM_NUMRETRIES_DEFAULT);

	public KurentoTest() {
		retry.useReport(testIdentifier);
	}

}
