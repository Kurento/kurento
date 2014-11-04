package org.kurento.client;

public class TransactionNotExecutedException extends RuntimeException {

	private static final long serialVersionUID = 3270649817818849622L;

	public TransactionNotExecutedException() {
	}

	public TransactionNotExecutedException(String message) {
		super(message);
	}

}
