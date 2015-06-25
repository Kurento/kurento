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
package org.kurento.test.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit rule to retry test case in case of failure.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.0.0
 */
public class Retry implements TestRule {
	private static Logger log = LoggerFactory.getLogger(Retry.class);
	private static final String SEPARATOR = "=======================================";

	private int retryCount;
	private int currentRetry = 1;
	private List<Throwable> exceptions;
	private TestReport testReport;
	private TestScenario testScenario;

	public Retry(int retryCount) {
		this.retryCount = retryCount;
		exceptions = new ArrayList<>(retryCount);
	}

	public void useReport(String testName) {
		testReport = new TestReport(testName, retryCount);
	}

	public Statement apply(Statement base, Description description) {
		return statement(base, description);
	}

	private Statement statement(final Statement base,
			final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				Throwable caughtThrowable = null;
				for (; currentRetry <= retryCount; currentRetry++) {
					try {
						testReport.appendHeader(description.getMethodName()
								+ " - Execution " + (exceptions.size() + 1)
								+ "/" + getRetryCount());
						base.evaluate();
						testReport.appendSuccess("Test ok");
						testReport.flushExtraInfoHtml();
						testReport.appendLine();
						return;
					} catch (Throwable t) {
						exceptions.add(t);
						if (testReport != null) {
							testReport.appendWarning("Test failed in retry "
									+ exceptions.size());
							testReport.appendException(t, testScenario);
							testReport.flushExtraInfoHtml();
							testReport.flushExtraErrorHtml();
						}

						caughtThrowable = t;
						log.error(SEPARATOR);
						log.error("{}: run {} failed",
								description.getDisplayName(), currentRetry, t);
						log.error(SEPARATOR);
					}
				}

				String errorMessage = "TEST ERROR: "
						+ description.getMethodName() + " (giving up after "
						+ retryCount + " retries)";
				if (exceptions.size() > 0 && testReport != null) {
					testReport.appendError(errorMessage);
					testReport.appendLine();
				}

				throw caughtThrowable;
			}
		};
	}

	public int getCurrentRetry() {
		return currentRetry;
	}

	public List<Throwable> getExceptions() {
		return exceptions;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public TestReport getTestReport() {
		return testReport;
	}

	public void setTestScenario(TestScenario testScenario) {
		this.testScenario = testScenario;
	}

}
