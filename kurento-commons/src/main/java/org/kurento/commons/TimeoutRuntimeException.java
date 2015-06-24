package org.kurento.commons;

public class TimeoutRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -2273855137081386476L;

	public TimeoutRuntimeException() {
	}

	public TimeoutRuntimeException(String message) {
		super(message);
	}

	public TimeoutRuntimeException(Throwable cause) {
		super(cause);
	}

	public TimeoutRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public TimeoutRuntimeException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
