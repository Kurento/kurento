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
package com.kurento.kmf.connector.exceptions;

import com.kurento.kmf.common.exception.KurentoException;

/**
 * Exception to be used when a command cannot be executed due to a Thrift error.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 * 
 */
public class ThriftInvocationException extends KurentoException {

	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public ThriftInvocationException() {
		super();
	}

	/**
	 * 
	 * @param msg
	 */
	public ThriftInvocationException(String msg) {
		super(msg);
	}

	/**
	 * 
	 * @param msg
	 * @param cause
	 */
	public ThriftInvocationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * 
	 * @param cause
	 */
	public ThriftInvocationException(Throwable cause) {
		super(cause);
	}

}
