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
package org.kurento.jsonrpcconnector.internal.message;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ResponseError {

	/**
	 * Error status code.
	 */
	private Integer code;

	/**
	 * Error message.
	 */
	private String message;

	/**
	 * Error data.
	 */
	private JsonElement data;

	// TODO Improve the way errors are created from Exceptions
	public static ResponseError newFromException(Throwable e) {

		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));

		return new ResponseError(-1, e.getClass().getSimpleName() + ":"
				+ e.getMessage(), writer.toString());
	}

	// TODO Improve the way errors are created from Exceptions
	public static ResponseError newFromException(int requestId, Exception e) {

		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		return new ResponseError(requestId, e.getClass().getSimpleName() + ":"
				+ e.getMessage(), writer.toString());
	}

	/**
	 * Default constructor.
	 */
	public ResponseError() {
	}

	/**
	 * Parameterised constructor.
	 *
	 * @param code
	 *            Error status code
	 * @param message
	 *            Error message
	 * @param data
	 *            Error data
	 */
	public ResponseError(int code, String message, String data) {
		this.code = Integer.valueOf(code);
		this.message = message;
		if (data != null) {
			this.data = new JsonPrimitive(data);
		}
	}

	public ResponseError(int code, String message, JsonElement data) {
		this.code = Integer.valueOf(code);
		this.message = message;
		this.data = data;
	}

	public ResponseError(int code, String message) {
		this.code = Integer.valueOf(code);
		this.message = message;
	}

	/**
	 * Error status code accessor (getter).
	 *
	 * @return Error status code
	 */
	public int getCode() {
		return (code != null) ? code.intValue() : 0;
	}

	/**
	 * Error status code mutator (setter).
	 *
	 * @param code
	 *            Error status code
	 */
	void setCode(int code) {
		this.code = Integer.valueOf(code);
	}

	/**
	 * Error message accessor (getter).
	 *
	 * @return Error message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Error message mutator (setter).
	 *
	 * @param message
	 *            Error message
	 */
	void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Error data accessor (getter).
	 *
	 * @return Error data
	 */
	public String getData() {
		if (data instanceof JsonPrimitive) {
			return ((JsonPrimitive) data).getAsString();
		} else if (data != null) {
			return data.toString();
		} else {
			return null;
		}
	}

	/**
	 * Error data mutator (setter).
	 *
	 * @param data
	 *            Error data
	 */
	void setData(String data) {
		this.data = new JsonPrimitive(data);
	}

}