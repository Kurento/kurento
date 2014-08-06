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
package org.kurento.kmf.test.base;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import org.kurento.kmf.test.services.KurentoServicesTestHelper;

/**
 * Utility class to print KMS log when a test fails.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@SuppressWarnings("deprecation")
public class KmsLogOnFailure extends TestWatcher {

	@Override
	protected void failed(Throwable e, Description description) {

		if (KurentoServicesTestHelper.printKmsLog()) {

			File logFile = KurentoServicesTestHelper.getServerLogFile();

			if (logFile != null && logFile.exists()) {
				System.err
						.println("******************************************************************************");
				System.err.println("Log file path: "
						+ logFile.getAbsolutePath());
				System.err.println("Content:");

				try {
					for (String line : FileUtils.readLines(logFile)) {
						System.err.println(line);
					}
				} catch (IOException e1) {
					System.err.println("Error reading lines in log file");
					e1.printStackTrace();
				}

				System.err
						.println("******************************************************************************");

			}
		}

	}

	@SuppressWarnings({ "unused" })
	private void showException(Throwable e) {
		if (e instanceof org.junit.internal.runners.model.MultipleFailureException) {

			MultipleFailureException multipleEx = (MultipleFailureException) e;
			for (Throwable failure : multipleEx.getFailures()) {
				failure.printStackTrace();
			}

		} else {
			e.printStackTrace();
		}
	}

}
