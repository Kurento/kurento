package com.kurento.modulecreator.json;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.kurento.modulecreator.descriptor.DataItem;

public class DataItemAdapter implements JsonSerializer<DataItem> {

	@Override
	public JsonElement serialize(DataItem src, Type typeOfSrc,
			JsonSerializationContext context) {

		JsonObject object = new JsonObject();

		if (src.getName() != null) {
			object.add("name", context.serialize(src.getName()));
		}

		if (src.getDoc() != null) {
			object.addProperty("doc", src.getDoc());
		}

		if (src.getType() != null) {
			object.add("type", context.serialize(src.getType()));
		}

		if (src.isOptional()) {
			object.addProperty("optional", src.isOptional());
			if (src.getDefaultValue() != null) {
				object.add("defaultValue", src.getDefaultValue());
			}
		}

		return object;
	}
}
