package org.kurento.commons;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFileFinder {

	private static final Logger log = LoggerFactory
			.getLogger(ConfigFilePropertyHolder.class);

	private static final String DEFAULT_KURENTO_CONF_FILE_NAME = "kurento.conf.json";

	public static Path searchDefaultConfigFile() throws IOException {
		return searchConfigFileInDefaultPlaces(DEFAULT_KURENTO_CONF_FILE_NAME);
	}

	public static Path searchConfigFileInDefaultPlaces(String configFileName)
			throws IOException {

		if (configFileName == null) {
			configFileName = DEFAULT_KURENTO_CONF_FILE_NAME;
		}

		Path path = getPathFromFile(configFileName);

		if (path == null) {
			log.warn("Config file '" + configFileName
					+ "' not found. Searching places are:"
					+ "1) A /config subdir of the current directory."
					+ "2) The current directory."
					+ "3) A classpath /config package."
					+ "4) The classpath root");
			return null;
		}

		return path;
	}

	/**
	 * The config file will be searched in the following places:
	 * <ul>
	 * <li>1. A /config subdir of the current directory.</li>
	 * <li>2. The current directory</li>
	 * <li>3. A classpath /config package</li>
	 * <li>4. The classpath root</li>
	 * </ul>
	 * 
	 * The list is ordered by precedence (locations higher in the list override
	 * lower items).
	 * 
	 * Remember that a property can be set using Java System properties (java
	 * -Dprop=value -jar file.jar)
	 * 
	 * @param fileName
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private static Path getPathFromFile(String fileName) throws IOException {

		Path path = Paths.get("config", fileName);

		if (Files.exists(path)) {
			return path;
		}

		path = Paths.get(fileName);

		if (Files.exists(path)) {
			return path;
		}

		path = getPathInClasspath("/config/" + fileName);

		if (path != null) {
			return path;
		}

		path = getPathInClasspath("/" + fileName);

		if (path != null) {
			return path;
		}

		return null;
	}

	private static Path getPathInClasspath(String resource) throws IOException {

		URL url = ConfigFileFinder.class.getResource(resource);

		if (url == null) {
			return null;
		}

		URI uri;
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(
					"Exception converting classpath URL to URI", e);
		}

		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
			return Paths.get(uri);
		}

		String s = uri.toString();
		int separator = s.indexOf("!/");
		String entryName = s.substring(separator + 2);
		URI fileURI = URI.create(s.substring(0, separator));

		FileSystem fs = FileSystems.newFileSystem(fileURI,
				Collections.<String, Object> emptyMap());
		return fs.getPath(entryName);
	}

}
