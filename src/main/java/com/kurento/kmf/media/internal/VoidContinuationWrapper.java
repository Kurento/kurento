package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.commands.internal.VoidCommandResult;

public class VoidContinuationWrapper implements Continuation<VoidCommandResult> {

	private Continuation<Void> wrappedContinuation;

	public VoidContinuationWrapper(Continuation<Void> continuation) {
		this.wrappedContinuation = continuation;
	}

	@Override
	public void onSuccess(VoidCommandResult result) {
		this.wrappedContinuation.onSuccess(null);
	}

	@Override
	public void onError(Throwable cause) {
		this.wrappedContinuation.onError(cause);
	}

}
