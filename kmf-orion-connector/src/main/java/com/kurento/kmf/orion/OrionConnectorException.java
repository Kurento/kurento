/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package com.kurento.kmf.orion;

import com.kurento.kmf.common.exception.KurentoException;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
public class OrionConnectorException extends KurentoException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1981989759033830188L;

	/**
	 * default constructor.
	 */
	public OrionConnectorException() {
		// Default constructor
	}

	/**
	 * Constructs a new runtime exception with the specified detail message. The
	 * cause is not initialised, and may subsequently be initialised by a call
	 * to initCause.
	 * 
	 * @param msg
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 */
	public OrionConnectorException(final String msg) {
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
	public OrionConnectorException(final String msg, final Throwable throwable) {
		super(msg, throwable);
	}

	/**
	 * 
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public OrionConnectorException(final Throwable cause) {
		super(cause);
	}

}
