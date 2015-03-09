package org.kurento.commons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFileManager {

	private static final Logger log = LoggerFactory
			.getLogger(ConfigFileManager.class);

	private static final String CONFIG_FILE_PATH_PROPERTY = "configFilePath";

	public static void loadConfigFile() {
		loadConfigFile("kurento.conf.json");
	}

	public static void loadConfigFile(String configFileName) {

		try {

			String configFilePath = System
					.getProperty(CONFIG_FILE_PATH_PROPERTY);

			Path configFile = null;

			if (configFilePath != null) {
				configFile = Paths.get(configFilePath);

				if (!Files.exists(configFile)) {
					log.warn(
							"Property '{}' points to an invalid location '{}'. Searching default config file '{}' in classpath and workdir",
							CONFIG_FILE_PATH_PROPERTY, configFilePath,
							configFileName);
					configFile = ConfigFileFinder
							.searchConfigFileInDefaultPlaces(configFileName);
				} else {
					log.info("Property {} points to a valid location. Will use the config from {}", CONFIG_FILE_PATH_PROPERTY, configFilePath);
				}
				
			} else {
				configFile = ConfigFileFinder
						.searchConfigFileInDefaultPlaces(configFileName);
			}

			if (configFile != null && Files.exists(configFile)) {
				ConfigFilePropertyHolder
				.configurePropertiesFromConfigFile(configFile);
			} else {
				log.warn("Config file {} not found. Using all default values", configFileName);
			}

		} catch (IOException e) {
			log.warn("Exception loading config file", e);
		}
	}

}
