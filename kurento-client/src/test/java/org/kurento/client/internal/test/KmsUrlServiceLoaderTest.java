/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.kurento.client.internal.KmsProvider;
import org.kurento.client.internal.KmsUrlLoader;
import org.kurento.client.internal.NotEnoughResourcesException;
import org.kurento.commons.ClassPath;

public class KmsUrlServiceLoaderTest {

  public static class TestKmsUrlProvider implements KmsProvider {

    @Override
    public String reserveKms(String id, int loadPoints) throws NotEnoughResourcesException {
      return "ws://vnfmUri?load=" + loadPoints;
    }

    @Override
    public String reserveKms(String id) throws NotEnoughResourcesException {
      return "ws://vnfmUri";
    }

    @Override
    public void releaseKms(String id) throws NotEnoughResourcesException {

    }
  }

  @Test
  public void testKmsUriProperty() throws IOException {

    String expectedKmsUri = "ws://test.url";

    System.setProperty(KmsUrlLoader.KMS_URL_PROPERTY, expectedKmsUri);

    String kmsUri = new KmsUrlLoader(null).getKmsUrl("id");

    assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);

    System.setProperty(KmsUrlLoader.KMS_URL_PROPERTY, "");
  }

  @Test
  public void testKurentoClient() throws IOException {

    String expectedKmsUri = "ws://test.url";

    System.setProperty(KmsUrlLoader.KMS_URL_PROPERTY, expectedKmsUri);

    String kmsUri = new KmsUrlLoader(null).getKmsUrl("id");

    assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);

    System.setProperty(KmsUrlLoader.KMS_URL_PROPERTY, "");
  }

  @Test
  public void testKmsUri() throws IOException {

    String expectedKmsUri = "ws://test.url";

    String kmsUri = new KmsUrlLoader(ClassPath.get("/config-test.properties")).getKmsUrl("id");

    assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
  }

  @Test
  public void testDefaultKmsUri() throws IOException {

    String expectedKmsUri = KmsUrlLoader.DEFAULT_KMS_URL;

    String kmsUri = new KmsUrlLoader(ClassPath.get("/non-existing.properties")).getKmsUrl("id");

    assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
  }

  @Test
  public void testInvalidFile() throws IOException {
    String expectedKmsUri = KmsUrlLoader.DEFAULT_KMS_URL;

    String kmsUri = new KmsUrlLoader(ClassPath.get("/invalid.properties")).getKmsUrl("id");

    assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
  }

  @Test
  public void testKmsUriProviderWithLoad() throws IOException {

    String expectedKmsUri = "ws://vnfmUri?load=50";

    KmsUrlLoader kmsUriLoader = new KmsUrlLoader(ClassPath.get("/provider-config.properties"));

    String kmsUri = kmsUriLoader.getKmsUrlLoad("id", 50);

    assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
  }

  @Test
  public void testKmsUriProvider() throws IOException {

    String expectedKmsUri = "ws://vnfmUri";

    KmsUrlLoader kmsUriLoader = new KmsUrlLoader(ClassPath.get("/provider-config.properties"));

    String kmsUri = kmsUriLoader.getKmsUrl("id");

    assertEquals("Invalid kmsUri read from file", expectedKmsUri, kmsUri);
  }

}
