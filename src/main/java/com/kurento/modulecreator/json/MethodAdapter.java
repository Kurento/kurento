package com.kurento.modulecreator.json;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.kurento.modulecreator.definition.Method;

public class MethodAdapter implements JsonSerializer<Method> {

	@Override
	public JsonElement serialize(Method src, Type typeOfSrc,
			JsonSerializationContext context) {

		JsonObject object = new JsonObject();

		if (src.getName() != null) {
			object.addProperty("name", src.getName());
		}

		if (src.getParams() != null) {
			object.add("params", context.serialize(src.getParams()));
		}

		if (src.getReturn() != null) {
			object.add("return", context.serialize(src.getReturn()));
		}

		return object;
	}

}
