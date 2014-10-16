package org.kurento.commons;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.kurento.commons.PropertiesManager.PropertyHolder;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

public class ConfigFilePropertyHolder implements PropertyHolder {

	private static final Gson gson = new GsonBuilder().create();

	private JsonObject configFile;

	public static void configurePropertiesFromConfigFile(Path configFilePath)
			throws JsonSyntaxException, JsonIOException, IOException {

		Preconditions.checkNotNull(configFilePath,
				"configFilePath paramter must be not null.");

		JsonReader reader = new JsonReader(Files.newBufferedReader(
				configFilePath, StandardCharsets.UTF_8));
		reader.setLenient(true);

		JsonObject configFile = gson.fromJson(reader, JsonObject.class);

		PropertiesManager.setPropertyHolder(new ConfigFilePropertyHolder(
				configFile));
	}

	public ConfigFilePropertyHolder(JsonObject configFile) {
		this.configFile = configFile;
	}

	@Override
	public String getProperty(String property) {
		String systemProperty = System.getProperty(property);

		if (systemProperty != null) {
			return systemProperty;
		}

		String[] tokens = property.split("\\.");

		int lastTokenNumber = tokens.length - 1;

		JsonObject currentObject = configFile;

		for (int i = 0; i < tokens.length; i++) {
			JsonElement element = currentObject.get(tokens[i]);
			if (element == null) {
				return null;
			}

			if (i == lastTokenNumber) {
				if (element instanceof JsonPrimitive) {
					return element.getAsString();
				} else {
					return element.toString();
				}
			}

			try {
				currentObject = (JsonObject) element;
			} catch (ClassCastException e) {
				return null;
			}
		}

		return null;
	}
}
