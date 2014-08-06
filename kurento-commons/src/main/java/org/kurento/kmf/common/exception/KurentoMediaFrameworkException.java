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
package org.kurento.kmf.common.exception;

/**
 * @Deprecated Use the new {@link KurentoException}
 * @author Ivan Gracia (igracia@naevatec.com)
 *
 */
@Deprecated
public class KurentoMediaFrameworkException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private int code = 1; // Default error code

	public KurentoMediaFrameworkException() {
		super();
	}

	/**
	 * @param code
	 *            Error code
	 * @Deprecated Code is no longer used
	 */
	@Deprecated
	public KurentoMediaFrameworkException(int code) {
		super();
		this.code = code;
	}

	public KurentoMediaFrameworkException(String message) {
		super();
	}

	/**
	 * @param message
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 * @param code
	 *            Error code
	 * @Deprecated Code is no longer used
	 */
	@Deprecated
	public KurentoMediaFrameworkException(String message, int code) {
		super(message);
		this.code = code;
	}

	/**
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public KurentoMediaFrameworkException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 * @param code
	 *            Error code
	 * @Deprecated Code is no longer used
	 */
	@Deprecated
	public KurentoMediaFrameworkException(String message, Throwable cause,
			int code) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * 
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public KurentoMediaFrameworkException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 * @param code
	 *            Error code
	 * @Deprecated Code is no longer used
	 */
	@Deprecated
	public KurentoMediaFrameworkException(Throwable cause, int code) {
		super(cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

}
