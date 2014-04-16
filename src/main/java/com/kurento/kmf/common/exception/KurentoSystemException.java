/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.common.exception;

/**
 * <p>
 * It's usage is intended for system-level exceptions. Usage is encouraged in
 * the following cases:
 * <ul>
 * <li>If the method encounters a system exception or error, but never for
 * business related errors.
 * <li>If the method performs an operation that results in a checked exception
 * that the bean method cannot recover.
 * <li>Any other unexpected error conditions.
 * </ul>
 * </p>
 * 
 * The original exception cause must be provided within the exception if it is
 * raised due to a previous exception.
 * 
 * This kind of exceptions are not checked and with CMT provoke a roll back at
 * the moment the are thrown.
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.1.1
 */
public class KurentoSystemException extends RuntimeException implements
		KurentoException {

	/**
	 * Default serial version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Internal error code of the exception.
	 */
	private int errorCode;

	@Override
	public int getErrorCode() {
		return errorCode;
	}

	@Override
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * default constructor.
	 */
	public KurentoSystemException() {
		// Default constructor
	}

	/**
	 * Constructs a new runtime exception with the specified detail message. The
	 * cause is not initialized, and may subsequently be initialized by a call
	 * to initCause.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 */
	public KurentoSystemException(final String msg) {
		super(msg);
	}

	/**
	 * 
	 * @param msg
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public KurentoSystemException(final String msg, final Throwable throwable) {
		super(msg, throwable);
	}

	/**
	 * 
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public KurentoSystemException(final Throwable cause) {
		super(cause);
	}

}
