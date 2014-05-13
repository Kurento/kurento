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
package com.kurento.kmf.thrift;


/**
 * Exception thrown when the Thrift Server, used to receive events from the
 * Media Server, can't be created or started.
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.3
 *
 */
public class ThriftServerException extends ThriftInterfaceException {

	private static final long serialVersionUID = -8258926012237744437L;

	public ThriftServerException(String message) {
		super(message);
	}

	public ThriftServerException(String message, Throwable cause) {
		super(message, cause);
	}
}
