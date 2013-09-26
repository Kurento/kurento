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
package com.kurento.kmf.common.exception;

import com.kurento.kmf.common.excption.internal.ExceptionUtils;

public class KurentoMediaFrameworkException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private int code = 1; // Default error code

	public KurentoMediaFrameworkException() {
		super();
	}

	public KurentoMediaFrameworkException(int code) {
		super();
		this.code = code;
	}

	public KurentoMediaFrameworkException(String message) {
		super();
	}

	public KurentoMediaFrameworkException(String message, int code) {
		super(message);
		this.code = code;
	}

	public KurentoMediaFrameworkException(String message, Throwable cause) {
		super(message, cause);
	}

	public KurentoMediaFrameworkException(String message, Throwable cause,
			int code) {
		super(message, cause);
		this.code = code;
	}

	public KurentoMediaFrameworkException(Throwable cause) {
		super(cause);
	}

	public KurentoMediaFrameworkException(Throwable cause, int code) {
		super(cause);
		this.code = code;
	}

	@Override
	public String getMessage() {
		String additionalMessage = super.getMessage() != null ? ". "
				+ super.getMessage() : "";
		return "Code " + code + ". " + ExceptionUtils.getErrorMessage(code)
				+ additionalMessage;
	}

	public int getCode() {
		return code;
	}

}
