package org.kurento.client;

public class TransactionRollbackException extends TransactionException {

	private static final long serialVersionUID = 151845936439259253L;

	public TransactionRollbackException() {
	}

	public TransactionRollbackException(String msg) {
		super(msg);
	}

	public TransactionRollbackException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public TransactionRollbackException(Throwable throwable) {
		super(throwable);
	}

	public TransactionExecutionException getKurentoServerException() {
		Throwable cause = getCause();
		if (cause instanceof TransactionExecutionException) {
			return (TransactionExecutionException) cause;
		} else {
			return null;
		}
	}

	public boolean isUserRollback() {
		return getCause() == null;
	}
}
