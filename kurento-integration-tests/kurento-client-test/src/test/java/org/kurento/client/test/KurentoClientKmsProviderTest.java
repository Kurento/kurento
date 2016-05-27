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
package org.kurento.client.test;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.client.KurentoClient;
import org.kurento.client.internal.KmsProvider;
import org.kurento.client.internal.NotEnoughResourcesException;
import org.kurento.test.base.KurentoClientTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.StandardSystemProperty;

public class KurentoClientKmsProviderTest extends KurentoClientTest {

  private static final Logger log = LoggerFactory.getLogger(KurentoClientKmsProviderTest.class);

  public static BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);

  public static class TestKmsUrlProvider implements KmsProvider {

    @Override
    public String reserveKms(String id, int loadPoints) throws NotEnoughResourcesException {
      if (loadPoints == 50) {
        queue.add("reserveKms(id,50)");
      } else {
        log.error("reserveKms called with {} instead of 50", loadPoints);
        queue.add("reserveKms(id," + loadPoints + ")");
      }

      return kms.getWsUri();
    }

    @Override
    public String reserveKms(String id) throws NotEnoughResourcesException {

      log.error("reserveKms called without load points");
      queue.add("reserveKms(id)");

      return kms.getWsUri();
    }

    @Override
    public void releaseKms(String id) throws NotEnoughResourcesException {
      queue.add("releaseKms(id)");
    }
  }

  @Test
  public void test() throws IOException {

    String oldKmsUrl = System.getProperty("kms.url");

    System.clearProperty("kms.url");

    Path backup = null;

    Path configFile = Paths.get(StandardSystemProperty.USER_HOME.value(), ".kurento",
        "config.properties");

    try {

      if (Files.exists(configFile)) {

        backup = configFile.getParent().resolve("config.properties.old");

        if (Files.exists(backup)) {
          Files.delete(backup);
        }

        Files.move(configFile, backup);
      }

      Files.createDirectories(configFile.getParent());

      try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
        writer.write("kms.url.provider: " + TestKmsUrlProvider.class.getName() + "\r\n");
      }

      KurentoClient kurento = KurentoClient.create();

      expectMethodCall("reserveKms(id)");

      kurento.destroy();

      expectMethodCall("releaseKms(id)");

    } finally {

      Files.delete(configFile);

      if (backup != null) {
        Files.move(backup, configFile);
      }

      if (oldKmsUrl != null) {
        System.setProperty("kms.url", oldKmsUrl);
      }
    }
  }

  private void expectMethodCall(String expectedMethod) {
    try {
      String result = queue.poll(10, TimeUnit.SECONDS);

      if (result == null) {
        fail("Event in KmsProvider not called");
      } else {

        if (!result.equals(expectedMethod)) {
          fail("Test failed");
        }
      }
    } catch (InterruptedException e) {
      fail("KmsProvider was not called");
    }
  }

}
