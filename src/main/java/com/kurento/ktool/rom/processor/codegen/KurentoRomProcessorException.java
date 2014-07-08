package com.kurento.ktool.rom.processor.codegen;

public class KurentoRomProcessorException extends RuntimeException {

	private static final long serialVersionUID = -5373430051337208460L;

	public KurentoRomProcessorException() {
	}

	public KurentoRomProcessorException(String message) {
		super(message);
	}

	public KurentoRomProcessorException(Throwable cause) {
		super(cause);
	}

	public KurentoRomProcessorException(String message, Throwable cause) {
		super(message, cause);
	}

	public KurentoRomProcessorException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
