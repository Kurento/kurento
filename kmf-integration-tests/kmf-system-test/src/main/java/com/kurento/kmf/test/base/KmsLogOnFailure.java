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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.test.services.KurentoServicesTestHelper;

/**
 * Utility class to print KMS log when a test fails.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class KmsLogOnFailure extends TestWatcher {

	private static final Logger log = LoggerFactory
			.getLogger(TestWatcher.class);

	@Override
	protected void failed(Throwable e, Description description) {

		if (KurentoServicesTestHelper.printKmsLog()) {

			String testDir = KurentoServicesTestHelper.getTestDir();
			String testCaseName = KurentoServicesTestHelper.getTestCaseName();
			String testName = KurentoServicesTestHelper.getTestName();
			File file = new File(testDir + "TEST-" + testCaseName + "/"
					+ testName + "-kms.log");

			if (file.exists()) {
				log.info("******************************************************************************");
				log.info("{}.{} FAILED", description.getClassName(), testName);
				log.info("\tcaused by: {} ({})", e.getClass()
						.getCanonicalName(), e.getMessage());
				log.info("******************************************************************************");

				try {
					for (String line : FileUtils.readLines(file)) {
						log.info(line);
					}
				} catch (IOException e1) {
					log.warn("Error reading lines in log file", e1);
				}

				log.info("******************************************************************************");
			}
		}

	}
}
