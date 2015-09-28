package org.kurento.modulecreator;

public class KurentoModuleCreatorException extends RuntimeException {

	private static final long serialVersionUID = -5373430051337208460L;

	public KurentoModuleCreatorException() {
	}

	public KurentoModuleCreatorException(String message) {
		super(message);
	}

	public KurentoModuleCreatorException(Throwable cause) {
		super(cause);
	}

	public KurentoModuleCreatorException(String message, Throwable cause) {
		super(message, cause);
	}

	public KurentoModuleCreatorException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
