package org.kurento.test.lifecycle;

import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;
import org.junit.runners.parameterized.TestWithParameters;

public class KurentoBlockJUnit4ClassRunnerWithParametersFactory extends
    BlockJUnit4ClassRunnerWithParametersFactory {

  @Override
  public Runner createRunnerForTestWithParameters(TestWithParameters test)
      throws InitializationError {
    return new KurentoBlockJUnit4ClassRunnerWithParameters(test);
  }
}
