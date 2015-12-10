package org.kurento.client;

public class TransactionNotCommitedException extends TransactionException {

	private static final long serialVersionUID = 3270649817818849622L;

	public TransactionNotCommitedException() {
	}

	public TransactionNotCommitedException(String message) {
		super(message);
	}

}
