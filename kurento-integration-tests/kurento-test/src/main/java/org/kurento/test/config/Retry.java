/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.test.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.internal.runners.model.MultipleFailureException;
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
@SuppressWarnings("deprecation")
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
    testReport = TestReport.getSingleton(testName, retryCount);
  }

  public void useReport(String testName, String htmlHeader) {
    testReport = TestReport.getSingleton(testName, retryCount, htmlHeader);
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return statement(base, description);
  }

  private Statement statement(final Statement base, final Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        Throwable caughtThrowable = null;
        for (; currentRetry <= retryCount; currentRetry++) {
          try {
            testReport.appendHeader(description.getClassName() + "." + description.getMethodName()
                + " - Execution " + (exceptions.size() + 1) + "/" + getRetryCount());
            base.evaluate();
            testReport.flushExtraInfoHtml();
            testReport.appendSuccess("Test ok");
            testReport.flushExtraInfoHtml();
            testReport.appendLine();
            return;
          } catch (Throwable t) {

            if (t instanceof MultipleFailureException) {
              MultipleFailureException m = (MultipleFailureException) t;
              for (Throwable throwable : m.getFailures()) {
                log.warn("Multiple exception element", throwable);
              }
            }

            exceptions.add(t);
            if (testReport != null) {
              testReport.appendWarning("Test failed in retry " + exceptions.size());
              testReport.appendException(t, testScenario);
              testReport.flushExtraInfoHtml();
              testReport.flushExtraErrorHtml();
            }

            caughtThrowable = t;
            log.error(SEPARATOR);
            log.error("{}: run {} failed", description.getDisplayName(), currentRetry, t);
            log.error(SEPARATOR);
          }
        }

        String errorMessage = "TEST ERROR: " + description.getMethodName() + " (giving up after "
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
