package org.kurento.client.internal;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmsUrlLoader {

	public static final String KMS_URI_PROPERTY = "kms.url";
	public static final String KMS_URI_PROVIDER_PROPERTY = "kms.url.provider";

	private static final Logger log = LoggerFactory
			.getLogger(KmsUrlLoader.class);

	public static final String DEFAULT_KMS_URI = "ws://127.0.0.1:8888/kurento";

	private String kmsUrlProviderClassName;

	private String kmsUri;

	private KmsUrlProvider kmsUrlProvider;

	public KmsUrlLoader(Path configFile) {
		kmsUri = load(configFile);
	}

	private String load(Path configFile) {

		String kmsUri = System.getProperty(KMS_URI_PROPERTY);
		if (kmsUri != null && !kmsUri.equals("")) {
			return kmsUri;
		}

		try {

			if (configFile != null && Files.exists(configFile)) {

				Properties properties = new Properties();
				try (BufferedReader reader = Files.newBufferedReader(configFile,
						StandardCharsets.UTF_8)) {
					properties.load(reader);
				}

				kmsUrlProviderClassName = properties
						.getProperty(KMS_URI_PROVIDER_PROPERTY);

				kmsUri = properties.getProperty(KMS_URI_PROPERTY);

				if (kmsUri == null && kmsUrlProviderClassName == null) {
					log.warn(
							"The file {} lacks property '{}' or '{}'. The default kms uri '{}' will be used",
							configFile.toAbsolutePath().toString(),
							KMS_URI_PROVIDER_PROPERTY, KMS_URI_PROPERTY,
							DEFAULT_KMS_URI);

					return DEFAULT_KMS_URI;
				} else {
					return kmsUri;
				}

			} else {
				return DEFAULT_KMS_URI;
			}

		} catch (Exception e) {
			log.warn("Exception loading {} file. Returning default kmsUri='{}'",
					configFile, DEFAULT_KMS_URI, e);

			return DEFAULT_KMS_URI;
		}
	}

	public String getKmsUrl() {
		if (kmsUri == null) {
			return loadKmsUrlFromProvider(-1);
		} else {
			return kmsUri;
		}
	}

	public String getKmsUrlLoad(int loadPoints) {
		if (kmsUri == null) {
			return loadKmsUrlFromProvider(loadPoints);
		} else {
			return kmsUri;
		}
	}

	private synchronized String loadKmsUrlFromProvider(int loadPoints) {

		if (kmsUrlProvider == null) {

			try {

				Class<?> vnfmClass = Class.forName(kmsUrlProviderClassName);
				kmsUrlProvider = (KmsUrlProvider) vnfmClass.newInstance();

			} catch (Exception e) {
				throw new RuntimeException("Exception loading vnfm class "
						+ kmsUrlProviderClassName, e);
			}
		}

		if (loadPoints == -1) {
			return kmsUrlProvider.getKmsUrl();
		} else {
			return kmsUrlProvider.getKmsUrl(loadPoints);
		}
	}

}
