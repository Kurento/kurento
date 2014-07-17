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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.commons.tests.IntegrationTests;
import com.kurento.kmf.test.services.KurentoServicesTestHelper;

/**
 * Base for tests (Content and Media API).
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@Category(IntegrationTests.class)
public class KurentoTest {

	public static final String FILE_SCHEMA = "file://";

	private static final Logger log = LoggerFactory
			.getLogger(KurentoTest.class);

	@Rule
	public TestName testName = new TestName();

	@Rule
	public KmsLogOnFailure logOnFailure = new KmsLogOnFailure();

	protected int threshold = 25;

	/**
	 * Compares two numbers and return true|false if these number are similar,
	 * using a threshold in the comparison.
	 * 
	 * @param i
	 *            First number to be compared
	 * @param j
	 *            Second number to be compared
	 * @return true|false
	 */
	public boolean compare(double i, double j) {
		return Math.abs(j - i) <= (i * getThreshold() / 100);
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	protected int getServerPort() {
		return KurentoServicesTestHelper.getAppHttpPort();
	}

	public String getPathTestFiles() {
		return KurentoServicesTestHelper.getTestFilesPath();
	}

	@Before
	public void setupKurentoServices() throws Exception {

		log.info("Starting test {}",
				this.getClass().getName() + "." + testName.getMethodName());

		KurentoServicesTestHelper.setTestCaseName(this.getClass().getName());
		KurentoServicesTestHelper.setTestName(testName.getMethodName());
		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

	}

	@After
	public void teardownKurentoServices() throws Exception {
		KurentoServicesTestHelper.teardownServices();
	}

	/*
	 * If not specified, the default file for recording will have ".webm"
	 * extension.
	 */
	public String getDefaultFileForRecording() {
		return getDefaultFileForRecording(".webm");
	}

	public String getDefaultFileForRecording(String preffix) {
		File fileForRecording = new File(KurentoServicesTestHelper.getTestDir()
				+ "/" + KurentoServicesTestHelper.getTestCaseName());
		return fileForRecording.getAbsolutePath() + "/"
				+ KurentoServicesTestHelper.getTestName() + preffix;
	}

}
