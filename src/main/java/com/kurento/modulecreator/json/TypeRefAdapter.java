package com.kurento.modulecreator.json;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.kurento.modulecreator.descriptor.TypeRef;

public class TypeRefAdapter implements JsonSerializer<TypeRef>,
		JsonDeserializer<TypeRef> {

	@Override
	public JsonElement serialize(TypeRef src, Type typeOfSrc,
			JsonSerializationContext context) {
		String name = src.getName();
		if (src.isList()) {
			name += "[]";
		}
		return new JsonPrimitive(name);
	}

	@Override
	public TypeRef deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		String typeRefString = json.getAsString();
		boolean list = false;
		if (typeRefString.endsWith("[]")) {
			typeRefString = typeRefString.substring(0,
					typeRefString.length() - 2);
			list = true;
		}

		return new TypeRef(typeRefString, list);
	}

}
