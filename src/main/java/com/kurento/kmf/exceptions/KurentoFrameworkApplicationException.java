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
package com.kurento.kmf.exceptions;

/**
 * <p>
 * It's usage is intended for application-level exceptions. This kind of
 * exceptions are intended for interacting with the client. Therefore, they
 * should just be used when the client can manage the exception flow, and it
 * provides useful information. For other kind of situations
 * {@link com.kurento.kmf.exceptions.KurentoFrameworkSystemException
 * [KurentoFrameworkSystemException]} must be used.
 * </p>
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 */
// TODO how to set rollback=false here?
public abstract class KurentoFrameworkApplicationException extends Exception
		implements KurentoFrameworkException {

	/**
	 * Default serial ID
	 */
	protected static final long serialVersionUID = 1L;

	/**
	 * Internal error code of the exception.
	 */
	// TODO errorCode maybe should have a default value for each project or type
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
	 * Constructs a new KurentoFrameworkApplicationException with null as its
	 * detail message.
	 * 
	 * @param errorCode
	 */
	public KurentoFrameworkApplicationException(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Constructs a new KurentoFrameworkApplicationException with the specified
	 * detail message.
	 * 
	 * @param msg
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the Throwable.getMessage() method.
	 * @param errorCode
	 */
	public KurentoFrameworkApplicationException(String msg, int errorCode) {
		super(msg);
		this.errorCode = errorCode;
	}

	/**
	 * Constructs a new KurentoFrameworkApplicationException with the specified
	 * detail message and cause.
	 * 
	 * @param msg
	 *            the detail message
	 * @param cause
	 *            the cause. A null value is permitted, and indicates that the
	 *            cause is nonexistent or unknown.
	 * @param errorCode
	 */
	public KurentoFrameworkApplicationException(String msg, Throwable cause,
			int errorCode) {
		super(msg, cause);
		this.errorCode = errorCode;
	}

	/**
	 * Constructs a new KurentoFrameworkApplicationException with the specified
	 * cause and a detail message.
	 * 
	 * @param cause
	 *            the cause. A null value is permitted, and indicates that the
	 *            cause is nonexistent or unknown.
	 * @param errorCode
	 */
	public KurentoFrameworkApplicationException(Throwable cause, int errorCode) {
		super(cause);
		this.errorCode = errorCode;
	}
}