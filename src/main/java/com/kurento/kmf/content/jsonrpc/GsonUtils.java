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
package com.kurento.kmf.content.jsonrpc;

import com.google.gson.Gson;

/**
 * 
 * Gson/JSON utilities; used to serialize Java object to JSON (as String).
 * 
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @version 1.0.0
 */
public class GsonUtils {

	/**
	 * Static instance of Gson object.
	 */
	private static Gson gson = new Gson();

	/**
	 * Serialize Java object to JSON (as String).
	 * 
	 * @param obj
	 *            Java Object representing a JSON message to be serialized
	 * @return Serialized JSON message (as String)
	 */
	public static String toString(Object obj) {
		return gson.toJson(obj);
	}

	/**
	 * Gson object accessor (getter).
	 * 
	 * @return son object
	 */
	public static Gson getGson() {
		return gson;
	}
}
