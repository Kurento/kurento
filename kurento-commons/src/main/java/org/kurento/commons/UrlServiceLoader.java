package org.kurento.commons;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UrlServiceLoader<P> {

	private static final Logger log = LoggerFactory.getLogger(UrlServiceLoader.class);

	private String urlProperty;
	private String urlProviderProperty;
	private String defaultUrl;

	private String serviceProviderClassName;
	private String url;

	private P serviceProvider;

	public UrlServiceLoader(Path configFile, String urlProperty,
			String urlProviderProperty, String defaultUrl) {

		this.urlProperty = urlProperty;
		this.urlProviderProperty = urlProviderProperty;
		this.defaultUrl = defaultUrl;

		url = load(configFile);
	}

	private String load(Path configFile) {

		String kmsUrl = System.getProperty(urlProperty);
		if (kmsUrl != null && !kmsUrl.equals("")) {
			return kmsUrl;
		}

		try {

			if (configFile != null && Files.exists(configFile)) {

				Properties properties = new Properties();
				try (BufferedReader reader = Files.newBufferedReader(configFile,
						StandardCharsets.UTF_8)) {
					properties.load(reader);
				}

				serviceProviderClassName = properties
						.getProperty(urlProviderProperty);

				kmsUrl = properties.getProperty(urlProperty);

				if (kmsUrl == null && serviceProviderClassName == null) {
					log.warn(
							"The file {} lacks property '{}' or '{}'. The default kms uri '{}' will be used",
							configFile.toAbsolutePath().toString(),
							urlProviderProperty, urlProperty, defaultUrl);

					return defaultUrl;
				} else {
					return kmsUrl;
				}

			} else {
				return defaultUrl;
			}

		} catch (Exception e) {
			log.warn("Exception loading {} file. Returning default kmsUri='{}'",
					configFile, defaultUrl, e);

			return defaultUrl;
		}
	}

	protected String getUrl() {
		return url;
	}

	@SuppressWarnings("unchecked")
	private P createUrlProvider() {
		try {

			Class<?> providerClass = Class.forName(serviceProviderClassName);

			return (P) providerClass.newInstance();

		} catch (Exception e) {
			throw new RuntimeException("Exception loading url provider class "
					+ serviceProviderClassName, e);
		}
	}

	protected P getServiceProvider() {
		if (serviceProvider == null) {
			serviceProvider = createUrlProvider();
		}
		return serviceProvider;
	}

}
