package com.kurento.kmf.content.internal.jsonrpc;

import com.google.gson.Gson;

public class GsonUtils {
	private static Gson gson = new Gson();

	public static String toString(Object obj) {
		return gson.toJson(obj);
	}

	public static Gson getGson() {
		return gson;
	}
}
