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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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
			String workspace = KurentoServicesTestHelper.getWorkspace();
			String testName = KurentoServicesTestHelper.getTestName();

			log.info("******************************************************************************");
			log.info("{}.{} FAILED", description.getClassName(), testName);
			log.info("\tcaused by: {} ({})", e.getClass().getCanonicalName(),
					e.getMessage());
			log.info("******************************************************************************");

			try {
				BufferedReader br = new BufferedReader(new FileReader(workspace
						+ testName + "/kms.log"));
				String line;
				while ((line = br.readLine()) != null) {
					log.info(line);
				}
				br.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

			log.info("******************************************************************************");
		}
	}
}
