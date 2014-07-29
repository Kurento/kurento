package com.kurento.kmf.media.test.base;

import org.junit.Assert;

import com.kurento.kmf.media.Continuation;

public class AsyncResultManager<E> extends AsyncManager<E> {

	public AsyncResultManager(String message) {
		super(message);
	}

	public Continuation<E> getContinuation() {
		return new Continuation<E>() {

			@Override
			public void onSuccess(E result) throws Exception {
				addResult(result);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				Assert.fail("Exception while '" + message + "': "
						+ cause.getClass().getName() + " - "
						+ cause.getMessage());
			}
		};
	}

}
