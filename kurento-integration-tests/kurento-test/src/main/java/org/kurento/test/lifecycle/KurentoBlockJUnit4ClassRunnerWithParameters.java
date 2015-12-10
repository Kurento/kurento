package org.kurento.test.lifecycle;

import org.junit.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

public class KurentoBlockJUnit4ClassRunnerWithParameters extends
    BlockJUnit4ClassRunnerWithParameters {

  public KurentoBlockJUnit4ClassRunnerWithParameters(TestWithParameters test)
      throws InitializationError {
    super(test);
  }

  @Override
  protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
    Description description = describeChild(method);
    if (isIgnored(method)) {
      notifier.fireTestIgnored(description);
    } else {
      runLeaf2(methodBlock(method), description, notifier);
    }
  }

  protected final void runLeaf2(Statement statement, Description description, RunNotifier notifier) {

    // TODO Do not use Listeners to start services. Implement services
    // management here.

    EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
    eachNotifier.fireTestStarted();

    try {
      Exception exception = ((KurentoRunNotifier) notifier).getException();
      if (exception != null) {
        eachNotifier.addFailure(exception);
      } else {
        statement.evaluate();
      }
    } catch (AssumptionViolatedException e) {
      eachNotifier.addFailedAssumption(e);
    } catch (Throwable e) {
      eachNotifier.addFailure(e);
    } finally {
      eachNotifier.fireTestFinished();
    }
  }
}
