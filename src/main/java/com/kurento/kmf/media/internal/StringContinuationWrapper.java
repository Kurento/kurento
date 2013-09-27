package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.commands.internal.StringCommandResult;

public class StringContinuationWrapper implements
		Continuation<StringCommandResult> {

	private Continuation<String> wrappedContinuation;

	public StringContinuationWrapper(Continuation<String> continuation) {
		this.wrappedContinuation = continuation;
	}

	@Override
	public void onSuccess(StringCommandResult result) {
		this.wrappedContinuation.onSuccess(result.getResult());
	}

	@Override
	public void onError(Throwable cause) {
		this.wrappedContinuation.onError(cause);
	}

}
