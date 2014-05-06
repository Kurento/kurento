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
package com.kurento.kmf.test;

/**
 * Properties manager (reading system properties with a default value).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class PropertiesManager {

	public static int getSystemProperty(String property, int defaultValue) {
		String systemValue = System.getProperty(property);
		return systemValue != null ? Integer.parseInt(systemValue)
				: defaultValue;
	}

	public static String getSystemProperty(String property, String defaultValue) {
		String systemValue = System.getProperty(property);
		return systemValue != null ? systemValue : defaultValue;
	}

	public static boolean getSystemProperty(String property,
			boolean defaultValue) {
		String systemValue = System.getProperty(property);
		return systemValue != null ? Boolean.getBoolean(systemValue)
				: defaultValue;
	}
}
