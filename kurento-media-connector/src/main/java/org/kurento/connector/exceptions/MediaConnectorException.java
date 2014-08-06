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
package org.kurento.connector.exceptions;

import org.kurento.common.exception.KurentoException;

/**
 * Exception to be used when an event that has been received form the media
 * server can't be propagated to the client
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.3
 *
 */
public class MediaConnectorException extends KurentoException {

	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public MediaConnectorException() {
		super();
	}

	/**
	 *
	 * @param msg
	 */
	public MediaConnectorException(String msg) {
		super(msg);
	}

	/**
	 *
	 * @param msg
	 * @param cause
	 */
	public MediaConnectorException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 *
	 * @param cause
	 */
	public MediaConnectorException(Throwable cause) {
		super(cause);
	}

}
