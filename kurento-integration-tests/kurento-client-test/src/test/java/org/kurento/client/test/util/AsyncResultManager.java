package org.kurento.client.test.util;

import org.kurento.client.Continuation;

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
				addError(cause);
			}
		};
	}
}
