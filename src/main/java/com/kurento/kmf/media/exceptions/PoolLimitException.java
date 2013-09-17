package com.kurento.kmf.media.exceptions;

public class PoolLimitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	// TODO once it is decided how to control the pool, remove this exception if
	// it's not needed anymore
	// TODO put it in an internal package
	public PoolLimitException(String message) {
		super(message);
	}
}
