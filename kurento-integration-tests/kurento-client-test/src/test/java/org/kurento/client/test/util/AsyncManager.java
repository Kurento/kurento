package org.kurento.client.test.util;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CountDownLatch;

import org.junit.Assert;

public class AsyncManager<E> {

	public static final long TIMEOUT_SECONDS = 20;

	protected CountDownLatch latch = new CountDownLatch(1);

	protected E result;
	protected Throwable cause;

	protected String message;

	public AsyncManager(String message) {
		this.message = message;
	}

	public void addResult(E result) {
		this.result = result;
		latch.countDown();
	}

	protected void addError(Throwable cause) {
		this.cause = cause;
		latch.countDown();
	}

	public E waitForResult() {

		if (this.cause != null) {
			throw new RuntimeException(cause);
		}

		try {

			if (latch.await(TIMEOUT_SECONDS, SECONDS)) {
				return result;
			} else {
				Assert.fail("Timeout of " + TIMEOUT_SECONDS + "s waiting for '"
						+ message + "'");
				return null;
			}

		} catch (InterruptedException e) {
			Assert.fail("InterruptedException waiting for '" + message + "'");
			return null;
		}
	}

}
