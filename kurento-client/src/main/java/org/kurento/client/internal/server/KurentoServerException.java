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

import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.message.ResponseError;

/**
 * This exception represents errors that take place in Kurento Server, while
 * operating with pipelines and media elements
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.1
 *
 */
public class KurentoServerException extends KurentoException {

	private static final long serialVersionUID = -4925041543188451274L;

	private ResponseError error;

	protected KurentoServerException(String message, ResponseError error) {
		super(message);
		this.error = error;
	}

	public KurentoServerException(ResponseError error) {
		super(error.getCompleteMessage());
		this.error = error;
	}

	public String getServerMessage() {
		return error.getMessage();
	}

	public String getData() {
		return error.getData();
	}

	public String getErrorType() {
		return error.getType();
	}

	public int getCode() {
		return error.getCode();
	}

	public ResponseError getError() {
		return error;
	}
}
