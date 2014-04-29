package com.kurento.tool.rom.client;

import com.kurento.kmf.media.Continuation;

public abstract class DefaultContinuation<F> implements Continuation<F> {

	private Continuation<?> cont;

	public DefaultContinuation(Continuation<?> cont) {
		this.cont = cont;
	}

	@Override
	public abstract void onSuccess(F result);

	@Override
	public void onError(Throwable cause) {
		cont.onError(cause);
	}

}
