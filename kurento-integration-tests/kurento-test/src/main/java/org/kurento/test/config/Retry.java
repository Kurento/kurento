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

	public void useReport() {
		testReport = new TestReport();
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
						base.evaluate();
						return;
					} catch (Throwable t) {
						exceptions.add(t);

						if (testReport != null) {
							int exSize = exceptions.size();
							testReport
									.appendHeader(description.getMethodName());
							testReport.appendWarning("Test with retry");
							testReport.appendHtml("Number of retries " + exSize
									+ "/" + getRetryCount());
							testReport.carriageReturn();
							testReport.appendException(t, testScenario);
						}

						caughtThrowable = t;
						log.error(SEPARATOR);
						log.error("{}: run {} failed",
								description.getDisplayName(), currentRetry, t);
						log.error(SEPARATOR);
					}
				}

				String errorMessage = "TEST FAILED: "
						+ description.getMethodName() + " (giving up after "
						+ retryCount + " failures)";
				if (exceptions.size() > 0 && testReport != null) {
					testReport.appendError(errorMessage);
					testReport.appendLine();
				}
				log.error(errorMessage);

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
