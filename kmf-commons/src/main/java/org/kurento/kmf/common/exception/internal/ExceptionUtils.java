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
package org.kurento.kmf.common.exception.internal;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory
			.getLogger(ExceptionUtils.class);

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
		String message = readBundleString(1, 0); // Default error message
		try {
			message = readBundleString(errorCode, 0);
		} catch (MissingResourceException e) {
			log.warn("Especific description for error code {} not found",
						errorCode);
		}
		return message;
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
		int result = Integer.parseInt(readBundleString(1, 1)); // Default HTTP
																// error code
		String httpErrorCode = "";
		try {
			httpErrorCode = readBundleString(errorCode, 1);
			result = Integer.parseInt(httpErrorCode);
		} catch (MissingResourceException e1) {
			log.warn("Especific description for error code {} not found", 
					errorCode);
		} catch (NumberFormatException e2) {
			log.warn("Error parsing HTTP Error Code {}", httpErrorCode);
		}
		return result;
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
		int result = Integer.parseInt(readBundleString(1, 2)); // Default JSON
																// error code
		String jsonErrorCode = "";
		try {
			jsonErrorCode = readBundleString(errorCode, 2);
			result = Integer.parseInt(jsonErrorCode);
		} catch (MissingResourceException e1) {
			log.warn("Especific description for error code {}  not found", 
					errorCode);
		} catch (NumberFormatException e2) {
			log.warn("Error parsing JSON Error Code {}", jsonErrorCode);
		}
		return result;
	}

	/**
	 * Read error code from resource bundle (stored as a properties file), and
	 * returns the String depending of the position passed as argument. The
	 * notation of the properties files is:
	 * 
	 * <pre>
	 * errorCode=error description;httpErrorCode;jsonRpcErrorCode
	 * </pre>
	 * 
	 * Therefore, the position 0 is for description, 1 for httpErrorCode, and 2
	 * for jsonRpcErrorCode.
	 * 
	 * @param errorCode
	 *            Error code
	 * @param position
	 *            0, 1, or 2, depending the value to be read from properties
	 *            (error description, HTTP error code, or JSON error code
	 *            respectively).
	 * @return Value from properties
	 */
	private static String readBundleString(int errorCode, int position) {
		return ResourceBundle.getBundle(EXCEPTIONS, getLocale())
				.getString(String.valueOf(errorCode)).split(SEPARATOR)[position];
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
