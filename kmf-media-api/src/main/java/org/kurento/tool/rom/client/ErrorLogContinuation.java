package org.kurento.tool.rom.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kurento.kmf.media.Continuation;

public class ErrorLogContinuation<E> implements Continuation<E> {

	private static Logger LOG = LoggerFactory
			.getLogger(ErrorLogContinuation.class);
	private String message;

	public ErrorLogContinuation(String message) {
		this.message = message;
	}

	@Override
	public void onSuccess(E result) {
	}

	@Override
	public void onError(Throwable cause) {
		LOG.error(message, cause);
	}
}
