package org.kurento.client.internal.client;

import org.kurento.client.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DefaultContinuation<F> implements Continuation<F> {

	protected static final Logger log = LoggerFactory
			.getLogger(DefaultContinuation.class);
	private final Continuation<?> cont;

	public DefaultContinuation(Continuation<?> cont) {
		this.cont = cont;
	}

	@Override
	public abstract void onSuccess(F result) throws Exception;

	@Override
	public void onError(Throwable cause) {
		try {
			cont.onError(cause);
		} catch (Exception e) {
			log.warn(
					"[Continuation] error invoking onError implemented by client",
					e);
		}
	}

}
