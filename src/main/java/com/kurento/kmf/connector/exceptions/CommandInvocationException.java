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

import com.kurento.kmf.exceptions.KurentoFrameworkApplicationException;
import com.kurento.kms.thrift.api.KmsMediaError;

/**
 * Exception to be used when the Kurento Media Server returns an error while the
 * client is invoking a command on a Media Object
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 * 
 */
public class CommandInvocationException extends
		KurentoFrameworkApplicationException {

	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param errorCode
	 */
	public CommandInvocationException(int errorCode) {
		super(errorCode);
	}

	/**
	 * 
	 * @param msg
	 * @param errorCode
	 */
	public CommandInvocationException(String msg, int errorCode) {
		super(msg, errorCode);
	}

	/**
	 * 
	 * @param msg
	 * @param cause
	 * @param errorCode
	 */
	public CommandInvocationException(String msg, Throwable cause, int errorCode) {
		super(msg, cause, errorCode);
	}

	/**
	 * 
	 * @param cause
	 * @param errorCode
	 */
	public CommandInvocationException(Throwable cause, int errorCode) {
		super(cause, errorCode);
	}

	/**
	 * Creates a new instance of this type of exception from the information
	 * contained in the {@link KmsMediaError} received, when a method is invoked
	 * through the Thrift interface with the media server.
	 * <p>
	 * The resulting exception uses the fields {@link KmsMediaError#description}
	 * and {@link KmsMediaError#errorCode}, since the type is supposed to be
	 * implied in the type of exception propagated.
	 * </p>
	 * 
	 * @param kmsError
	 * @return a new instance of the exception
	 */
	public static CommandInvocationException newFromKmsError(
			KmsMediaError kmsError) {
		return new CommandInvocationException(kmsError.description,
				kmsError.getErrorCode());
	}

}
