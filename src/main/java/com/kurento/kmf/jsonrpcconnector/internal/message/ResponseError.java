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
package com.kurento.kmf.jsonrpcconnector.internal.message;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ResponseError {

	/**
	 * Error status code.
	 */
	private int code;

	/**
	 * Error message.
	 */
	private String message;

	/**
	 * Error data.
	 */
	private String data;

	// TODO Improve the way errors are created from Exceptions
	public static ResponseError newFromException(Exception e) {

		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));

		return new ResponseError(-1, e.getClass().getSimpleName() + ":"
				+ e.getMessage(), writer.toString());
	}

	/**
	 * Default constructor.
	 */
	public ResponseError() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param code
	 *            Error status code
	 * @param message
	 *            Error message
	 * @param data
	 *            Error data
	 */
	public ResponseError(int code, String message, String data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	/**
	 * Error status code accessor (getter).
	 * 
	 * @return Error status code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Error status code mutator (setter).
	 * 
	 * @param code
	 *            Error status code
	 */
	void setCode(int code) {
		this.code = code;
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
		return data;
	}

	/**
	 * Error data mutator (setter).
	 * 
	 * @param data
	 *            Error data
	 */
	void setData(String data) {
		this.data = data;
	}

}