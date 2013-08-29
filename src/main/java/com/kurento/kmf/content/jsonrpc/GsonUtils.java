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
