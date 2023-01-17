/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.lifecycle;

import static org.kurento.test.services.TestService.TestServiceScope.TEST;
import static org.kurento.test.services.TestService.TestServiceScope.TESTCLASS;
import static org.kurento.test.services.TestService.TestServiceScope.TESTSUITE;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.FrameworkField;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.services.TestService;
import org.kurento.test.services.TestService.TestServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener to handle lifecycle of tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTestListener extends RunListener {

  private Class<?> testClass;
  private List<FrameworkField> serviceFields;
  private static List<TestService> serviceRunners = new ArrayList<>();

  protected static Logger log = LoggerFactory.getLogger(KurentoTestListener.class);

  public enum ServiceMethod {
    START, STOP;
  }

  public KurentoTestListener(List<FrameworkField> services) {

    this.serviceFields = services;

    for (FrameworkField service : serviceFields) {
      TestService serviceRunner = null;
      try {
        serviceRunner = (TestService) service.getField().get(null);
        if (!serviceRunners.contains(serviceRunner)) {
          serviceRunners.add(serviceRunner);
          if (serviceRunner.getScope() == TESTSUITE) {
            serviceRunner.start();
          }
        }

      } catch (Throwable e) {
        log.warn("Exception instanting service in class {}", serviceRunner, e);
      }
    }

  }

  private void invokeServices(ServiceMethod method, TestServiceScope scope) {
    for (TestService serviceRunner : serviceRunners) {
      log.trace("** Only for debugging: serviceRunner: {}. All Service Runners: {}", serviceRunner,
          serviceRunners);
      log.trace("** Only for debugging: serviceRunner.getScope: {}", serviceRunner.getScope());
      if (serviceRunner.getScope() == scope) {
        if (method == ServiceMethod.START) {
          serviceRunner.start();
        } else if (method == ServiceMethod.STOP) {
          log.trace("** Only for debugging: serviceRunner.stop() {}", serviceRunner);
          serviceRunner.stop();
          log.trace(
              "### Only for debugging: After serviceRunner.stop() -> serviceRunner: {}. All Service Runners: {}",
              serviceRunner, serviceRunners);
        }
      }
    }
  }

  @Override
  public void testRunStarted(Description description) {
    testClass = description.getTestClass();

    log.debug("Starting test class {}", testClass.getName());
    invokeServices(ServiceMethod.START, TESTCLASS);
  }

  @Override
  public void testRunFinished(Result result) {
    log.debug("Finishing test class {}. Test(s) failed: {}", testClass.getName(),
        result.getFailureCount());

    invokeServices(ServiceMethod.STOP, TESTCLASS);
  }

  @Override
  public void testStarted(Description description) {
    String methodName = description.getMethodName();
    KurentoTest.setTestMethodName(methodName);
    log.debug("Starting test {}.{}", testClass.getName(), methodName);

    invokeServices(ServiceMethod.START, TEST);

    KurentoTest
        .logMessage("|       TEST STARTING: " + description.getClassName() + "." + methodName);
  }

  @Override
  public void testFinished(Description description) {
    String methodName = description.getMethodName();
    log.debug("Finishing test {}.{}", testClass.getName(), methodName);

    invokeServices(ServiceMethod.STOP, TEST);

    KurentoTestWatcher.invokeMethodsAnnotatedWith(FinishedTest.class, description.getTestClass(),
        null, description);
  }

  public void testSuiteFinished() {
    log.debug("Finishing test suite");
    invokeServices(ServiceMethod.STOP, TESTSUITE);
  }
}
