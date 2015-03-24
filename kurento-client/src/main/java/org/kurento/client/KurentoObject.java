package org.kurento.client;

import java.util.concurrent.Executor;

public interface KurentoObject {

	boolean isCommited();

	void waitCommited() throws InterruptedException;

	void whenCommited(Continuation<?> continuation);

	void whenCommited(Continuation<?> continuation, Executor executor);

	/**
	 *
	 * Explicitly release a media object form memory. All of its children will
	 * also be released.
	 *
	 **/
	void release();

	/**
	 *
	 * Explicitly release a media object form memory. All of its children will
	 * also be released. Asynchronous call.
	 *
	 * @param continuation
	 *            {@link #onSuccess(void)} will be called when the actions
	 *            complete. {@link #onError} will be called if there is an
	 *            exception.
	 *
	 **/
	void release(Continuation<Void> continuation);

	void release(Transaction tx);
	
	String getId();

}
