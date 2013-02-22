/*
 * Kurento Commons MSControl: Simplified Media Control API for the Java Platform based on jsr309
 * Copyright (C) 2011  Tikal Technologies
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kurento.kmf.media;

/**
 * General purpose exception.
 */
public class MediaException extends Exception {

	private static final long serialVersionUID = 5114447844981856910L;

	/**
	 * Constructs a MsControlException with the specified detail message
	 * 
	 * @param message
	 *            the detail message
	 */
	public MediaException(String message) {
		super(message);
	}

	/**
	 * Constructs a MsControlException with its origin and the specified detail
	 * message
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 */
	public MediaException(String message, Throwable cause) {
		super(message, cause);
	}

}
