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

/**
 * Assertion class to validate parameters within Content API.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class Assert {

	/**
	 * Asserts that an object is not null; if it is null, a RuntimeException is
	 * raised: ContentApiException or ContentApiUserException, depending the
	 * error code (ContentApiException = 10000..19999,
	 * ContentApiUserException=10000..29999).
	 * 
	 * @see ContentApiException
	 * @see ContentApiUserException
	 * @param object
	 *            Object to be checked whether or not is null
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            object is null
	 */
	public static void notNull(Object object, int errorCode) {
		notNull(object, "", errorCode);
	}

	/**
	 * Asserts that an object is not null; if it is null, a RuntimeException is
	 * raised: ContentApiException or ContentApiUserException, depending the
	 * error code (ContentApiException = 10000..19999,
	 * ContentApiUserException=10000..29999); in addition, a message passed as
	 * parameter is appended at the end of the error description.
	 * 
	 * @see ContentApiException
	 * @see ContentApiUserException
	 * @param object
	 *            Object to be checked whether or not is null
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            object is null
	 * @param message
	 *            Message to be appended at the end of the description error
	 */
	public static void notNull(Object object, String message, int errorCode) {
		if (object == null) {
			throw new KurentoMediaFrameworkException(message, errorCode);
		}
	}

	/**
	 * Asserts whether or not a condition is met; if not, a RuntimeException is
	 * raised: ContentApiException or ContentApiUserException, depending the
	 * error code (ContentApiException = 10000..19999,
	 * ContentApiUserException=10000..29999); in addition, a message passed as
	 * parameter is appended at the end of the error description.
	 * 
	 * @param condition
	 *            Boolean condition to be checked
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            condition is not met
	 * @param errorMessageAppend
	 *            Message to be appended at the end of the description error
	 */
	public static void isTrue(boolean condition, int errorCode) {
		isTrue(condition, "", errorCode);
	}

	/**
	 * Asserts whether or not a condition is met; if not, a RuntimeException is
	 * raised: ContentApiException or ContentApiUserException, depending the
	 * error code (ContentApiException = 10000..19999,
	 * ContentApiUserException=10000..29999).
	 * 
	 * @param condition
	 *            Boolean condition to be checked
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            condition is not met
	 */
	public static void isTrue(boolean condition, String message, int errorCode) {
		if (!condition) {
			throw new KurentoMediaFrameworkException(message, errorCode);
		}
	}
}
