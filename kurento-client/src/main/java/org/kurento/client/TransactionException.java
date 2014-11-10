package org.kurento.client;

import org.kurento.commons.exception.KurentoException;

public class TransactionException extends KurentoException {

	private static final long serialVersionUID = 1876787972925582820L;

	/**
	 * default constructor.
	 */
	public TransactionException() {
		// Default constructor
	}

	/**
	 * Constructs a new runtime exception with the specified detail message. The
	 * cause is not initialized, and may subsequently be initialized by a call
	 * to initCause.
	 *
	 * @param msg
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 */
	public TransactionException(final String msg) {
		super(msg);
	}

	/**
	 *
	 * @param msg
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 * @param throwable
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public TransactionException(final String msg, final Throwable throwable) {
		super(msg, throwable);
	}

	/**
	 *
	 * @param throwable
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public TransactionException(final Throwable throwable) {
		super(throwable);
	}

}
