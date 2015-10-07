package org.kurento.client.internal;

import java.nio.file.Path;

import org.kurento.commons.UrlLoader;

public class KmsUrlLoader extends UrlLoader<KmsUrlProvider> {

	public static final String KMS_URL_PROPERTY = "kms.url";
	public static final String KMS_URL_PROVIDER_PROPERTY = "kms.url.provider";
	public static final String DEFAULT_KMS_URL = "ws://127.0.0.1:8888/kurento";

	public KmsUrlLoader(Path configFile) {
		super(configFile, KMS_URL_PROPERTY, KMS_URL_PROVIDER_PROPERTY,
				DEFAULT_KMS_URL);
	}

	public String getKmsUrl() {
		if (getUrl() == null) {
			return loadKmsUrlFromProvider(-1);
		} else {
			return getUrl();
		}
	}

	public String getKmsUrlLoad(int loadPoints) {
		if (getUrl() == null) {
			return loadKmsUrlFromProvider(loadPoints);
		} else {
			return getUrl();
		}
	}

	private synchronized String loadKmsUrlFromProvider(int loadPoints) {

		if (loadPoints == -1) {
			return getUrlProvider().getKmsUrl();
		} else {
			return getUrlProvider().getKmsUrl(loadPoints);
		}
	}

}
