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
package org.kurento.common.exception;

/**
 * Assertion class to validate parameters within Content API.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class Assert {

	/**
	 * Asserts that an object is not null; if it is null, a
	 * KurentoMediaFrameworkException is thrown.
	 * 
	 * @param object
	 *            Object to be checked whether or not is null
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            object is null
	 */
	@Deprecated
	public static void notNull(Object object, int errorCode) {
		notNull(object, "", errorCode);
	}

	/**
	 * Asserts that an object is not null; if it is null, a
	 * KurentoMediaFrameworkException is thrown.
	 * 
	 * @param object
	 *            Object to be checked whether or not is null
	 */
	public static void notNull(Object object) {
		notNull(object, "");
	}

	/**
	 * Asserts that an object is not null; if it is null, a
	 * KurentoMediaFrameworkException is thrown. In addition, a message passed
	 * as parameter is appended at the end of the error description.
	 * 
	 * @param object
	 *            Object to be checked whether or not is null
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            object is null
	 * @param message
	 *            Message to be appended at the end of the description error
	 */
	@Deprecated
	public static void notNull(Object object, String message, int errorCode) {
		if (object == null) {
			throw new KurentoException(message + ' ' + errorCode);
		}
	}

	/**
	 * Asserts that an object is not null; if it is null, a
	 * KurentoMediaFrameworkException is thrown. In addition, a message passed
	 * as parameter is appended at the end of the error description.
	 * 
	 * @param object
	 *            Object to be checked whether or not is null
	 * @param message
	 *            Message to be appended at the end of the description error
	 */
	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new KurentoException(message);
		}
	}

	/**
	 * Asserts whether or not a condition is met; if not, a
	 * KurentoMediaFrameworkException is thrown. In addition, a message passed
	 * as parameter is appended at the end of the error description.
	 * 
	 * @param condition
	 *            Boolean condition to be checked
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            condition is not met
	 */
	@Deprecated
	public static void isTrue(boolean condition, int errorCode) {
		isTrue(condition, "", errorCode);
	}

	/**
	 * Asserts whether or not a condition is met; if not, a
	 * KurentoMediaFrameworkException is thrown.
	 * 
	 * @param condition
	 *            Boolean condition to be checked
	 * @param message
	 * @param errorCode
	 *            Error code which determines the exception to be raise if the
	 *            condition is not met
	 */
	@Deprecated
	public static void isTrue(boolean condition, String message, int errorCode) {
		if (!condition) {
			throw new KurentoException(message + ' ' + errorCode);
		}
	}
}
