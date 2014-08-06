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
package org.kurento.client.internal.server;

/**
 * Exception that represents an error in the JSON RPC protocol (i.e. malformed
 * commands and so on)
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.1
 * 
 */
public class ProtocolException extends MediaServerException {

	private static final long serialVersionUID = -4925041543188451274L;

	public ProtocolException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProtocolException(String message) {
		super(message);
	}

	public ProtocolException(Throwable cause) {
		super(cause);
	}

}
