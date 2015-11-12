package org.kurento.client.internal;

import java.nio.file.Path;

import org.kurento.commons.UrlServiceLoader;

public class KmsUrlLoader extends UrlServiceLoader<KmsProvider> {

	public static final String KMS_URL_PROPERTY = "kms.url";
	public static final String KMS_URL_PROVIDER_PROPERTY = "kms.url.provider";
	public static final String DEFAULT_KMS_URL = "ws://127.0.0.1:8888/kurento";

	public KmsUrlLoader(Path configFile) {
		super(configFile, KMS_URL_PROPERTY, KMS_URL_PROVIDER_PROPERTY,
				DEFAULT_KMS_URL);
	}

	public String getKmsUrl(String id) {
		if (getUrl() == null) {
			return loadKmsUrlFromProvider(id, -1);
		} else {
			return getUrl();
		}
	}

	public String getKmsUrlLoad(String id, int loadPoints) {
		if (getUrl() == null) {
			return loadKmsUrlFromProvider(id, loadPoints);
		} else {
			return getUrl();
		}
	}

	private synchronized String loadKmsUrlFromProvider(String id,
			int loadPoints) {

		KmsProvider kmsProvider = getServiceProvider();
		if (loadPoints == -1) {
			return kmsProvider.reserveKms(id);
		} else {
			return kmsProvider.reserveKms(id, loadPoints);
		}
	}

	public void clientDestroyed(String id) {
		if (getUrl() == null) {
			getServiceProvider().releaseKms(id);
		}
	}

}
