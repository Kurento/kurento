package org.kurento.commons;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.kurento.commons.PropertiesManager.PropertyHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static Logger log = LoggerFactory
			.getLogger(ConfigFilePropertyHolder.class);

	private static final Gson gson = new GsonBuilder().create();

	private JsonObject configFile;

	public static void configurePropertiesFromConfigFile(Path configFilePath)
			throws JsonSyntaxException, JsonIOException, IOException {

		Preconditions.checkNotNull(configFilePath,
				"configFilePath paramter must be not null.");

		log.debug("Using configuration file in path '" + configFilePath + "' ("
				+ configFilePath.getClass().getCanonicalName() + ")");

		JsonReader reader = new JsonReader(Files.newBufferedReader(
				configFilePath, StandardCharsets.UTF_8));
		reader.setLenient(true);

		JsonObject configFile = gson.fromJson(reader, JsonObject.class);

		traceConfigContent(configFile);

		PropertiesManager.setPropertyHolder(new ConfigFilePropertyHolder(
				configFile));
	}

	private static void traceConfigContent(JsonObject configFile) {
		if (log.isDebugEnabled()) {
			Gson gs = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			String jsonContents = gs.toJson(configFile);
			log.debug("Configuration content: " + jsonContents);
		}
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
