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
			privateStringField = RunNotifier.class
					.getDeclaredField("listeners");
			privateStringField.setAccessible(true);
			this.childListeners = (List<RunListener>) privateStringField
					.get(notifier);

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception trying to access private field in RunListener class",
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

		abstract protected void notifyListener(RunListener each)
				throws Exception;
	}

	public void fireTestRunStarted(final Description description) {
		new SafeNotifier() {
			@Override
			protected void notifyListener(RunListener each) throws Exception {
				each.testRunStarted(description);
			}
		}.run();
	}

	public void fireTestRunFinished(final Result result) {
		new SafeNotifier() {
			@Override
			protected void notifyListener(RunListener each) throws Exception {
				each.testRunFinished(result);
			}
		}.run();
	}

	public void fireTestStarted(final Description description)
			throws StoppedByUserException {
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

	public void fireTestFailure(Failure failure) {
		fireTestFailures(childListeners, asList(failure));
	}

	private void fireTestFailures(List<RunListener> listeners,
			final List<Failure> failures) {
		if (!failures.isEmpty()) {
			new SafeNotifier(listeners) {
				@Override
				protected void notifyListener(RunListener listener)
						throws Exception {
					for (Failure each : failures) {
						listener.testFailure(each);
					}
				}
			}.run();
		}
	}

	public void fireTestAssumptionFailed(final Failure failure) {
		new SafeNotifier() {
			@Override
			protected void notifyListener(RunListener each) throws Exception {
				each.testAssumptionFailure(failure);
			}
		}.run();
	}

	public void fireTestIgnored(final Description description) {
		new SafeNotifier() {
			@Override
			protected void notifyListener(RunListener each) throws Exception {
				each.testIgnored(description);
			}
		}.run();
	}

	public void fireTestFinished(final Description description) {
		new SafeNotifier() {
			@Override
			protected void notifyListener(RunListener each) throws Exception {
				each.testFinished(description);
			}
		}.run();
	}

	public void pleaseStop() {
		pleaseStop = true;
	}

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
