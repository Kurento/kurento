package com.kurento.kms.idl.json;

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
import com.kurento.kms.idl.model.Doc;
import com.kurento.kms.idl.model.Method;
import com.kurento.kms.idl.model.RemoteClass;
import com.kurento.kms.idl.model.TypeRef;

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
			object.addProperty("doc", src.getDoc().getDoc());
		}

		if (src.isAbstract()) {
			object.add("abstract", new JsonPrimitive(true));
		}

		if (src.getExtends() != null) {
			object.add("extends", context.serialize(src.getExtends()));
		}

		if (!src.getConstructors().isEmpty()) {
			object.add("constructors", context.serialize(src.getConstructors()));
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
		Doc doc = null;
		boolean abstractValue = false;
		TypeRef extendsValue = null;
		List<Method> constructors = new ArrayList<Method>();
		List<Method> methods = new ArrayList<Method>();
		List<TypeRef> events = new ArrayList<TypeRef>();

		if (object.get("name") != null) {
			name = object.get("name").getAsString();
		}
		
		if (object.get("doc") != null) {
			doc = new Doc(object.get("doc").getAsString());
		}

		if (object.get("abstract") != null) {
			abstractValue = object.get("abstract").getAsBoolean();
		}

		if (object.get("extends") != null) {
			extendsValue = context.deserialize(object.get("extends"),
					TypeRef.class);
		}

		if (object.get("constructors") != null) {
			constructors = context.deserialize(object.get("constructors"),
					new TypeToken<List<Method>>() {
					}.getType());
		}

		if (object.get("methods") != null) {
			methods = context.deserialize(object.get("methods"),
					new TypeToken<List<Method>>() {
					}.getType());
		}

		if (object.get("events") != null) {
			events = context.deserialize(object.get("events"),
					new TypeToken<List<TypeRef>>() {
					}.getType());
		}

		RemoteClass remoteClass = new RemoteClass(name, doc, extendsValue, constructors, methods, events);
		remoteClass.setAbstract(abstractValue);
		return remoteClass;
	}

}
