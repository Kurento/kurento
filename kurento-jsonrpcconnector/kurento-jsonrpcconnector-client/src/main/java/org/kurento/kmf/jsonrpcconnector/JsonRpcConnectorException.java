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
package org.kurento.kmf.jsonrpcconnector;

import org.kurento.kmf.common.exception.KurentoException;

/**
 * This is a general exception used in the JsonRpcConnector package.
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.3
 */
public class JsonRpcConnectorException extends KurentoException {

	private static final long serialVersionUID = -9166377169939591329L;

	public JsonRpcConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

	public JsonRpcConnectorException(String message) {
		super(message);
	}

	public JsonRpcConnectorException(Throwable cause) {
		super(cause);
	}

}
