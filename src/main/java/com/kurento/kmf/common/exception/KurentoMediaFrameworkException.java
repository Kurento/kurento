package com.kurento.kmf.common.exception;

import com.kurento.kmf.common.excption.internal.ExceptionUtils;

public class KurentoMediaFrameworkException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private int code = 1; // Default error code

	public KurentoMediaFrameworkException() {
		super();
	}

	public KurentoMediaFrameworkException(int code) {
		super();
		this.code = code;
	}

	public KurentoMediaFrameworkException(String message) {
		super();
	}

	public KurentoMediaFrameworkException(String message, int code) {
		super(message);
		this.code = code;
	}

	public KurentoMediaFrameworkException(String message, Throwable cause) {
		super(message, cause);
	}

	public KurentoMediaFrameworkException(String message, Throwable cause,
			int code) {
		super(message, cause);
		this.code = code;
	}

	public KurentoMediaFrameworkException(Throwable cause) {
		super(cause);
	}

	public KurentoMediaFrameworkException(Throwable cause, int code) {
		super(cause);
		this.code = code;
	}

	@Override
	public String getMessage() {
		String additionalMessage = super.getMessage() != null ? ". "
				+ super.getMessage() : "";
		return "Code " + code + ". " + ExceptionUtils.getErrorMessage(code)
				+ additionalMessage;
	}

	public int getCode() {
		return code;
	}

}
