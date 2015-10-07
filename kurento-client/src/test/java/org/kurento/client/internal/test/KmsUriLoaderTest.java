package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.kurento.client.internal.KmsUrlLoader;
import org.kurento.client.internal.KmsUrlProvider;
import org.kurento.client.internal.NotEnoughResourcesException;
import org.kurento.commons.ClassPath;

public class KmsUriLoaderTest {

	public static class TestKmsUriProvider implements KmsUrlProvider {

		@Override
		public String getKmsUrl(int loadPoints)
				throws NotEnoughResourcesException {
			return "ws://vnfmUri?load=" + loadPoints;
		}

		@Override
		public String getKmsUrl() throws NotEnoughResourcesException {
			return "ws://vnfmUri";
		}
	}

	@Test
	public void testKmsUriProperty() throws IOException {

		String expectedKmsUri = "ws://test.url";

		System.setProperty(KmsUrlLoader.KMS_URL_PROPERTY, expectedKmsUri);

		String kmsUri = new KmsUrlLoader(null).getKmsUrl();

		assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);

		System.setProperty(KmsUrlLoader.KMS_URL_PROPERTY, "");
	}

	@Test
	public void testKmsUri() throws IOException {

		String expectedKmsUri = "ws://test.url";

		String kmsUri = new KmsUrlLoader(
				ClassPath.get("/config-test.properties")).getKmsUrl();

		assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
	}

	@Test
	public void testDefaultKmsUri() throws IOException {

		String expectedKmsUri = KmsUrlLoader.DEFAULT_KMS_URL;

		String kmsUri = new KmsUrlLoader(
				ClassPath.get("/non-existing.properties")).getKmsUrl();

		assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
	}

	@Test
	public void testInvalidFile() throws IOException {
		String expectedKmsUri = KmsUrlLoader.DEFAULT_KMS_URL;

		String kmsUri = new KmsUrlLoader(ClassPath.get("/invalid.properties"))
				.getKmsUrl();

		assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
	}

	@Test
	public void testKmsUriProviderWithLoad() throws IOException {

		String expectedKmsUri = "ws://vnfmUri?load=50";

		KmsUrlLoader kmsUriLoader = new KmsUrlLoader(
				ClassPath.get("/provider-config.properties"));

		String kmsUri = kmsUriLoader.getKmsUrlLoad(50);

		assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
	}

	@Test
	public void testKmsUriProvider() throws IOException {

		String expectedKmsUri = "ws://vnfmUri";

		KmsUrlLoader kmsUriLoader = new KmsUrlLoader(
				ClassPath.get("/provider-config.properties"));

		String kmsUri = kmsUriLoader.getKmsUrl();

		assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
	}

}
