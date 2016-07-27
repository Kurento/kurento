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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.base.KurentoClientTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KurentoClientKmsConnectionTest extends KurentoClientTest {

  private static Logger log = LoggerFactory.getLogger(KurentoClientKmsConnectionTest.class);

  @Test
  public void errorSendingClosedKmsTest() throws Exception {

    String kmsUrl = kms.getWsUri();

    KurentoClient kurento = KurentoClient.create(kmsUrl, new KurentoConnectionListener() {

      @Override
      public void reconnected(boolean sameServer) {
      }

      @Override
      public void disconnected() {
        log.debug("Disconnected");
      }

      @Override
      public void connectionFailed() {
      }

      @Override
      public void connected() {
      }
    });

    kurento.createMediaPipeline();

    kms.stopKms();

    try {
      kurento.createMediaPipeline();
      fail("KurentoException should be thrown");
    } catch (KurentoException e) {
      assertThat(e.getMessage(), containsString("Exception connecting to WebSocket"));
    }
  }
}
