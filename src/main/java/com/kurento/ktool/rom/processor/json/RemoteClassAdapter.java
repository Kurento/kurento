package com.kurento.ktool.rom.processor.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.kurento.ktool.rom.processor.model.Method;
import com.kurento.ktool.rom.processor.model.Property;
import com.kurento.ktool.rom.processor.model.RemoteClass;
import com.kurento.ktool.rom.processor.model.TypeRef;

public class RemoteClassAdapter implements JsonSerializer<RemoteClass>,
		JsonDeserializer<RemoteClass> {

	@Override
	public JsonElement serialize(RemoteClass src, Type typeOfSrc,
			JsonSerializationContext context) {

		JsonObject object = new JsonObject();

		if (src.getName() != null) {
			object.addProperty("name", src.getName());
		}

		if (src.getDoc() != null) {
			object.addProperty("doc", src.getDoc());
		}

		if (src.isAbstract()) {
			object.add("abstract", new JsonPrimitive(true));
		}

		if (src.getExtends() != null) {
			object.add("extends", context.serialize(src.getExtends()));
		}

		if (src.getConstructor() != null) {
			object.add("constructor", context.serialize(src.getConstructor()));
		}

		if (!src.getMethods().isEmpty()) {
			object.add("methods", context.serialize(src.getMethods()));
		}

		if (!src.getEvents().isEmpty()) {
			object.add("events", context.serialize(src.getEvents()));
		}

		return object;
	}

	@Override
	public RemoteClass deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		JsonObject object = (JsonObject) json;

		String name = null;
		String doc = null;
		boolean abstractValue = false;
		TypeRef extendsValue = null;
		Method constructor = null;
		List<Method> methods = new ArrayList<Method>();
		List<Property> properties = new ArrayList<Property>();
		List<TypeRef> events = new ArrayList<TypeRef>();

		if (object.get("name") != null) {
			name = object.get("name").getAsString();
		}

		if (object.get("doc") != null) {
			doc = object.get("doc").getAsString();
		}

		if (object.get("abstract") != null) {
			abstractValue = object.get("abstract").getAsBoolean();
		}

		if (object.get("extends") != null) {
			extendsValue = context.deserialize(object.get("extends"),
					TypeRef.class);
		}

		if (object.get("constructor") != null) {
			constructor = context.deserialize(object.get("constructor"),
					new TypeToken<Method>() {
					}.getType());
		}

		if (object.get("methods") != null) {
			methods = context.deserialize(object.get("methods"),
					new TypeToken<List<Method>>() {
					}.getType());
		}

		if (object.get("properties") != null) {
			properties = context.deserialize(object.get("properties"),
					new TypeToken<List<Property>>() {
					}.getType());
		}

		if (object.get("events") != null) {
			events = context.deserialize(object.get("events"),
					new TypeToken<List<TypeRef>>() {
					}.getType());
		}

		RemoteClass remoteClass = new RemoteClass(name, doc, extendsValue,
				constructor, methods, properties, events);
		remoteClass.setAbstract(abstractValue);
		return remoteClass;
	}

}
