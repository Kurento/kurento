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
package com.kurento.kmf.common.exception.internal;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Exception utility singleton class; it reads Kurento exceptions properties
 * using the default locale. If specific locale need to be used, new exception
 * definition should be created (e.g. kurento-exceptions_en.properties), and
 * then the locale should be set on the singleton:
 * 
 * <pre>
 * ExceptionsUtil.setLocale(Locale.ENGLISH);
 * </pre>
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class ExceptionUtils {
	/**
	 * Kurento exceptions properties name.
	 */
	private static final String EXCEPTIONS = "kurento-error-codes";

	/**
	 * Separator within the properties file. Notation: <br/>
	 * 
	 * <pre>
	 * errorCode=error description;httpErrorCode;jsonRpcErrorCode
	 * </pre>
	 */
	private static final String SEPARATOR = ";";

	/**
	 * Singleton.
	 */
	private static ExceptionUtils singleton = null;

	/**
	 * Object for I18N.
	 */
	private Locale locale;

	/**
	 * Constructor; it gets the default locale.
	 */
	private ExceptionUtils() {
		this.locale = Locale.getDefault();
	}

	/**
	 * Singleton accessor (getter).
	 * 
	 * @return ExceptionsUtil singleton
	 */
	public static ExceptionUtils getSingleton() {
		if (singleton == null) {
			singleton = new ExceptionUtils();
		}
		return singleton;
	}

	/**
	 * Reads exceptions properties file by key and get the error message
	 * associated to a error code.
	 * 
	 * @param errorCode
	 *            Error code
	 * @return Error message
	 */
	public static String getErrorMessage(int errorCode) {
		// TODO: manage exceptions here in case bundle cannot load or key is not
		// found
		return ResourceBundle.getBundle(EXCEPTIONS, getLocale())
				.getString(String.valueOf(errorCode)).split(SEPARATOR)[0];
	}

	/**
	 * Reads exceptions properties file by key and get the HTTP code associated
	 * to a error code.
	 * 
	 * @param errorCode
	 *            Error code
	 * @return HTTP error code
	 */
	public static int getHttpErrorCode(int errorCode) {
		// TODO: manage exceptions here in case bundle cannot load or key is not
		// found
		String result = ResourceBundle.getBundle(EXCEPTIONS, getLocale())
				.getString(String.valueOf(errorCode)).split(SEPARATOR)[1];
		// TODO: manage exception here in case result is not an int
		return Integer.parseInt(result);
	}

	/**
	 * Reads exceptions properties file by key and get the JSON code associated
	 * to a error code.
	 * 
	 * @param errorCode
	 *            Error code
	 * @return JSON error code
	 */
	public static int getJsonErrorCode(int errorCode) {
		// TODO: manage exceptions here in case bundle cannot load or key is not
		// found
		String result = ResourceBundle.getBundle(EXCEPTIONS, getLocale())
				.getString(String.valueOf(errorCode)).split(SEPARATOR)[2];
		// TODO: manage exception here in case result is not an int
		return Integer.parseInt(result);
	}

	/**
	 * Locale accessor (getter).
	 * 
	 * @return Locale
	 */
	public static Locale getLocale() {
		return ExceptionUtils.getSingleton().locale;
	}

	/**
	 * Locale mutator (setter).
	 * 
	 * @param locale
	 *            Locale
	 */
	public static void setLocale(String locale) {
		ExceptionUtils.getSingleton().locale = new Locale(locale);
	}

	/**
	 * Locale mutator (setter).
	 * 
	 * @param locale
	 *            Locale
	 */
	public static void setLocale(Locale locale) {
		ExceptionUtils.getSingleton().locale = locale;
	}

}
