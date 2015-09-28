package org.kurento.modulecreator.json;

import java.lang.reflect.Type;

import org.kurento.modulecreator.definition.TypeRef;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class TypeRefAdapter implements JsonSerializer<TypeRef>, JsonDeserializer<TypeRef> {

	@Override
	public JsonElement serialize(TypeRef src, Type typeOfSrc, JsonSerializationContext context) {
		String name = src.getName();
		if (src.isList()) {
			name += "[]";
		} else if (src.isMap()) {
			name += "<>";
		}
		return new JsonPrimitive(name);
	}

	@Override
	public TypeRef deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		String typeRefString = json.getAsString();

		return TypeRef.parseFromJson(typeRefString);
	}

}
