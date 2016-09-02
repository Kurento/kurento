/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

import static java.util.Arrays.asList;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

public class KurentoRunNotifier extends RunNotifier {

  private List<RunListener> childListeners = new CopyOnWriteArrayList<RunListener>();
  private volatile boolean pleaseStop = false;
  private Exception exception;

  @SuppressWarnings("unchecked")
  public KurentoRunNotifier(RunNotifier notifier) {

    Field privateStringField;
    try {
      privateStringField = RunNotifier.class.getDeclaredField("listeners");
      privateStringField.setAccessible(true);
      this.childListeners = (List<RunListener>) privateStringField.get(notifier);

    } catch (Exception e) {
      throw new RuntimeException("Exception trying to access private field in RunListener class",
          e);
    }
  }

  private abstract class SafeNotifier {
    private final List<RunListener> currentListeners;

    SafeNotifier() {
      this(childListeners);
    }

    SafeNotifier(List<RunListener> currentListeners) {
      this.currentListeners = currentListeners;
    }

    void run() {
      for (RunListener listener : currentListeners) {
        try {
          notifyListener(listener);
        } catch (Exception e) {
          exception = e;
          break;
        }
      }
    }

    protected abstract void notifyListener(RunListener each) throws Exception;
  }

  @Override
  public void fireTestRunStarted(final Description description) {
    new SafeNotifier() {
      @Override
      protected void notifyListener(RunListener each) throws Exception {
        each.testRunStarted(description);
      }
    }.run();
  }

  @Override
  public void fireTestRunFinished(final Result result) {
    new SafeNotifier() {
      @Override
      protected void notifyListener(RunListener each) throws Exception {
        each.testRunFinished(result);
      }
    }.run();
  }

  @Override
  public void fireTestStarted(final Description description) throws StoppedByUserException {
    if (pleaseStop) {
      throw new StoppedByUserException();
    }
    new SafeNotifier() {
      @Override
      protected void notifyListener(RunListener each) throws Exception {
        each.testStarted(description);
      }
    }.run();
  }

  @Override
  public void fireTestFailure(Failure failure) {
    fireTestFailures(childListeners, asList(failure));
  }

  private void fireTestFailures(List<RunListener> listeners, final List<Failure> failures) {
    if (!failures.isEmpty()) {
      new SafeNotifier(listeners) {
        @Override
        protected void notifyListener(RunListener listener) throws Exception {
          for (Failure each : failures) {
            listener.testFailure(each);
          }
        }
      }.run();
    }
  }

  @Override
  public void fireTestAssumptionFailed(final Failure failure) {
    new SafeNotifier() {
      @Override
      protected void notifyListener(RunListener each) throws Exception {
        each.testAssumptionFailure(failure);
      }
    }.run();
  }

  @Override
  public void fireTestIgnored(final Description description) {
    new SafeNotifier() {
      @Override
      protected void notifyListener(RunListener each) throws Exception {
        each.testIgnored(description);
      }
    }.run();
  }

  @Override
  public void fireTestFinished(final Description description) {
    new SafeNotifier() {
      @Override
      protected void notifyListener(RunListener each) throws Exception {
        each.testFinished(description);
        exception = null;
      }
    }.run();
  }

  @Override
  public void pleaseStop() {
    pleaseStop = true;
  }

  @Override
  public void addFirstListener(RunListener listener) {
    if (listener == null) {
      throw new NullPointerException("Cannot add a null listener");
    }
    childListeners.add(0, listener);
  }

  public Exception getException() {
    return exception;
  }
}
