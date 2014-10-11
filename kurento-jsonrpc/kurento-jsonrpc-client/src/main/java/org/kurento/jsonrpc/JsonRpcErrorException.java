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
package org.kurento.jsonrpc;

import org.kurento.jsonrpc.message.ResponseError;

import com.google.gson.JsonElement;

public class JsonRpcErrorException extends JsonRpcException {

	private static final long serialVersionUID = 1584953670536766280L;

	private final ResponseError error;

	public JsonRpcErrorException(int code, String message) {
		this(code, message, null);
	}

	public JsonRpcErrorException(int code, String message, JsonElement data) {
		this(new ResponseError(code, message, data));
	}

	public JsonRpcErrorException(ResponseError error) {
		super(createExceptionMessage(error));
		this.error = error;
	}

	private static String createExceptionMessage(ResponseError error) {
		return error.getMessage()
				+ ((error.getData() != null) ? (". Data: " + error.getData())
						: "");
	}

	public ResponseError getError() {
		return error;
	}

	public String getData() {
		return error.getData();
	}

	public int getCode() {
		return error.getCode();
	}

}
