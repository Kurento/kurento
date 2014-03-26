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
	 * default constructor.
	 * 
	 * @param s
	 *            the message
	 */
	public KurentoSystemException(final String s) {
		super(s);
	}

	/**
	 * default constructor.
	 * 
	 * @param s
	 *            the message
	 * @param throwable
	 *            the error cause
	 */
	public KurentoSystemException(final String s, final Throwable throwable) {
		super(s, throwable);
	}

	/**
	 * default constructor.
	 * 
	 * @param throwable
	 *            the error cause
	 */
	public KurentoSystemException(final Throwable throwable) {
		super(throwable);
	}

}
