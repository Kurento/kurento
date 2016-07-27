/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.client.HttpPostEndpoint;
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.test.base.KurentoClientTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListenerTest extends KurentoClientTest {

  private static Logger log = LoggerFactory.getLogger(ConnectionListenerTest.class);

  @Test
  public void disconnectionEventTest() throws InterruptedException, IOException {

    final CountDownLatch disconnectedLatch = new CountDownLatch(1);

    String kmsUrl = kms.getWsUri();

    log.debug("Connecting to KMS in " + kmsUrl);

    KurentoClient kurentoClient = KurentoClient.create(kmsUrl, new KurentoConnectionListener() {

      @Override
      public void disconnected() {
        log.debug("disconnected from KMS");
        disconnectedLatch.countDown();
      }

      @Override
      public void connectionFailed() {

      }

      @Override
      public void connected() {

      }

      @Override
      public void reconnected(boolean sameServer) {

      }
    });

    MediaPipeline pipeline = kurentoClient.createMediaPipeline();

    PlayerEndpoint player =
        new PlayerEndpoint.Builder(pipeline, "http://" + getTestFilesHttpPath()
            + "/video/format/small.webm").build();

    HttpPostEndpoint httpEndpoint = new HttpPostEndpoint.Builder(pipeline).build();

    player.connect(httpEndpoint);

    try {
      kms.stopKms();
    } catch (Exception e) {
      fail("Exception thrown when destroying kms. " + e);
    }

    log.debug("Waiting for disconnection event");
    if (!disconnectedLatch.await(60, TimeUnit.SECONDS)) {
      fail("Event disconnected should be thrown when kcs is destroyed");
    }
    log.debug("Disconnection event received");
  }

  @Test
  public void reconnectTest() throws InterruptedException, IOException {

    String kmsUrl = kms.getWsUri();

    log.debug("Connecting to KMS in " + kmsUrl);

    KurentoClient kurentoClient = KurentoClient.create(kmsUrl);

    kurentoClient.createMediaPipeline();

    kms.stopKms();

    Thread.sleep(3000);

    kms.start();

    kurentoClient.createMediaPipeline();

    kms.stopKms();
  }
}
